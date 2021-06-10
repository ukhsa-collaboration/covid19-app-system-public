package smoke

import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.DeleteObjectsRequest
import org.assertj.core.api.Assertions.assertThat
import org.http4k.client.JavaHttpClient
import org.http4k.core.Status
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import smoke.actors.MobileApp
import smoke.data.AnalyticsEventsData.analyticsEvents
import smoke.env.SmokeTests
import uk.nhs.nhsx.core.Json
import java.util.UUID

@Suppress("UNCHECKED_CAST")
class AnalyticsEventsSubmissionSmokeTest {

    private val config = SmokeTests.loadConfig()
    private val client = JavaHttpClient()

    private val mobileApp = MobileApp(client, config)

    @BeforeEach
    fun clearSubmissionStore() {
        val s3Client = AmazonS3ClientBuilder.defaultClient()
        val keys = s3Client.listObjects(config.analytics_events_submission_store).objectSummaries.map { it.key }.toTypedArray()
        if (keys.isNotEmpty()) {
            s3Client.deleteObjects(DeleteObjectsRequest(config.analytics_events_submission_store).withKeys(*keys))
        }
    }

    @Test
    fun `submit valid payload`() {

        val randomUUID = UUID.randomUUID()
        val uploadResponse = mobileApp.submitAnalyticEvents(analyticsEvents(randomUUID).trimIndent())

        assertThat(uploadResponse.status).withFailMessage("unexpected http response code").isEqualTo(Status.OK)
        assertThat(uploadResponse.header("X-Amz-Meta-Signature")).withFailMessage("missing signature header").isNotBlank
        assertThat(uploadResponse.header("X-Amz-Meta-Signature-Date")).withFailMessage("missing signature date header").isNotBlank

        val s3Client = AmazonS3ClientBuilder.defaultClient()
        val objectSummaries = s3Client.listObjects(config.analytics_events_submission_store).objectSummaries
        assertThat(objectSummaries).hasSize(1)

        val s3Object = s3Client.getObject(config.analytics_events_submission_store, objectSummaries[0].key)
        val storedPayload: Map<String, *> = Json.readJsonOrThrow(s3Object.objectContent) as Map<String, *>

        assertThat(storedPayload).containsKey("uuid")
        val metadata = storedPayload["metadata"] as Map<String, *>
        assertThat(metadata["operatingSystemVersion"]).isEqualTo(randomUUID.toString())
    }

    @Test
    fun `submit invalid payload`() {
        val uploadResponse = mobileApp.submitAnalyticEvents("{}")

        assertThat(uploadResponse.status).withFailMessage("unexpected http response code").isEqualTo(Status.BAD_REQUEST)
        assertThat(uploadResponse.header("X-Amz-Meta-Signature")).withFailMessage("missing signature header").isNotBlank
        assertThat(uploadResponse.header("X-Amz-Meta-Signature-Date")).withFailMessage("missing signature date header").isNotBlank

        val s3Client = AmazonS3ClientBuilder.defaultClient()
        val objectSummaries = s3Client.listObjects(config.analytics_events_submission_store).objectSummaries
        assertThat(objectSummaries).isEmpty()
    }
}
