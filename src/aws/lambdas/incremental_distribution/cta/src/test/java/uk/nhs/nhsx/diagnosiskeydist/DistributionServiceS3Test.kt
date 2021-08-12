package uk.nhs.nhsx.diagnosiskeydist

import batchZipCreation.Exposure
import batchZipCreation.Exposure.TemporaryExposureKeyExport
import com.amazonaws.services.kms.model.SigningAlgorithmSpec
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.ObjectMetadata
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import strikt.api.Assertion
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.contains
import strikt.assertions.doesNotContain
import strikt.assertions.hasSize
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.map
import strikt.assertions.message
import uk.nhs.nhsx.core.Json
import uk.nhs.nhsx.core.ObjectKeyFilters
import uk.nhs.nhsx.core.UniqueId
import uk.nhs.nhsx.core.aws.cloudfront.AwsCloudFront
import uk.nhs.nhsx.core.aws.s3.AwsS3Client
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.ByteArraySource
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.core.aws.s3.UniqueObjectKeyNameProvider
import uk.nhs.nhsx.core.aws.ssm.ParameterName
import uk.nhs.nhsx.core.aws.xray.Tracing
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.core.signature.KeyId
import uk.nhs.nhsx.core.signature.RFC2616DatedSigner
import uk.nhs.nhsx.core.signature.Signature
import uk.nhs.nhsx.core.signature.Signer
import uk.nhs.nhsx.diagnosiskeydist.DistributionBucketAssertions.contains
import uk.nhs.nhsx.diagnosiskeydist.DistributionBucketAssertions.dailyBatchIsEqualTo
import uk.nhs.nhsx.diagnosiskeydist.DistributionBucketAssertions.doesNotContain
import uk.nhs.nhsx.diagnosiskeydist.DistributionBucketAssertions.isNotEmptyZip
import uk.nhs.nhsx.diagnosiskeydist.DistributionBucketAssertions.tekExport
import uk.nhs.nhsx.diagnosiskeydist.DistributionBucketAssertions.tekSignature
import uk.nhs.nhsx.diagnosiskeydist.DistributionBucketAssertions.twoHourlyBatchIsEqualTo
import uk.nhs.nhsx.diagnosiskeydist.Submissions.x
import uk.nhs.nhsx.diagnosiskeydist.Submissions.y
import uk.nhs.nhsx.diagnosiskeydist.TEKSignatureListAssertions.contains
import uk.nhs.nhsx.diagnosiskeydist.TEKSignatureListAssertions.hasSize
import uk.nhs.nhsx.diagnosiskeydist.TemporaryExposureKeyExportAssertions.contains
import uk.nhs.nhsx.diagnosiskeydist.TemporaryExposureKeyExportAssertions.hasSignatureSize
import uk.nhs.nhsx.diagnosiskeydist.TemporaryExposureKeyExportAssertions.hasSize
import uk.nhs.nhsx.diagnosiskeydist.TemporaryExposureKeyExportAssertions.isEmpty
import uk.nhs.nhsx.diagnosiskeydist.agspec.ENIntervalNumber.Companion.enIntervalNumberFromTimestamp
import uk.nhs.nhsx.diagnosiskeydist.apispec.DailyZIPSubmissionPeriod
import uk.nhs.nhsx.diagnosiskeydist.keydistribution.UploadToS3KeyDistributor
import uk.nhs.nhsx.diagnosiskeydist.s3.SubmissionFromS3Repository
import uk.nhs.nhsx.diagnosiskeyssubmission.TestKitAwareObjectKeyNameProvider
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKey
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKeyPayload
import uk.nhs.nhsx.domain.TestKit
import uk.nhs.nhsx.isolationpayment.IpcTokenIdGenerator
import uk.nhs.nhsx.testhelper.BatchExport
import uk.nhs.nhsx.testhelper.assertions.contains
import uk.nhs.nhsx.testhelper.data.asInstant
import uk.nhs.nhsx.testhelper.s3.TinyS3
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset.UTC
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class DistributionServiceS3Test {

    private val clock = Clock.fixed("2020-07-16T07:46:00Z".asInstant(), UTC)

    @BeforeEach
    fun setup() = Tracing.disableXRayComplaintsForMainClasses()

    @Test
    fun `should abort outside service window`() {
        startS3(clock) { client ->
            val testSetup = TestSetup(clock, client)
            val distributionService = testSetup.distributionService

            expectThrows<IllegalStateException> { distributionService.distributeKeys("2020-07-14T19:30:00Z".asInstant()) }
                .message.isEqualTo("CloudWatch Event triggered Lambda at wrong time.")
        }
    }

    @Test
    fun `should not abort if flag is false`() {
        startS3(clock) { client ->
            val now = clock.instant()

            val testSetup = TestSetup(clock, client) {
                it.copy(shouldAbortOutsideTimeWindow = true)
            }

            val submissionBucket = testSetup.submissionBucket
            val distributionBucket = testSetup.distributionBucket
            val distributionService = testSetup.distributionService

            submissionBucket.create()
            distributionBucket.create()

            distributionService.distributeKeys(now)

            distributionBucket
                .expectThat()
                .dailyBatchIsEqualTo(15)
                .twoHourlyBatchIsEqualTo(168)
        }
    }

    @Test
    fun `distribute keys from submissions on single day`() {
        startS3(clock) { client ->
            val now = clock.instant()

            val testSetup = TestSetup(clock, client)
            val submissionBucket = testSetup.submissionBucket
            val distributionBucket = testSetup.distributionBucket

            submissionBucket
                .create()

            distributionBucket
                .create()

            submissionBucket.addSubmissions(Submissions(y, now, now.minus(Duration.ofDays(1))))

            testSetup.distributionService.distributeKeys(now)

            distributionBucket
                .expectThat()
                .dailyBatchIsEqualTo(15)
                .twoHourlyBatchIsEqualTo(168)
        }
    }

    @Test
    fun `distribute keys from submissions on multiple days`() {
        startS3(clock) { client ->
            val now = clock.instant()
            val yesterday = now.minus(Duration.ofDays(1))
            val twoDaysAgo = now.minus(Duration.ofDays(2))
            val threeDaysAgo = now.minus(Duration.ofDays(3))

            val testSetup = TestSetup(clock, client)
            val submissionBucket = testSetup.submissionBucket
            val distributionBucket = testSetup.distributionBucket

            distributionBucket
                .create()

            submissionBucket
                .create()

            submissionBucket.addSubmissions(Submissions(y, now, yesterday, twoDaysAgo, threeDaysAgo))

            testSetup.distributionService.distributeKeys(now)

            distributionBucket
                .expectThat()
                .dailyBatchIsEqualTo(15)
                .twoHourlyBatchIsEqualTo(168)
                .tekExport(now.dailyZIPSubmissionPeriodS3Key()) { hasSize(14) }
                .tekExport(yesterday.dailyZIPSubmissionPeriodS3Key()) { hasSize(13) }
                .tekExport(twoDaysAgo.dailyZIPSubmissionPeriodS3Key()) { hasSize(12) }
                .tekExport(threeDaysAgo.dailyZIPSubmissionPeriodS3Key()) { hasSize(0) }
        }
    }

    @Test
    fun `tek export has correct signature info`() {
        startS3(clock) { client ->
            val now = clock.instant()

            val testSetup = TestSetup(clock, client)
            val submissionBucket = testSetup.submissionBucket
            val distributionBucket = testSetup.distributionBucket

            submissionBucket
                .create()

            distributionBucket
                .create()

            testSetup.distributionService.distributeKeys(now)

            distributionBucket
                .expectThat()
                .tekExport(now.dailyZIPSubmissionPeriodS3Key()) {
                    hasSignatureSize(1)
                    contains(
                        Exposure.SignatureInfo.newBuilder()
                            .setAppBundleId("uk.nhs.covid19.internal")
                            .setAndroidPackage("uk.nhs.covid19.internal")
                            .setVerificationKeyVersion("v1")
                            .setVerificationKeyId("234")
                            .setSignatureAlgorithm("1.2.840.10045.4.3.2")
                            .build()
                    )
                }
        }
    }

    @Test
    fun `tek signature list has correct signature info`() {
        startS3(clock) { client ->
            val now = clock.instant()

            val testSetup = TestSetup(clock, client)
            val submissionBucket = testSetup.submissionBucket
            val distributionBucket = testSetup.distributionBucket

            submissionBucket
                .create()

            distributionBucket
                .create()

            testSetup.distributionService.distributeKeys(now)

            distributionBucket
                .expectThat()
                .tekSignature(now.dailyZIPSubmissionPeriodS3Key()) {
                    hasSize(1)
                    contains(
                        Exposure.SignatureInfo.newBuilder()
                            .setAndroidPackage("uk.nhs.covid19.internal")
                            .setAppBundleId("uk.nhs.covid19.internal")
                            .setVerificationKeyVersion("v1")
                            .setVerificationKeyId("234")
                            .setSignatureAlgorithm("1.2.840.10045.4.3.2")
                            .build()
                    )
                }
        }
    }

    @Test
    fun `deletes old objects that dont match uploaded`() {
        startS3(clock) { client ->
            val now = clock.instant()

            val testSetup = TestSetup(clock, client)
            val submissionBucket = testSetup.submissionBucket
            val distributionBucket = testSetup.distributionBucket

            distributionBucket
                .create()
                .putZip("obj-key-not-uploaded")

            submissionBucket
                .create()

            submissionBucket.addSubmissions(Submissions(y, now))

            testSetup.distributionService.distributeKeys(now)

            distributionBucket
                .expectThat()
                .dailyBatchIsEqualTo(15)
                .twoHourlyBatchIsEqualTo(168)
                .doesNotContain("obj-key-not-uploaded")
        }
    }

    @Test
    fun `no deletion if object key matches uploaded`() {
        startS3(clock) { client ->
            val now = clock.instant()

            val testSetup = TestSetup(clock, client)
            val submissionBucket = testSetup.submissionBucket
            val distributionBucket = testSetup.distributionBucket

            submissionBucket
                .create()

            distributionBucket
                .create()
                .putZip("distribution/daily/2020070300.zip")
                .debug()

            submissionBucket.addSubmissions(Submissions(y, now))

            testSetup.distributionService.distributeKeys(now)

            distributionBucket
                .expectThat()
                .dailyBatchIsEqualTo(15)
                .twoHourlyBatchIsEqualTo(168)
                .contains("distribution/daily/2020070300.zip")
        }
    }

    @Test
    fun `invalidates cloud front caches`() {
        startS3(clock) { client ->
            val now = clock.instant()

            val testSetup = TestSetup(clock, client)
            val submissionBucket = testSetup.submissionBucket
            val distributionBucket = testSetup.distributionBucket

            submissionBucket
                .create()

            distributionBucket
                .create()

            submissionBucket.addSubmissions(Submissions(y, now))

            testSetup.distributionService.distributeKeys(now)

            distributionBucket
                .expectThat()
                .dailyBatchIsEqualTo(15)
                .twoHourlyBatchIsEqualTo(168)

            val awsCloudFront = testSetup.awsCloudFront

            verify(exactly = 1) {
                awsCloudFront.invalidateCache("dis-id", "dist-pattern-daily")
            }

            verify(exactly = 1) {
                awsCloudFront.invalidateCache("dis-id", "dist-pattern-2hourly")
            }
        }
    }

    @Test
    fun `check daily batch exists at midnight boundary`() {
        val clock = Clock.fixed("2020-09-16T23:47:00Z".asInstant(), UTC)

        startS3(clock) { client ->
            val now = clock.instant()

            val testSetup = TestSetup(clock, client)

            val submissionBucket = testSetup.submissionBucket
            val distributionBucket = testSetup.distributionBucket

            submissionBucket.create()
            distributionBucket.create()

            submissionBucket.addSubmissions(Submissions(y, now))

            testSetup.distributionService.distributeKeys(now)

            distributionBucket
                .expectThat()
                .dailyBatchIsEqualTo(15)
                .twoHourlyBatchIsEqualTo(168)
                .contains("distribution/daily/2020091700.zip")
        }
    }

    @Test
    fun `does not overwrite populated zips within last 14 days`() {
        startS3(clock) { client ->
            val date = "2020-07-16T07:46:00Z".asInstant()
            val oldDate = "2020-07-01T07:46:00Z".asInstant()
            val processingDate = "2020-07-17T07:46:00Z".asInstant()

            val withinPeriod = date.dailyZIPSubmissionPeriodS3Key()
            val outsidePeriod = oldDate.dailyZIPSubmissionPeriodS3Key()

            val testSetup = TestSetup(clock, client)

            val submissionBucket = testSetup.submissionBucket
            val distributionBucket = testSetup.distributionBucket

            submissionBucket
                .create()

            distributionBucket
                .create()
                .putZip(withinPeriod)
                .putZip(outsidePeriod)

            val submissionDates = IntRange(0, 14).map { date.minus(Duration.ofDays(it.toLong())) }

            submissionBucket.addSubmissions(Submissions(x, *submissionDates.toTypedArray()))

            testSetup.distributionService.distributeKeys(processingDate)

            expectThat(testSetup.events).contains(EmptyZipDistributed::class)

            distributionBucket
                .expectThat()
                .tekExport("distribution/daily/2020071800.zip") { isEmpty() }
                .also {
                    listOf(
                        "distribution/daily/2020071700.zip",
                        "distribution/daily/2020071600.zip",
                        "distribution/daily/2020071500.zip",
                        "distribution/daily/2020071400.zip",
                        "distribution/daily/2020071300.zip",
                        "distribution/daily/2020071200.zip",
                        "distribution/daily/2020071100.zip",
                        "distribution/daily/2020071000.zip",
                        "distribution/daily/2020070900.zip",
                        "distribution/daily/2020070800.zip",
                        "distribution/daily/2020070700.zip",
                        "distribution/daily/2020070600.zip",
                        "distribution/daily/2020070500.zip",
                        "distribution/daily/2020070400.zip"
                    ).forEach { k -> it.tekExport(k) { hasSize(14) } }
                }
                .tekExport("distribution/daily/2020070100.zip") { isEmpty() }
        }
    }

    @Test
    fun `overwrite populated file older than 14 days with empty signed ZIP`() {
        startS3(clock) { client ->
            val date = "2020-07-16T07:46:00Z".asInstant()
            val oldDate = "2020-07-01T07:46:00Z".asInstant()
            val processingDate = "2020-07-17T07:46:00Z".asInstant()

            val withinPeriod = date.dailyZIPSubmissionPeriodS3Key()
            val outsidePeriod = oldDate.dailyZIPSubmissionPeriodS3Key()

            val testSetup = TestSetup(clock, client)

            val submissionBucket = testSetup.submissionBucket
            val distributionBucket = testSetup.distributionBucket

            submissionBucket
                .create()

            distributionBucket
                .create()
                .putZip(withinPeriod)
                .putZip(outsidePeriod)
                .putZip("random-file")

            submissionBucket.addSubmissions(Submissions(x, date))

            testSetup.distributionService.distributeKeys(processingDate)

            expectThat(testSetup.events).contains(EmptyZipDistributed::class)

            distributionBucket
                .expectThat()
                .dailyBatchIsEqualTo(16)
                .twoHourlyBatchIsEqualTo(168)
                .doesNotContain("random-file")
                .tekExport("distribution/daily/2020071700.zip") { hasSize(14) }
                .tekExport("distribution/daily/2020071600.zip") { isEmpty() }
                .tekExport("distribution/daily/2020070100.zip") {
                    isEmpty()
                    contains(
                        Exposure.SignatureInfo.newBuilder()
                            .setAndroidPackage("uk.nhs.covid19.internal")
                            .setAppBundleId("uk.nhs.covid19.internal")
                            .setVerificationKeyVersion("v1")
                            .setVerificationKeyId("234")
                            .setSignatureAlgorithm("1.2.840.10045.4.3.2")
                            .build()
                    )
                }
        }
    }

    @Test
    fun `does not override ZIPs older than 20 days`() {
        startS3(clock) { client ->
            val before = "2020-07-15T07:46:00Z".asInstant()
            val processingDate = "2020-07-16T07:46:00Z".asInstant()

            val testSetup = TestSetup(clock, client)

            val submissionBucket = testSetup.submissionBucket
            val distributionBucket = testSetup.distributionBucket

            submissionBucket
                .create()

            distributionBucket
                .create()
                .putZip("distribution/daily/2019030400.zip", "foobar.txt", "Hello World".toByteArray())

            submissionBucket.addSubmissions(Submissions(x, before))

            testSetup.distributionService.distributeKeys(processingDate)


            distributionBucket
                .expectThat()
                .dailyBatchIsEqualTo(16)
                .twoHourlyBatchIsEqualTo(168)
                .isNotEmptyZip("distribution/daily/2019030400.zip")
                .tekExport("distribution/daily/2020071600.zip") { hasSize(14) }
                .tekExport("distribution/daily/2020071500.zip") { isEmpty() }
        }
    }
}

