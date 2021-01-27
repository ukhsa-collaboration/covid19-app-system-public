package uk.nhs.nhsx.diagnosiskeydist

import batchZipCreation.Exposure
import batchZipCreation.Exposure.TemporaryExposureKey
import com.amazonaws.services.kms.model.SigningAlgorithmSpec
import com.amazonaws.services.s3.model.S3ObjectSummary
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import uk.nhs.nhsx.core.aws.cloudfront.AwsCloudFront
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.Locator
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.core.aws.ssm.ParameterName
import uk.nhs.nhsx.core.signature.KeyId
import uk.nhs.nhsx.core.signature.Signature
import uk.nhs.nhsx.core.signature.Signer
import uk.nhs.nhsx.diagnosiskeydist.agspec.ENIntervalNumber
import uk.nhs.nhsx.diagnosiskeydist.keydistribution.SaveToFileKeyDistributor
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKey
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKeyPayload
import uk.nhs.nhsx.testhelper.BatchExport.tekExportFromZipFile
import uk.nhs.nhsx.testhelper.BatchExport.tekListFromZipFile
import uk.nhs.nhsx.testhelper.BatchExport.tekSignatureListFromZipFile
import uk.nhs.nhsx.testhelper.mocks.FakeS3
import java.io.File
import java.nio.file.Path
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import java.util.function.Consumer
import java.util.stream.Collectors
import java.util.stream.IntStream

class DistributionServiceTest {

    private val awsS3 = FakeS3()
    private val awsCloudFront = mock(AwsCloudFront::class.java)
    private val mobileAppBundleId = "uk.nhs.covid19.internal"
    private val exposureProtobuf = ExposureProtobuf(mobileAppBundleId)

    private val batchProcessingConfig = BatchProcessingConfig(
        true,
        BucketName.of("dist-zip-bucket-name"),
        "dis-id",
        "dist-pattern-daily",
        "dist-pattern-2hourly",
        ParameterName.of("ssmKeyIdParameterName"),
        ParameterName.of("ssmContentKeyIdParameterName")
    )
    private val signer =
        Signer { Signature(KeyId.of("key-id"), SigningAlgorithmSpec.ECDSA_SHA_256, byteArrayOf(1, 2, 3)) }


    @Test
    fun shouldAbortOutsideServiceWindow(@TempDir distributionFolder: Path) {
        assertThatThrownBy {
            val date = toInstant(2020, 7, 14, 19, 30)
            DistributionService(
                MockSubmissionRepository(emptyList()),
                exposureProtobuf,
                SaveToFileKeyDistributor(distributionFolder.toFile()),
                signer,
                awsCloudFront,
                awsS3,
                batchProcessingConfig
            ).distributeKeys(date)
        }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("CloudWatch Event triggered Lambda at wrong time.")
    }

    @Test
    fun shouldNotAbortIfFlagIsFalse(@TempDir distributionFolder: Path) {
        val date = toInstant(2020, 7, 14, 19, 30)
        DistributionService(
            MockSubmissionRepository(emptyList()),
            exposureProtobuf,
            SaveToFileKeyDistributor(distributionFolder.toFile()),
            signer,
            awsCloudFront,
            awsS3,
            BatchProcessingConfig(
                false,
                BucketName.of("dist-zip-bucket-name"),
                "",
                "",
                "",
                ParameterName.of(""),
                ParameterName.of("")
            )
        ).distributeKeys(date)
        assertDailyExportBatchExists(distributionFolder)
    }

    @Test
    fun distributeKeysFromSubmissionsOnSingleDay(@TempDir distributionFolder: Path) {
        val date = toInstant(2020, 7, 16, 7, 46)
        val dateBefore = toInstant(2020, 7, 15, 7, 46)
        DistributionService(
            MockSubmissionRepository(listOf(date, dateBefore)),
            exposureProtobuf,
            SaveToFileKeyDistributor(distributionFolder.toFile()),
            signer,
            awsCloudFront,
            awsS3,
            batchProcessingConfig
        ).distributeKeys(date)
        assertDailyExportBatchExists(distributionFolder)
        assertTwoHourlyExportBatchExists(distributionFolder)
        val latestDailyZipFile = File(distributionFolder.toFile(), "distribution/daily/2020071600.zip")
        var keys: List<TemporaryExposureKey?> = tekListFromZipFile(latestDailyZipFile)
        assertEquals(14, keys.size, "keys in latest daily zip file")
        val earlierDailyZipFile = File(distributionFolder.toFile(), "distribution/daily/2020071500.zip")
        keys = tekListFromZipFile(earlierDailyZipFile)
        assertEquals(0, keys.size, "keys in earlier daily zip file")
    }

