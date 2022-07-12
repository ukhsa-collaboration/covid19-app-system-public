package smoke

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.DeleteObjectsRequest
import com.amazonaws.services.s3.model.S3ObjectSummary
import org.http4k.cloudnative.env.Environment
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import smoke.actors.MobileApp
import smoke.actors.createHandler
import smoke.data.AnalyticsEventsData.analyticsEvents
import smoke.env.SmokeTests
import strikt.api.expectThat
import strikt.assertions.any
import strikt.assertions.containsKey
import strikt.assertions.get
import strikt.assertions.hasSize
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.startsWith
import uk.nhs.nhsx.core.Json
import uk.nhs.nhsx.testhelper.assertions.S3Assertions.listObjects
import uk.nhs.nhsx.testhelper.assertions.S3Assertions.objectSummaries
import uk.nhs.nhsx.testhelper.assertions.S3ObjectSummaryAssertions.key
import uk.nhs.nhsx.testhelper.assertions.hasStatus
import uk.nhs.nhsx.testhelper.assertions.isNotNullOrBlank
import uk.nhs.nhsx.testhelper.assertions.signatureDateHeader
import uk.nhs.nhsx.testhelper.assertions.signatureHeader
import java.util.*

@Suppress("UNCHECKED_CAST")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AnalyticsEventsSubmissionSmokeTest {

    private val config = SmokeTests.loadConfig()
    private val client = createHandler(Environment.ENV)

    private val mobileApp = MobileApp(client, config)

    @BeforeAll
    fun clearEventsSubmissionParquetStore() {
        // we only clear before the start of this set of smoke tests because there is a minimum delay of 60
        // seconds between submitting a payload and it appearing in the parquet bucket.
        clearSubmissionStore(config.analytics_events_submission_parquet_store)
    }

    @BeforeEach
    fun clearSubmissionStore() {
        clearSubmissionStore(config.analytics_events_submission_store)
    }

    @Test
    fun `submit valid payload`() {
        val randomUUID = UUID.randomUUID()
        submitValidPayload(randomUUID)

        val s3Client = AmazonS3ClientBuilder.defaultClient()
        val objectSummaries = s3Client.listObjects(config.analytics_events_submission_store).objectSummaries

        expectThat(objectSummaries).hasSize(1)

        val s3Object = s3Client.getObject(config.analytics_events_submission_store, objectSummaries[0].key)
        val storedPayload: Map<String, *> = Json.readJsonOrThrow(s3Object.objectContent) as Map<String, *>

        expectThat(storedPayload).containsKey("uuid")

        val metadata = storedPayload["metadata"] as Map<String, *>

        expectThat(metadata)["operatingSystemVersion"].isEqualTo(randomUUID.toString())
    }

    @Test
    fun `submit invalid payload`() {
        val uploadResponse = mobileApp.submitAnalyticEvents("{}")

        expectThat(uploadResponse).hasStatus(BAD_REQUEST).and {
            signatureHeader.isNotNullOrBlank()
            signatureDateHeader.isNotNullOrBlank()
        }

        expectThat(AmazonS3ClientBuilder.defaultClient())
            .listObjects(config.analytics_events_submission_store)
            .objectSummaries
            .isEmpty()
    }

    @Test
    fun `submitted valid payload is converted to parquet`() {
        val ingestionInterval = config.analytics_events_submission_ingestion_interval.toInt()
        if (ingestionInterval > 60) {
            println("This test will take at least ${ingestionInterval / 60.0} minutes to complete")
        }
        val maxWaitMills = (ingestionInterval + 5L) * 1_000  // how long to keep trying before failing the test
        val nEvents = 100

        val randomUuids = (1..nEvents + 1).map { UUID.randomUUID() }
        randomUuids.forEach { uuid -> submitValidPayload(uuid) }

        val s3Client = AmazonS3ClientBuilder.defaultClient()
        val objectSummaries = getObjectSummaries(
            s3Client,
            config.analytics_events_submission_parquet_store,
            timeoutMillis = maxWaitMills,
        )

        expectThat(objectSummaries) {
            not { any { key.startsWith("format-conversion-failed") } }
            hasSize(1)
        }

        val s3Object = s3Client.getObject(objectSummaries[0].bucketName, objectSummaries[0].key)

        // TODO: parse parquet file and test its contents -- this may be tricky due to the ordering of the tests
        //  contents have been manually verified though
    }

    private fun clearSubmissionStore(store: String) {
        val s3Client = AmazonS3ClientBuilder.defaultClient()

        val keys = s3Client.listObjects(store)
            .objectSummaries
            .map(S3ObjectSummary::getKey)
            .toTypedArray()

        if (keys.isNotEmpty()) {
            s3Client.deleteObjects(DeleteObjectsRequest(store).withKeys(*keys))
        }
    }

    private fun submitValidPayload(randomUUID: UUID) {
        val uploadResponse = mobileApp.submitAnalyticEvents(analyticsEvents(randomUUID).trimIndent())

        expectThat(uploadResponse).hasStatus(OK).and {
            signatureHeader.isNotNullOrBlank()
            signatureDateHeader.isNotNullOrBlank()
        }
    }

    private fun getObjectSummaries(
        s3Client: AmazonS3,
        bucketName: String,
        timeoutMillis: Long,
        delay: Long = 1000L
    ): MutableList<S3ObjectSummary> {
        val startTime = System.currentTimeMillis()

        while (true) {
            if (System.currentTimeMillis() > startTime + timeoutMillis) {
                throw Exception("S3 listing did not appear within timeout ($timeoutMillis ms)")
            }

            val objectListing = s3Client.listObjects(bucketName)
            if (objectListing.objectSummaries.size == 0) {
                Thread.sleep(delay)
                continue
            } else {
                return objectListing.objectSummaries
            }
        }
    }
}