private inline fun startS3(clock: Clock, fn: (AmazonS3) -> Unit) {
    val tinyS3 = TinyS3(0, clock)
    try {
        tinyS3.start()
        fn(tinyS3.client())
    } finally {
        tinyS3.stop()
    }
}

@Suppress("MemberVisibilityCanBePrivate")
class TestSetup(
    val clock: Clock,
    amazonS3: AmazonS3,
    configOverride: (BatchProcessingConfig) -> BatchProcessingConfig = { it }
) {

    val distributionBucketName = BucketName.of("te-local-key-distribution")
    val submissionBucketName = BucketName.of("te-local-key-submission")

    val keyNameProvider = TestKitAwareObjectKeyNameProvider(
        UniqueObjectKeyNameProvider(
            { clock.instant() },
            UniqueId.ID
        ), TestKit.LAB_RESULT
    )

    val submissionBucket = SubmissionBucket(
        submissionBucketName,
        amazonS3,
    ) { keyNameProvider.generateObjectKeyName().value }

    val distributionBucket = DistributionBucket(distributionBucketName, amazonS3)

    val events = RecordingEvents()

    val awsS3Client = AwsS3Client(events, amazonS3)

    val keys = KeyPairSigner.generateKeys()

    val signer = KeyPairSigner(keys)

    val keyDistributor = UploadToS3KeyDistributor(
        awsS3Client,
        RFC2616DatedSigner({ clock.instant() }, signer)
    )

    val awsCloudFront = mockk<AwsCloudFront>().also {
        every { it.invalidateCache(any(), any()) } just runs
    }

    val exposureProtobuf = ExposureProtobuf("uk.nhs.covid19.internal")

    val config = BatchProcessingConfig(
        true,
        distributionBucketName,
        "dis-id",
        "dist-pattern-daily",
        "dist-pattern-2hourly",
        ParameterName.of("ssmKeyIdParameterName"),
        ParameterName.of("ssmContentKeyIdParameterName"),
        Duration.ofMinutes(-15)
    ).let(configOverride)

    val submissionRepository = SubmissionFromS3Repository(
        awsS3 = awsS3Client,
        objectKeyFilter = ObjectKeyFilters.batched().withPrefixes(listOf("LAB_RESULT")),
        submissionBucketName = submissionBucketName,
        events = events
    ) { clock.instant() }

    val distributionService = DistributionService(
        submissionRepository = submissionRepository,
        exposureProtobuf = exposureProtobuf,
        keyDistributor = keyDistributor,
        signer = signer,
        awsCloudFront = awsCloudFront,
        awsS3 = awsS3Client,
        config = config,
        events = events
    ) { clock.instant() }
}

