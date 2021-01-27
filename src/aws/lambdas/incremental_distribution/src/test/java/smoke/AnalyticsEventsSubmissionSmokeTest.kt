package smoke

import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.DeleteObjectsRequest
import org.assertj.core.api.Assertions.assertThat
import org.http4k.client.JavaHttpClient
import org.http4k.core.Status
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import smoke.actors.MobileApp
import smoke.env.SmokeTests
import uk.nhs.nhsx.core.Jackson
import java.util.UUID

@Suppress("UNCHECKED_CAST")
class AnalyticsEventsSubmissionSmokeTest {

    private val config = SmokeTests.loadConfig()
    private val client = JavaHttpClient()

    private val mobileApp = MobileApp(client, config)

    @BeforeEach
    fun clearSubmissionStore() {
        val s3Client = AmazonS3ClientBuilder.defaultClient()
        val keys = s3Client.listObjects(config.analyticsEventsSubmissionStore).objectSummaries.map { it.key }.toTypedArray()
        if (keys.isNotEmpty()) {
            s3Client.deleteObjects(DeleteObjectsRequest(config.analyticsEventsSubmissionStore).withKeys(*keys))
        }
    }

    @Test
    fun `submit valid payload`() {

        val randomUUID = UUID.randomUUID()
        val uploadResponse = mobileApp.submitAnalyticEvents("""
                {
                    "metadata": {
                        "operatingSystemVersion": "$randomUUID",
                        "latestApplicationVersion": "3.0",
                        "deviceModel": "iPhone11,2",
                        "postalDistrict": "A1"
                    },
                    "events": [
                        {
                            "type": "exposure_window",
                            "version": 1,
                            "payload": {
                                "date": "2020-08-24T21:59:00Z",
                                "infectiousness": "high|none|standard",
                                "scanInstances": [
                                    {
                                        "minimumAttenuation": 1,
                                        "secondsSinceLastScan": 5,
                                        "typicalAttenuation": 2
                                    }
                                ],
                                "riskScore": "FIXME: sample int value (range?) or string value (enum?)"
                            }
                        }
                    ]
                }
            """.trimIndent())

        assertThat(uploadResponse.status).withFailMessage("unexpected http response code").isEqualTo(Status.OK)
        assertThat(uploadResponse.header("X-Amz-Meta-Signature")).withFailMessage("missing signature header").isNotBlank()
        assertThat(uploadResponse.header("X-Amz-Meta-Signature-Date")).withFailMessage("missing signature date header").isNotBlank()

        val s3Client = AmazonS3ClientBuilder.defaultClient()
        val objectSummaries = s3Client.listObjects(config.analyticsEventsSubmissionStore).objectSummaries
        assertThat(objectSummaries).hasSize(1)

        val s3Object = s3Client.getObject(config.analyticsEventsSubmissionStore, objectSummaries[0].key)
        val storedPayload: Map<String, *> = Jackson.readJson(s3Object.objectContent, Map::class.java) as Map<String, *>

        assertThat(storedPayload).containsKey("uuid")
        val metadata = storedPayload["metadata"] as Map<String, *>
        assertThat(metadata["operatingSystemVersion"]).isEqualTo(randomUUID.toString())
    }

    @Test
    fun `submit invalid payload`() {
        val uploadResponse = mobileApp.submitAnalyticEvents("{}")

        assertThat(uploadResponse.status).withFailMessage("unexpected http response code").isEqualTo(Status.BAD_REQUEST)
        assertThat(uploadResponse.header("X-Amz-Meta-Signature")).withFailMessage("missing signature header").isNotBlank()
        assertThat(uploadResponse.header("X-Amz-Meta-Signature-Date")).withFailMessage("missing signature date header").isNotBlank()

        val s3Client = AmazonS3ClientBuilder.defaultClient()
        val objectSummaries = s3Client.listObjects(config.analyticsEventsSubmissionStore).objectSummaries
        assertThat(objectSummaries).isEmpty()
    }
}