    @Test
    fun distributeKeysFromSubmissionsOnMultipleDays(@TempDir distributionFolder: Path) {
        val date = toInstant(2020, 7, 16, 7, 46)
        val dateBefore = toInstant(2020, 7, 15, 7, 46)
        val dateBeforeThat = toInstant(2020, 7, 14, 7, 46)
        val date3DaysBefore = toInstant(2020, 7, 13, 7, 46)
        DistributionService(
            MockSubmissionRepository(listOf(date, dateBefore, dateBeforeThat, date3DaysBefore)),
            exposureProtobuf,
            SaveToFileKeyDistributor(distributionFolder.toFile()),
            signer,
            awsCloudFront,
            awsS3,
            batchProcessingConfig
        ).distributeKeys(date)
        assertDailyExportBatchExists(distributionFolder)
        assertTwoHourlyExportBatchExists(distributionFolder)
        val latestDailyZipFile = File(distributionFolder.toFile(), "distribution/daily/2020071600.zip")
        var keys: List<TemporaryExposureKey?> = tekListFromZipFile(latestDailyZipFile)
        assertEquals(14, keys.size, "keys in latest daily zip file")
        var earlierDailyZipFile = File(distributionFolder.toFile(), "distribution/daily/2020071500.zip")
        keys = tekListFromZipFile(earlierDailyZipFile)
        assertEquals(13, keys.size, "keys in earlier daily zip file")
        earlierDailyZipFile = File(distributionFolder.toFile(), "distribution/daily/2020071400.zip")
        keys = tekListFromZipFile(earlierDailyZipFile)
        assertEquals(12, keys.size, "keys in even earlier daily zip file")
    }

    @Test
    fun tekExportHasCorrectSignatureInfo(@TempDir distributionFolder: Path) {
        val date = toInstant(2020, 7, 16, 7, 46)
        DistributionService(
            MockSubmissionRepository(listOf(date)),
            exposureProtobuf,
            SaveToFileKeyDistributor(distributionFolder.toFile()),
            signer,
            awsCloudFront,
            awsS3,
            batchProcessingConfig
        ).distributeKeys(date)
        val latestDailyZipFile = File(distributionFolder.toFile(), "distribution/daily/2020071600.zip")
        val temporaryExposureKeyExport = tekExportFromZipFile(latestDailyZipFile)
        assertThat(temporaryExposureKeyExport.signatureInfosList).hasSize(1)
        assertThat(temporaryExposureKeyExport.signatureInfosList[0]).isEqualTo(expectedSignatureInfo)
    }

    @Test
    fun tekSignatureListHasCorrectSignatureInfo(@TempDir distributionFolder: Path) {
        val date = toInstant(2020, 7, 16, 7, 46)
        DistributionService(
            MockSubmissionRepository(listOf(date)),
            exposureProtobuf,
            SaveToFileKeyDistributor(distributionFolder.toFile()),
            signer,
            awsCloudFront,
            awsS3,
            batchProcessingConfig
        ).distributeKeys(date)
        val latestDailyZipFile = File(distributionFolder.toFile(), "distribution/daily/2020071600.zip")
        val tekSignatureList = tekSignatureListFromZipFile(latestDailyZipFile)
        assertThat(tekSignatureList.signaturesList).hasSize(1)
        assertThat(tekSignatureList.signaturesList[0].signatureInfo).isEqualTo(expectedSignatureInfo)
    }

    @Test
    fun deletesOldObjectsThatDontMatchUploaded(@TempDir distributionFolder: Path) {
        val date = toInstant(2020, 7, 16, 7, 46)
        val notMatchedObjectKey = ObjectKey.of("obj-key-not-uploaded")
        awsS3.existing.add(
            object : S3ObjectSummary() {
                init {
                    setBucketName(batchProcessingConfig.zipBucketName.value)
                    setKey(notMatchedObjectKey.value)
                }
            }
        )
        DistributionService(
            MockSubmissionRepository(listOf(date)),
            exposureProtobuf,
            SaveToFileKeyDistributor(distributionFolder.toFile()),
            signer,
            awsCloudFront,
            awsS3,
            batchProcessingConfig
        ).distributeKeys(date)

        assertThat(awsS3.deleted).contains(
            Locator.of(batchProcessingConfig.zipBucketName, notMatchedObjectKey)
        )
    }