class DistributionBucket(
    val bucketName: BucketName,
    val amazonS3: AmazonS3
) {

    fun create() = apply {
        amazonS3.createBucket(bucketName.value)
    }

    fun debug() = apply {
        amazonS3.listObjects(bucketName.value)
            .objectSummaries
            .forEach { println("# ${bucketName.value}: ${it.key}") }
    }

    fun expectThat() = expectThat(this)

    fun putZip(objectKey: String) = apply {
        val source = ByteArrayOutputStream()
            .also { ZipOutputStream(it).close() }
            .let(ByteArrayOutputStream::toByteArray)
            .let(::ByteArraySource)

        put(objectKey, source)
    }

    fun putZip(objectKey: String, name: String, content: ByteArray) = apply {
        val out = ByteArrayOutputStream().also {
            ZipOutputStream(it).use { zos ->
                zos.putNextEntry(ZipEntry(name))
                zos.write(content)
                zos.closeEntry()
            }
        }

        put(objectKey, ByteArraySource(out.toByteArray()))
    }

    private fun put(objectKey: String, source: ByteArraySource) {
        source.openStream().use {
            val metadata = ObjectMetadata().apply {
                contentType = "application/zip"
                contentLength = source.size.toLong()
            }

            amazonS3.putObject(bucketName.value, objectKey, it, metadata)
        }
    }

    fun getZip(objectKey: String) = amazonS3.getObject(bucketName.value, objectKey)
        .objectContent
        .use { sin -> ZipFileReader.read(sin) }

    fun getTekExportOrNull(objectKey: String) =
        getZip(objectKey)["export.bin"]?.let { BatchExport.tekExportFromExportBin(it) }

    fun getTekSignatureListOrNull(objectKey: String) =
        getZip(objectKey)["export.sig"]?.let { BatchExport.tekSignatureListFromSig(it) }
}

