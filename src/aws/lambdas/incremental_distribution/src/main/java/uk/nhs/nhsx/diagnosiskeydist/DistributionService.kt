package uk.nhs.nhsx.diagnosiskeydist

import uk.nhs.nhsx.core.aws.cloudfront.AwsCloudFront
import uk.nhs.nhsx.core.aws.s3.AwsS3
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.Locator
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.signature.Signer
import uk.nhs.nhsx.diagnosiskeydist.ConcurrentExecution.SYSTEM_EXIT_ERROR_HANDLER
import uk.nhs.nhsx.diagnosiskeydist.agspec.ENIntervalNumber
import uk.nhs.nhsx.diagnosiskeydist.apispec.DailyZIPSubmissionPeriod
import uk.nhs.nhsx.diagnosiskeydist.apispec.TwoHourlyZIPSubmissionPeriod
import uk.nhs.nhsx.diagnosiskeydist.apispec.ZIPSubmissionPeriod
import uk.nhs.nhsx.diagnosiskeydist.keydistribution.KeyDistributor
import uk.nhs.nhsx.diagnosiskeydist.keydistribution.KeyFileUtility
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKey
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.security.NoSuchAlgorithmException
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.Collections.synchronizedList
import java.util.function.Supplier

/**
 * Batch job to generate and upload daily and two-hourly Diagnosis Key Distribution ZIPs every two hours during a 15' window
 */
class DistributionService(
    private val submissionRepository: SubmissionRepository,
    private val exposureProtobuf: ExposureProtobuf,
    private val keyDistributor: KeyDistributor,
    private val signer: Signer,
    private val awsCloudFront: AwsCloudFront,
    private val awsS3: AwsS3,
    private val config: BatchProcessingConfig,
    private val events: Events,
    private val clock: Supplier<Instant>
) {
    private val uploadedZipFileNames = synchronizedList(ArrayList<String>())

    @Throws(Exception::class)
    fun distributeKeys(now: Instant) {
        val window = DistributionServiceWindow(now, config.zipSubmissionPeriodOffset)

        events(
            javaClass, DistributionBatchWindow(
                now,
                window.earliestBatchStartDateWithinHourInclusive(),
                window.latestBatchStartDateWithinHourExclusive()
            )
        )

        if (!window.isValidBatchStartDate()) {
            check(!config.shouldAbortOutsideTimeWindow) { "CloudWatch Event triggered Lambda at wrong time." }
        }

        val allSubmissions = submissionRepository.loadAllSubmissions()
        val daily = DailyZIPSubmissionPeriod.periodForSubmissionDate(now)
        val twoHourly = TwoHourlyZIPSubmissionPeriod.periodForSubmissionDate(now)
        for (lastZipPeriod in listOf(daily, twoHourly)) {
            ConcurrentExecution(
                "Distribution: ${lastZipPeriod.javaClass.simpleName}",
                Duration.ofMinutes(MAXIMAL_ZIP_SIGN_S3_PUT_TIME_MINUTES.toLong()),
                events,
                clock,
                SYSTEM_EXIT_ERROR_HANDLER
            ).use { pool ->
                for (zipPeriod in lastZipPeriod.allPeriodsToGenerate()) {
                    pool.execute { distributeKeys(allSubmissions, window, zipPeriod) }
                }
            }
        }

        removeUnmodifiedObjectsFromDistributionBucket(config.zipBucketName)
        invalidateCloudFrontCaches()
    }

    private fun removeUnmodifiedObjectsFromDistributionBucket(bucketName: BucketName) {
        val distributionObjectSummaries = awsS3.getObjectSummaries(bucketName)
        for (s3ObjectSummary in distributionObjectSummaries) {
            if (!uploadedZipFileNames.contains(s3ObjectSummary.key)) {
                awsS3.deleteObject(Locator.of(bucketName, ObjectKey.of(s3ObjectSummary.key)))
            }
        }
    }

    private fun invalidateCloudFrontCaches() {
        awsCloudFront.invalidateCache(
            config.cloudFrontDistributionId,
            config.distributionPatternDaily
        )
        awsCloudFront.invalidateCache(
            config.cloudFrontDistributionId,
            config.distributionPattern2Hourly
        )
    }

    @Throws(IOException::class, NoSuchAlgorithmException::class)
    private fun distributeKeys(
        submissions: List<Submission>,
        window: DistributionServiceWindow,
        zipPeriod: ZIPSubmissionPeriod
    ) {
        val temporaryExposureKeys = validKeysFromSubmissions(submissions, window, zipPeriod)
        val binFile = File.createTempFile("export", ".bin")
        val sigFile = File.createTempFile("export", ".sig")

        try {
            val binFileContent = generateExportFileContentFrom(temporaryExposureKeys, window, zipPeriod)
            KeyFileUtility.writeToFile(binFile, binFileContent)

            val sigFileContent = generateSigFileContentFrom(binFileContent)
            KeyFileUtility.writeToFile(sigFile, sigFileContent)

            val objectName = zipPeriod.zipPath()
            keyDistributor.distribute(
                config.zipBucketName,
                ObjectKey.of(objectName),
                binFile,
                sigFile
            )

            uploadedZipFileNames.add(objectName)
        } finally {
            listOf(binFile, sigFile).forEach(File::delete)
        }
    }

    private fun validKeysFromSubmissions(
        submissions: List<Submission>,
        window: DistributionServiceWindow,
        zipPeriod: ZIPSubmissionPeriod,
    ): List<StoredTemporaryExposureKey> {
        val temporaryExposureKeys = mutableListOf<StoredTemporaryExposureKey>()
        for (submission in submissions) {
            if (zipPeriod.isCoveringSubmissionDate(submission.submissionDate, window.zipSubmissionPeriodOffset)) {
                for (key in submission.payload.temporaryExposureKeys) {
                    val keyIntervalNumber = ENIntervalNumber(key.rollingStartNumber.toLong())
                    if (keyIntervalNumber.validUntil(window.zipExpirationExclusive())) {
                        temporaryExposureKeys.add(key)
                    }
                }
            }
        }

        // Important: the keys must not be distributed in submission order for privacy reasons
        temporaryExposureKeys.shuffle()
        return temporaryExposureKeys
    }

    @Throws(IOException::class)
    private fun generateExportFileContentFrom(
        temporaryExposureKeys: List<StoredTemporaryExposureKey?>,
        window: DistributionServiceWindow,
        period: ZIPSubmissionPeriod
    ): ByteArray {
        val bout = ByteArrayOutputStream()
        bout.write(EK_EXPORT_V1_HEADER.toByteArray())
        bout.write(
            exposureProtobuf.buildTemporaryExposureKeyExport(
                temporaryExposureKeys,
                period,
                window.zipSubmissionPeriodOffset
            ).toByteArray()
        )
        return bout.toByteArray()
    }

    private fun generateSigFileContentFrom(binFileContent: ByteArray): ByteArray =
        signer.sign(binFileContent)
            .let { exposureProtobuf.buildTEKSignatureList(it.asByteBuffer()) }
            .toByteArray()

    companion object {
        private const val MAXIMAL_ZIP_SIGN_S3_PUT_TIME_MINUTES = 6
        const val EK_EXPORT_V1_HEADER = "EK Export v1    "
    }
}
