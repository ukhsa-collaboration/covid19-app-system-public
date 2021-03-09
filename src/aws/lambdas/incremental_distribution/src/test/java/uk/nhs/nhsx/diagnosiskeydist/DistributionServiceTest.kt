package uk.nhs.nhsx.diagnosiskeydist

import batchZipCreation.Exposure
import batchZipCreation.Exposure.TemporaryExposureKey
import batchZipCreation.Exposure.TemporaryExposureKeyExport
import com.amazonaws.services.kms.model.SigningAlgorithmSpec
import com.amazonaws.services.s3.model.S3ObjectSummary
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import uk.nhs.nhsx.core.SystemClock
import uk.nhs.nhsx.core.aws.cloudfront.AwsCloudFront
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.Locator
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.core.aws.ssm.ParameterName
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.core.signature.KeyId
import uk.nhs.nhsx.core.signature.Signature
import uk.nhs.nhsx.core.signature.Signer
import uk.nhs.nhsx.diagnosiskeydist.agspec.ENIntervalNumber
import uk.nhs.nhsx.diagnosiskeydist.keydistribution.KeyDistributor
import uk.nhs.nhsx.diagnosiskeydist.keydistribution.SaveToFileKeyDistributor
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKey
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKeyPayload
import uk.nhs.nhsx.testhelper.BatchExport.tekExportFromZipFile
import uk.nhs.nhsx.testhelper.BatchExport.tekListFromZipFile
import uk.nhs.nhsx.testhelper.BatchExport.tekSignatureListFromZipFile
import uk.nhs.nhsx.testhelper.data.asInstant
import uk.nhs.nhsx.testhelper.mocks.FakeS3
import java.io.File
import java.nio.file.Path
import java.time.Duration
import java.time.Instant

class DistributionServiceTest {

    @Test
    fun `should abort outside service window`(@TempDir folder: Path) {
        fun distribute() = DistributionServiceBuilder(folder)
            .build()
            .distributeKeys("2020-07-14T19:30:00Z".asInstant())

        assertThatThrownBy { distribute() }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("CloudWatch Event triggered Lambda at wrong time.")
    }

    @Test
    fun `should not abort if flag is false`(@TempDir folder: Path) {
        val config = BatchProcessingConfig(
            false,
            BucketName.of("dist-zip-bucket-name"),
            "",
            "",
            "",
            ParameterName.of("notempty"),
            ParameterName.of("notempty"),
            Duration.ofMinutes(-15)
        )

        DistributionServiceBuilder(folder)
            .withBatchProcessingConfig(config)
            .build()
            .distributeKeys("2020-07-14T19:30:00Z".asInstant())

        assertDailyExportBatchExists(folder)
    }

    @Test
    fun `distribute keys from submissions on single day`(@TempDir folder: Path) {
        val date = "2020-07-16T07:46:00Z".asInstant()
        val dateBefore = "2020-07-15T07:46:00Z".asInstant()

        DistributionServiceBuilder(folder)
            .withSubmissionRepository(date, dateBefore)
            .build()
            .distributeKeys(date)

        assertDailyExportBatchExists(folder)
        assertTwoHourlyExportBatchExists(folder)

        folder.load("distribution/daily/2020071600.zip").assertTekList {
            assertThat(it).describedAs("keys in latest daily zip file").hasSize(14)
        }

        folder.load("distribution/daily/2020071500.zip").assertTekList {
            assertThat(it).describedAs("keys in earlier daily zip file").isEmpty()
        }
    }

    @Test
    fun `distribute keys from submissions on multiple days`(@TempDir folder: Path) {
        val date = "2020-07-16T07:46:00Z".asInstant()
        val dateBefore = "2020-07-15T07:46:00Z".asInstant()
        val dateBeforeThat = "2020-07-14T07:46:00Z".asInstant()
        val date3DaysBefore = "2020-07-13T07:46:00Z".asInstant()

        DistributionServiceBuilder(folder)
            .withSubmissionRepository(date, dateBefore, dateBeforeThat, date3DaysBefore)
            .build()
            .distributeKeys(date)

        assertDailyExportBatchExists(folder)
        assertTwoHourlyExportBatchExists(folder)

        folder.load("distribution/daily/2020071600.zip").assertTekList {
            assertThat(it)
                .describedAs("keys in latest daily zip file")
                .hasSize(14)
        }

        folder.load("distribution/daily/2020071500.zip").assertTekList {
            assertThat(it)
                .describedAs("keys in earlier daily zip file")
                .hasSize(13)
        }

        folder.load("distribution/daily/2020071400.zip").assertTekList {
            assertThat(it)
                .describedAs("keys in even earlier daily zip file")
                .hasSize(12)
        }
    }