class SubmissionBucket(
    val bucketName: BucketName,
    val amazonS3: AmazonS3,
    private val namingFn: () -> String
) {

    fun create() = apply {
        amazonS3.createBucket(bucketName.value)
    }

    fun expectThat() = expectThat(this)

    fun debug() = apply {
        amazonS3.listObjects(bucketName.value)
            .objectSummaries
            .forEach { println("# ${bucketName.value}: ${it.key}") }
    }

    fun debugContents() = apply {
        amazonS3.listObjects(bucketName.value)
            .objectSummaries
            .forEach {
                println("# ${bucketName.value}: ${it.key}")
                println(amazonS3.getObjectAsString(bucketName.value, it.key))
                println()
            }
    }

    fun addSubmissions(submissions: List<Submission>) = apply {
        submissions.forEach {
            val objectKey = namingFn.invoke()

            val source = ByteArraySource
                .fromUtf8String(Json.toJson(it.payload))

            source.openStream().use { bout ->
                amazonS3.putObject(
                    bucketName.value,
                    objectKey,
                    bout,
                    ObjectMetadata().apply {
                        contentLength = source.size.toLong()
                        setHeader("x-amz-meta-last-modified", it.submissionDate.toString())
                    }
                )
            }
        }
    }
}

typealias RollingStartGenerator = (timestamp: Instant) -> List<Long>