    @Test
    fun noDeletionIfObjectKeyMatchesUploaded(@TempDir distributionFolder: Path) {
        val date = toInstant(2020, 7, 16, 7, 46)
        val matchedObjectKey = ObjectKey.of("distribution/daily/2020070300.zip")
        awsS3.existing.add(
            object : S3ObjectSummary() {
                init {
                    setBucketName(batchProcessingConfig.zipBucketName.value)
                    setKey(matchedObjectKey.value)
                }
            }
        )
        DistributionService(
            MockSubmissionRepository(listOf(date)),
            exposureProtobuf,
            SaveToFileKeyDistributor(distributionFolder.toFile()),
            signer,
            awsCloudFront,
            awsS3,
            batchProcessingConfig
        ).distributeKeys(date)
        assertThat(awsS3.deleted).hasSize(0)
    }

    @Test
    fun invalidatesCloudFrontCaches(@TempDir distributionFolder: Path) {
        val date = toInstant(2020, 7, 16, 7, 46)
        DistributionService(
            MockSubmissionRepository(listOf(date)),
            exposureProtobuf,
            SaveToFileKeyDistributor(distributionFolder.toFile()),
            signer,
            awsCloudFront,
            awsS3,
            batchProcessingConfig
        ).distributeKeys(date)
        verify(awsCloudFront, times(1))
            .invalidateCache("dis-id", "dist-pattern-daily")
        verify(awsCloudFront, times(1))
            .invalidateCache("dis-id", "dist-pattern-2hourly")
    }

    @Test
    fun checkDailyBatchExistsAtMidnightBoundary(@TempDir distributionFolder: Path) {
        val date = toInstant(2020, 9, 16, 23, 47)
        DistributionService(
            MockSubmissionRepository(listOf(date)),
            exposureProtobuf,
            SaveToFileKeyDistributor(distributionFolder.toFile()),
            signer,
            awsCloudFront,
            awsS3,
            batchProcessingConfig
        ).distributeKeys(date)
        val distributionDailyDir = File(distributionFolder.toFile(), "distribution/daily")
        assertTrue(File(distributionDailyDir.path + "/2020091700.zip").exists())
        assertDailyExportBatchExists(distributionFolder)
    }

    private fun assertDailyExportBatchExists(distributionFolder: Path) {
        val distributionDailyDir = File(distributionFolder.toFile(), "distribution/daily")
        assertTrue(distributionDailyDir.exists())
        assertEquals(15, distributionDailyDir.list().size)
    }

    private fun assertTwoHourlyExportBatchExists(distributionFolder: Path) {
        val distributionTwoHourlyDir = File(distributionFolder.toFile(), "distribution/two-hourly")
        assertTrue(distributionTwoHourlyDir.exists())
        assertEquals(168, distributionTwoHourlyDir.list().size)
    }

    internal class MockSubmissionRepository(submissionDates: List<Instant>) : SubmissionRepository {
        private val submissions: MutableList<Submission> = ArrayList()
        override fun loadAllSubmissions(
            minimalSubmissionTimeEpocMillisExclusive: Long,
            maxLimit: Int,
            maxResults: Int
        ): List<Submission> {
            return submissions
        }

        private fun makeKeySet(submissionDate: Instant): Submission {
            val mostRecentKeyRollingStart =
                ENIntervalNumber.enIntervalNumberFromTimestamp(Date.from(submissionDate)).enIntervalNumber / 144 * 144
            val keys = IntStream.range(0, 14)
                .mapToObj { i: Int -> makeKey(mostRecentKeyRollingStart - i * 144) }
                .collect(Collectors.toList())
            return Submission(Date.from(submissionDate), StoredTemporaryExposureKeyPayload(keys))
        }

        companion object {
            private fun makeKey(keyStartTime: Long): StoredTemporaryExposureKey {
                return StoredTemporaryExposureKey("ABC", Math.toIntExact(keyStartTime), 144, 7)
            }
        }

        init {
            submissionDates.forEach(Consumer { it: Instant -> submissions.add(makeKeySet(it)) })
        }
    }

    private fun toInstant(year: Int, month: Int, dayOfMonth: Int, hour: Int, minute: Int): Instant =
        LocalDateTime.of(year, month, dayOfMonth, hour, minute, 0).toInstant(ZoneOffset.UTC)

    private val expectedSignatureInfo = Exposure.SignatureInfo.newBuilder()
        .setAndroidPackage(mobileAppBundleId)
        .setAppBundleId(mobileAppBundleId)
        .setVerificationKeyVersion("v1")
        .setVerificationKeyId("234")
        .setSignatureAlgorithm("1.2.840.10045.4.3.2")
        .build()
}