    @Test
    fun `tek export has correct signature info`(@TempDir folder: Path) {
        val date = "2020-07-16T07:46:00Z".asInstant()

        DistributionServiceBuilder(folder)
            .withSubmissionRepository(date)
            .build()
            .distributeKeys(date)

        folder.load("distribution/daily/2020071600.zip").assertTekImport {
            assertThat(it.signatureInfosList).hasSize(1)
            assertThat(it.signatureInfosList).first().isEqualTo(expectedSignatureInfo)
        }
    }

    @Test
    fun `tek signature list has correct signature info`(@TempDir folder: Path) {
        val date = "2020-07-16T07:46:00Z".asInstant()

        DistributionServiceBuilder(folder)
            .withSubmissionRepository(date)
            .build()
            .distributeKeys(date)

        folder.load("distribution/daily/2020071600.zip").assertTkSignatureList {
            assertThat(it.signaturesList).hasSize(1)
            assertThat(it.signaturesList)
                .first()
                .extracting { i -> i.signatureInfo }
                .isEqualTo(expectedSignatureInfo)
        }
    }

    @Test
    fun `deletes old objects that dont match uploaded`(@TempDir folder: Path) {
        val date = "2020-07-16T07:46:00Z".asInstant()
        val notMatchedObjectKey = ObjectKey.of("obj-key-not-uploaded")
        val batchProcessingConfig = BatchProcessingConfigs.standard()

        val awsS3 = FakeS3().add(batchProcessingConfig.zipBucketName, notMatchedObjectKey)

        DistributionServiceBuilder(folder)
            .withSubmissionRepository(date)
            .withAwsS3(awsS3)
            .withBatchProcessingConfig(batchProcessingConfig)
            .build()
            .distributeKeys(date)

        assertThat(awsS3.deleted).contains(
            Locator.of(batchProcessingConfig.zipBucketName, notMatchedObjectKey)
        )
    }

    @Test
    fun `no deletion if object key matches uploaded`(@TempDir folder: Path) {
        val date = "2020-07-16T07:46:00Z".asInstant()
        val matchedObjectKey = ObjectKey.of("distribution/daily/2020070300.zip")
        val batchProcessingConfig = BatchProcessingConfigs.standard()

        val awsS3 = FakeS3().add(batchProcessingConfig.zipBucketName, matchedObjectKey)

        DistributionServiceBuilder(folder)
            .withSubmissionRepository(date)
            .withAwsS3(awsS3)
            .withBatchProcessingConfig(batchProcessingConfig)
            .build()
            .distributeKeys(date)

        assertThat(awsS3.deleted).isEmpty()
    }

    @Test
    fun `invalidates cloud front caches`(@TempDir folder: Path) {
        val date = "2020-07-16T07:46:00Z".asInstant()
        val awsCloudFront = mockk<AwsCloudFront>()

        every { awsCloudFront.invalidateCache(any(), any()) } returns Unit

        DistributionServiceBuilder(folder)
            .withCloudFront(awsCloudFront)
            .withSubmissionRepository(date)
            .build()
            .distributeKeys(date)

        verify(exactly = 1) {
            awsCloudFront.invalidateCache("dis-id", "dist-pattern-daily")
        }

        verify(exactly = 1) {
            awsCloudFront.invalidateCache("dis-id", "dist-pattern-2hourly")
        }
    }

    @Test
    fun `check daily batch exists at midnight boundary`(@TempDir folder: Path) {
        val date = "2020-09-16T23:47:00Z".asInstant()
        DistributionServiceBuilder(folder)
            .withSubmissionRepository(date).build()
            .distributeKeys(date)

        val distributionDailyDir = folder.load("distribution/daily")

        assertThat(File(distributionDailyDir.path + "/2020091700.zip")).exists()
        assertDailyExportBatchExists(folder)
    }

    private fun assertDailyExportBatchExists(folder: Path) {
        val dir = folder.load("distribution/daily")
        assertThat(dir).exists()
        assertThat(dir.list()!!).hasSize(15)
    }

    private fun assertTwoHourlyExportBatchExists(folder: Path) {
        val dir = folder.load("distribution/two-hourly")
        assertThat(dir).exists()
        assertThat(dir.list()!!).hasSize(168)
    }