object Submissions {

    val x: RollingStartGenerator = { t ->
        val mostRecentKeyRollingStart = enIntervalNumberFromTimestamp(t).enIntervalNumber / 144 * 144
        LongRange(0, 13).map { mostRecentKeyRollingStart }.toList()
    }

    val y: RollingStartGenerator = { t ->
        val mostRecentKeyRollingStart = enIntervalNumberFromTimestamp(t).enIntervalNumber / 144 * 144
        LongRange(0, 14).map { mostRecentKeyRollingStart - it * 144 }.toList()
    }

    operator fun invoke(g: RollingStartGenerator, vararg timestamps: Instant): List<Submission> =
        timestamps.map { createSubmissionFor(g, it) }

    private fun createSubmissionFor(g: RollingStartGenerator, submissionTimestamp: Instant): Submission {
        val keys = g(submissionTimestamp)
            .map {
                StoredTemporaryExposureKey(
                    IpcTokenIdGenerator.getToken().value,
                    Math.toIntExact(it),
                    144,
                    7
                )
            }.toList()
        return Submission(
            submissionTimestamp,
            ObjectKey.of("mobile/LAB_RESULT/abc"),
            StoredTemporaryExposureKeyPayload(keys)
        )
    }
}

object SubmissionBucketAssertions {
    fun Assertion.Builder<SubmissionBucket>.isEmpty() {
        assert("is empty") { submissionBucket ->
            val bucketName = submissionBucket.bucketName.value
            val amazonS3 = submissionBucket.amazonS3
            val summaries = amazonS3.listObjects(bucketName).objectSummaries
            get("object summaries") { summaries }.isEmpty()
        }
    }
}

