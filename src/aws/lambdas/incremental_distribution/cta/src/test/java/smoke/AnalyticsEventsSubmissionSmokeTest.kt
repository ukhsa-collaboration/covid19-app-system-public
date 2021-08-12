package smoke

import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.DeleteObjectsRequest
import com.amazonaws.services.s3.model.S3ObjectSummary
import org.http4k.cloudnative.env.Environment
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import smoke.actors.MobileApp
import smoke.actors.createHandler
import smoke.data.AnalyticsEventsData.analyticsEvents
import smoke.env.SmokeTests
import strikt.api.expectThat
import strikt.assertions.containsKey
import strikt.assertions.get
import strikt.assertions.hasSize
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import uk.nhs.nhsx.core.Json
import uk.nhs.nhsx.testhelper.assertions.S3Assertions.listObjects
import uk.nhs.nhsx.testhelper.assertions.S3Assertions.objectSummaries
import uk.nhs.nhsx.testhelper.assertions.hasStatus
import uk.nhs.nhsx.testhelper.assertions.isNotNullOrBlank
import uk.nhs.nhsx.testhelper.assertions.signatureDateHeader
import uk.nhs.nhsx.testhelper.assertions.signatureHeader
import java.util.*

@Suppress("UNCHECKED_CAST")
class AnalyticsEventsSubmissionSmokeTest {

    private val config = SmokeTests.loadConfig()
    private val client = createHandler(Environment.ENV)

    private val mobileApp = MobileApp(client, config)

    @BeforeEach
    fun clearSubmissionStore() {
        val s3Client = AmazonS3ClientBuilder.defaultClient()

        val keys = s3Client.listObjects(config.analytics_events_submission_store)
            .objectSummaries
            .map(S3ObjectSummary::getKey)
            .toTypedArray()

        if (keys.isNotEmpty()) {
            s3Client.deleteObjects(DeleteObjectsRequest(config.analytics_events_submission_store).withKeys(*keys))
        }
    }

    @Test
    fun `submit valid payload`() {
        val randomUUID = UUID.randomUUID()
        val uploadResponse = mobileApp.submitAnalyticEvents(analyticsEvents(randomUUID).trimIndent())

        expectThat(uploadResponse).hasStatus(OK).and {
            signatureHeader.isNotNullOrBlank()
            signatureDateHeader.isNotNullOrBlank()
        }

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
}