    private class MockSubmissionRepository(submissionDates: List<Instant>) : SubmissionRepository {
        private val submissions = mutableListOf<Submission>()

        init {
            submissionDates.forEach { submissions.add(makeKeySet(it)) }
        }

        override fun loadAllSubmissions(
            minimalSubmissionTimeEpocMillisExclusive: Long,
            limit: Int,
            maxResults: Int
        ): List<Submission> {
            return submissions
        }

        private fun makeKeySet(submissionDate: Instant): Submission {
            val mostRecentKeyRollingStart =
                ENIntervalNumber.enIntervalNumberFromTimestamp(submissionDate).enIntervalNumber / 144 * 144

            val keys = LongRange(0, 14)
                .map { makeKey(mostRecentKeyRollingStart - it * 144) }
                .toList()

            return Submission(submissionDate, StoredTemporaryExposureKeyPayload(keys))
        }

        private fun makeKey(keyStartTime: Long): StoredTemporaryExposureKey =
            StoredTemporaryExposureKey("ABC", Math.toIntExact(keyStartTime), 144, 7)
    }

    private val expectedSignatureInfo = Exposure.SignatureInfo.newBuilder()
        .setAndroidPackage("uk.nhs.covid19.internal")
        .setAppBundleId("uk.nhs.covid19.internal")
        .setVerificationKeyVersion("v1")
        .setVerificationKeyId("234")
        .setSignatureAlgorithm("1.2.840.10045.4.3.2")
        .build()


    class DistributionServiceBuilder(folder: Path) {
        private val exposureProtobuf = ExposureProtobuf("uk.nhs.covid19.internal")
        private var submissionRepository: SubmissionRepository = MockSubmissionRepository(emptyList())
        private var keyDistributor: KeyDistributor = SaveToFileKeyDistributor(folder.toFile())
        private var signer = Signer {
            Signature(KeyId.of("key-id"), SigningAlgorithmSpec.ECDSA_SHA_256, byteArrayOf(1, 2, 3))
        }
        private var cloudFront = mockk<AwsCloudFront>().also {
            every { it.invalidateCache(any(), any()) } returns Unit
        }

        private var awsS3: FakeS3 = FakeS3()
        private var batchProcessingConfig = BatchProcessingConfigs.standard()

        fun withSubmissionRepository(vararg dates: Instant): DistributionServiceBuilder {
            this.submissionRepository = MockSubmissionRepository(dates.asList())
            return this
        }

        fun withCloudFront(cloudFront: AwsCloudFront): DistributionServiceBuilder {
            this.cloudFront = cloudFront
            return this
        }

        fun withAwsS3(awsS3: FakeS3): DistributionServiceBuilder {
            this.awsS3 = awsS3
            return this
        }

        fun withBatchProcessingConfig(config: BatchProcessingConfig): DistributionServiceBuilder {
            this.batchProcessingConfig = config
            return this
        }

        fun build(): DistributionService = DistributionService(
            submissionRepository,
            exposureProtobuf,
            keyDistributor,
            signer,
            cloudFront,
            awsS3,
            batchProcessingConfig,
            RecordingEvents(),
            SystemClock.CLOCK
        )
    }

    private object BatchProcessingConfigs {
        fun standard() = BatchProcessingConfig(
            true,
            BucketName.of("dist-zip-bucket-name"),
            "dis-id",
            "dist-pattern-daily",
            "dist-pattern-2hourly",
            ParameterName.of("ssmKeyIdParameterName"),
            ParameterName.of("ssmContentKeyIdParameterName"),
            Duration.ofMinutes(-15)
        )
    }

    private fun FakeS3.add(name: BucketName, key: ObjectKey): FakeS3 {
        this.existing.add(object : S3ObjectSummary() {
            init {
                setBucketName(name.value)
                setKey(key.value)
            }
        })
        return this
    }

    private fun Path.load(otherPath: String): File = File(this.toFile(), otherPath)

    private fun File.assertTekList(fn: (List<TemporaryExposureKey>) -> Unit) = fn(tekListFromZipFile(this))

    private fun File.assertTekImport(fn: (TemporaryExposureKeyExport) -> Unit) = fn(tekExportFromZipFile(this))

    private fun File.assertTkSignatureList(fn: (Exposure.TEKSignatureList) -> Unit) =
        fn(tekSignatureListFromZipFile(this))
}