object DistributionBucketAssertions {
    fun Assertion.Builder<DistributionBucket>.isEmpty() = apply {
        assert("is empty") { distributionBucket ->
            val bucketName = distributionBucket.bucketName.value
            val amazonS3 = distributionBucket.amazonS3
            val summaries = amazonS3.listObjects(bucketName).objectSummaries
            get("object summaries") { summaries }.isEmpty()
        }
    }

    fun Assertion.Builder<DistributionBucket>.dailyBatchIsEqualTo(expected: Int) = apply {
        assert("should have created daily batch with the prefix: distribution/daily") { distributionBucket ->
            val bucketName = distributionBucket.bucketName.value
            val amazonS3 = distributionBucket.amazonS3
            val summaries = amazonS3.listObjects(bucketName, "distribution/daily").objectSummaries
            get("object summaries") { summaries }.hasSize(expected)
        }
    }

    fun Assertion.Builder<DistributionBucket>.twoHourlyBatchIsEqualTo(expected: Int) = apply {
        assert("should have created two hourly batch with the prefix: distribution/two-hourly") { distributionBucket ->
            val bucketName = distributionBucket.bucketName.value
            val amazonS3 = distributionBucket.amazonS3
            val summaries = amazonS3.listObjects(bucketName, "distribution/two-hourly").objectSummaries
            get("object summaries") { summaries }.hasSize(expected)
        }
    }

