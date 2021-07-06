package uk.nhs.nhsx.highriskpostcodesupload

import com.amazonaws.HttpMethod.GET
import com.amazonaws.HttpMethod.POST
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.SystemClock.CLOCK
import uk.nhs.nhsx.core.aws.cloudfront.AwsCloudFront
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.core.events.RiskyPostDistrictUpload
import uk.nhs.nhsx.core.exceptions.HttpStatusCode
import uk.nhs.nhsx.core.exceptions.HttpStatusCode.METHOD_NOT_ALLOWED_405
import uk.nhs.nhsx.testhelper.ContextBuilder
import uk.nhs.nhsx.testhelper.ProxyRequestBuilder.request
import uk.nhs.nhsx.testhelper.TestDatedSigner
import uk.nhs.nhsx.testhelper.mocks.FakeCsvUploadServiceS3
import uk.nhs.nhsx.testhelper.withBearerToken
import uk.nhs.nhsx.testhelper.withCsv
import uk.nhs.nhsx.testhelper.withCustomOai
import uk.nhs.nhsx.testhelper.withJson
import uk.nhs.nhsx.testhelper.withMethod
import uk.nhs.nhsx.testhelper.withRequestId

class RiskyPostCodesHandlerTest {

    private val payload = """{
      "postDistricts": {
        "CODE1": {
          "riskIndicator": "H",
          "tierIndicator": "EN.Tier3"
        },
        "CODE2": {
          "riskIndicator": "M",
          "tierIndicator": "EN.Tier2"
        },
        "CODE3": {
          "riskIndicator": "L",
          "tierIndicator": "EN.Tier1"
        }
      },
      "localAuthorities": {
        "A1": {
          "tierIndicator": "EN.Tier1"
        },
        "A2": {
          "tierIndicator": "EN.Tier2"
        },
        "A3": {
          "tierIndicator": "EN.Tier2"
        }
      }
    }""".trimIndent()

    private val environmentSettings = mapOf(
        "BUCKET_NAME" to "my-bucket",
        "DISTRIBUTION_ID" to "my-distribution",
        "DISTRIBUTION_INVALIDATION_PATTERN" to "invalidation-pattern",
        "MAINTENANCE_MODE" to "FALSE",
        "custom_oai" to "OAI"
    )

    private val environment = Environment.fromName("test", Environment.Access.TEST.apply(environmentSettings))
    private val awsCloudFront = mockk<AwsCloudFront>()
    private val s3Storage = FakeCsvUploadServiceS3()
    private val datedSigner = TestDatedSigner("date")
    private val events = RecordingEvents()
    private val handler = HighRiskPostcodesUploadHandler(
        environment, CLOCK, events, { true }, datedSigner, s3Storage, awsCloudFront, { true }
    )

    @Test
    fun `accepts payload`() {
        every { awsCloudFront.invalidateCache(any(), any()) } just Runs
        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/upload/high-risk-postal-districts")
            .withBearerToken("anything")
            .withJson(payload)

        val responseEvent = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(responseEvent.statusCode).isEqualTo(HttpStatusCode.ACCEPTED_202.code)
        assertThat(responseEvent.body).isEqualTo("successfully uploaded")

        val contentToStore = """{"postDistricts":{"CODE1":"H","CODE2":"M","CODE3":"L"}}"""
        assertThat(datedSigner.count).isEqualTo(2)
        assertThat(datedSigner.content[0]).isEqualTo("date:".toByteArray() + contentToStore.toByteArray())
        assertThat(s3Storage.count).isEqualTo(4)
        assertThat(s3Storage.bucket.value).isEqualTo("my-bucket")
        events.contains(RiskyPostDistrictUpload::class)

        verify(exactly = 1) { awsCloudFront.invalidateCache("my-distribution", "invalidation-pattern") }
    }

    @Test
    fun `not found when path is wrong`() {
        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("dodgy")
            .withBearerToken("anything")
            .withJson(payload)

        val responseEvent = handler.handleRequest(requestEvent, ContextBuilder.aContext())

        assertThat(responseEvent.statusCode).isEqualTo(HttpStatusCode.NOT_FOUND_404.code)
        assertThat(responseEvent.body).isEqualTo(null)
        verifyNoMockInteractions()
    }

    @Test
    fun `not allowed when method is wrong`() {
        val requestEvent = request()
            .withMethod(GET)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/upload/high-risk-postal-districts")
            .withBearerToken("anything")
            .withJson(payload)

        val responseEvent = handler.handleRequest(requestEvent, ContextBuilder.aContext())

        assertThat(responseEvent.statusCode).isEqualTo(METHOD_NOT_ALLOWED_405.code)
        assertThat(responseEvent.body).isEqualTo(null)
        verifyNoMockInteractions()
    }

    @Test
    fun `unprocessable when wrong content type`() {
        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/upload/high-risk-postal-districts")
            .withBearerToken("anything")
            .withCsv("some random csv")

        val responseEvent = handler.handleRequest(requestEvent, ContextBuilder.aContext())

        assertValidationError(responseEvent, "validation error: unable to deserialize payload")
    }

    @Test
    fun `unprocessable when no body`() {
        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/upload/high-risk-postal-districts")
            .withBearerToken("anything")
            .withJson(null)

        val responseEvent = handler.handleRequest(requestEvent, ContextBuilder.aContext())

        assertValidationError(responseEvent, "validation error: unable to deserialize payload")
    }

    @Test
    fun `unprocessable when empty body`() {
        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/upload/high-risk-postal-districts")
            .withBearerToken("anything")
            .withJson("")

        val responseEvent = handler.handleRequest(requestEvent, ContextBuilder.aContext())

        assertValidationError(responseEvent, "validation error: unable to deserialize payload")
    }

    private fun assertValidationError(responseEvent: APIGatewayProxyResponseEvent, reason: String) {
        assertThat(responseEvent.statusCode).isEqualTo(HttpStatusCode.UNPROCESSABLE_ENTITY_422.code)
        assertThat(responseEvent.body).isEqualTo(reason)
        verifyNoMockInteractions()
    }

    private fun verifyNoMockInteractions() {
        verify(exactly = 0) { awsCloudFront.invalidateCache(any(), any()) }
    }
}
