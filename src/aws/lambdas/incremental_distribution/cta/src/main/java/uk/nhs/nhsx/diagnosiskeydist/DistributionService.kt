package uk.nhs.nhsx.diagnosiskeydist

import uk.nhs.nhsx.core.Clock
import uk.nhs.nhsx.core.aws.cloudfront.AwsCloudFront
import uk.nhs.nhsx.core.aws.s3.AwsS3
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.Locator
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.signature.Signer
import uk.nhs.nhsx.diagnosiskeydist.ConcurrentExecution.Companion.SYSTEM_EXIT_ERROR_HANDLER
import uk.nhs.nhsx.diagnosiskeydist.agspec.ENIntervalNumber
import uk.nhs.nhsx.diagnosiskeydist.apispec.DailyZIPSubmissionPeriod
import uk.nhs.nhsx.diagnosiskeydist.apispec.DailyZIPSubmissionPeriod.Companion.DAILY_PATH_PREFIX
import uk.nhs.nhsx.diagnosiskeydist.apispec.TwoHourlyZIPSubmissionPeriod
import uk.nhs.nhsx.diagnosiskeydist.apispec.ZIPSubmissionPeriod
import uk.nhs.nhsx.diagnosiskeydist.keydistribution.KeyDistributor
import uk.nhs.nhsx.diagnosiskeydist.keydistribution.KeyFileUtility.writeToFile
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKey
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit.DAYS
import java.util.*
import java.util.Collections.synchronizedList

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
    private val clock: Clock
) {
    private val uploadedZipFileNames = synchronizedList(ArrayList<String>())

    fun distributeKeys(now: Instant) {
        val window = DistributionServiceWindow(now, config.zipSubmissionPeriodOffset)

        events(
            DistributionBatchWindow(
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
                    pool.execute { distributeSubmissions(allSubmissions, window, zipPeriod) }
                }
            }
        }

        deleteOrReplaceZipFiles(config.zipBucketName, window)
        invalidateCloudFrontCaches()
    }

    private fun deleteOrReplaceZipFiles(bucketName: BucketName, window: DistributionServiceWindow) {
        fun generateEmptyZipFor(
            objectKey: String,
            threshold: Instant,
            window: DistributionServiceWindow
        ) {
            val dailyInstant = DailyZIPSubmissionPeriod.parseOrNull(objectKey)
            if (dailyInstant != null && dailyInstant.isAfter(threshold)) {
                distributeExposureKeys(listOf(), window, DailyZIPSubmissionPeriod(dailyInstant))
                events(EmptyZipDistributed(objectKey))
            }
        }

        val thirtyDaysAgo = clock()
            .truncatedTo(DAYS)
            .minus(Duration.ofDays(30))

        awsS3.getObjectSummaries(bucketName)
            .map { it.key }
            .filterNot { uploadedZipFileNames.contains(it) }
            .forEach {
                when {
                    it.startsWith(DAILY_PATH_PREFIX) -> generateEmptyZipFor(it, thirtyDaysAgo, window)
                    else -> awsS3.deleteObject(Locator.of(bucketName, ObjectKey.of(it)))
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

    private fun distributeSubmissions(
        submissions: List<Submission>,
        window: DistributionServiceWindow,
        zipPeriod: ZIPSubmissionPeriod
    ) {
        distributeExposureKeys(validKeysFromSubmissions(submissions, window, zipPeriod), window, zipPeriod)
    }

    private fun distributeExposureKeys(
        temporaryExposureKeys: List<StoredTemporaryExposureKey>,
        window: DistributionServiceWindow,
        zipPeriod: ZIPSubmissionPeriod
    ) {
        val binFile = File.createTempFile("export", ".bin")
        val sigFile = File.createTempFile("export", ".sig")

        try {
            val binFileContent = generateExportFileContentFrom(temporaryExposureKeys, window, zipPeriod)
            writeToFile(binFile, binFileContent)

            val sigFileContent = generateSigFileContentFrom(binFileContent)
            writeToFile(sigFile, sigFileContent)

            val objectName = zipPeriod.zipPath()

            keyDistributor.distribute(config.zipBucketName, ObjectKey.of(objectName), binFile, sigFile)

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
                    if (ENIntervalNumber(key.rollingStartNumber.toLong()).validUntil(window.zipExpirationExclusive())) {
                        temporaryExposureKeys.add(key)
                    }
                }
            }
        }

        // Important: the keys must not be distributed in submission order for privacy reasons
        temporaryExposureKeys.shuffle()
        return temporaryExposureKeys
    }

    private fun generateExportFileContentFrom(
        temporaryExposureKeys: List<StoredTemporaryExposureKey>,
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