    fun Assertion.Builder<DistributionBucket>.contains(expected: String) = apply {
        assert("contains %s", expected) { distributionBucket ->
            val bucketName = distributionBucket.bucketName.value
            val amazonS3 = distributionBucket.amazonS3
            val summaries = amazonS3.listObjects(bucketName).objectSummaries
            get("object summaries") { summaries }.map { it.key }.contains(expected)
        }
    }

    fun Assertion.Builder<DistributionBucket>.doesNotContain(expected: String) = apply {
        assert("does not contain %s", expected) { distributionBucket ->
            val bucketName = distributionBucket.bucketName.value
            val amazonS3 = distributionBucket.amazonS3
            val summaries = amazonS3.listObjects(bucketName).objectSummaries
            get("object summaries") { summaries }.map { it.key }.doesNotContain(expected)
        }
    }

    fun Assertion.Builder<DistributionBucket>.isNotEmptyZip(objectKey: String) = apply {
        assert("zip is not empty %s", objectKey) { distributionBucket ->
            distributionBucket.getZip(objectKey).isNotEmpty()
        }
    }

    fun Assertion.Builder<DistributionBucket>.tekExport(
        objectKey: String,
        fn: Assertion.Builder<TemporaryExposureKeyExport>.() -> Unit
    ) = apply {
        get("get tek export") { getTekExportOrNull(objectKey) }
            .describedAs("TekExport for $objectKey does not exist")
            .isNotNull()
            .and(fn)
    }

    fun Assertion.Builder<DistributionBucket>.tekSignature(
        objectKey: String,
        fn: Assertion.Builder<Exposure.TEKSignatureList>.() -> Unit
    ) = apply {
        get("get tek signature") { getTekSignatureListOrNull(objectKey) }
            .describedAs("TEKSignatureList for $objectKey does not exist")
            .isNotNull()
            .and(fn)
    }
}

object TEKSignatureListAssertions {
    fun Assertion.Builder<Exposure.TEKSignatureList>.hasSize(expected: Int) = apply {
        get(Exposure.TEKSignatureList::getSignaturesCount).isEqualTo(expected)
    }

    fun Assertion.Builder<Exposure.TEKSignatureList>.contains(expected: Exposure.SignatureInfo) = apply {
        get(Exposure.TEKSignatureList::getSignaturesList).map { it.signatureInfo }.contains(expected)
    }
}

object TemporaryExposureKeyExportAssertions {
    fun Assertion.Builder<TemporaryExposureKeyExport>.hasSignatureSize(expected: Int) = apply {
        get(TemporaryExposureKeyExport::getSignatureInfosList).hasSize(expected)
    }

    fun Assertion.Builder<TemporaryExposureKeyExport>.contains(expected: Exposure.SignatureInfo) = apply {
        get(TemporaryExposureKeyExport::getSignatureInfosList).contains(expected)
    }

    fun Assertion.Builder<TemporaryExposureKeyExport>.hasSize(expected: Int) = apply {
        get(TemporaryExposureKeyExport::getKeysCount).isEqualTo(expected)
    }

    fun Assertion.Builder<TemporaryExposureKeyExport>.isEmpty() = apply {
        hasSize(0)
    }
}

class KeyPairSigner(private val keyPair: KeyPair) : Signer {
    override fun sign(bytes: ByteArray): Signature {
        val signed = java.security.Signature.getInstance("SHA1WithRSA").apply {
            initSign(keyPair.private)
        }.apply {
            update(bytes)
        }.sign()

        return Signature(
            KeyId.of("key-pair-signer"),
            SigningAlgorithmSpec.ECDSA_SHA_256,
            signed
        )
    }

    companion object {
        fun generateKeys(): KeyPair = KeyPairGenerator.getInstance("RSA")
            .apply { initialize(1024) }
            .generateKeyPair()
    }
}

fun Instant.dailyZIPSubmissionPeriodS3Key() = DailyZIPSubmissionPeriod.zipPathFor(this)

object ZipFileReader {
    fun read(input: InputStream) = input.use { read(ZipInputStream(it)) }

    private fun read(input: ZipInputStream): Map<String, ByteArray> {
        val contents = mutableMapOf<String, ByteArray>()

        input.use { zin ->
            var entry = zin.nextEntry

            while (entry != null) {
                val name = entry.name
                val buffer = ByteArray(1024)
                ByteArrayOutputStream().use { out ->
                    var length: Int
                    while (zin.read(buffer).also { length = it } > 0) {
                        out.write(buffer, 0, length)
                    }
                    contents.put(name, out.toByteArray())
                }
                entry = zin.nextEntry
            }
        }

        return contents.toMap()
    }
}
