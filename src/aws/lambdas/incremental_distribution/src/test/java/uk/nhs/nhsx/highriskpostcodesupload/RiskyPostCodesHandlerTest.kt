package uk.nhs.nhsx.highriskpostcodesupload

import com.amazonaws.HttpMethod
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.google.common.primitives.Bytes
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.testhelper.ContextBuilder
import uk.nhs.nhsx.testhelper.ProxyRequestBuilder
import uk.nhs.nhsx.testhelper.TestDatedSigner
import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.aws.cloudfront.AwsCloudFront
import uk.nhs.nhsx.core.exceptions.HttpStatusCode
import uk.nhs.nhsx.testhelper.mocks.FakeCsvUploadServiceS3
import java.nio.charset.StandardCharsets

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
        "MAINTENANCE_MODE" to "FALSE"
    )

    private val environment = Environment.fromName("test", Environment.Access.TEST.apply(environmentSettings))
    private val awsCloudFront = mockk<AwsCloudFront>()
    private val s3Storage = FakeCsvUploadServiceS3()
    private val datedSigner = TestDatedSigner("date")
    private val handler = HighRiskPostcodesUploadHandler(environment, { true }, datedSigner, s3Storage, awsCloudFront)

    @Test
    fun `accepts payload`() {
        every { awsCloudFront.invalidateCache(any(), any()) } just Runs
        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath("/upload/high-risk-postal-districts")
            .withBearerToken("anything")
            .withJson(payload)
            .build()

        val responseEvent = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(responseEvent.statusCode).isEqualTo(HttpStatusCode.ACCEPTED_202.code)
        assertThat(responseEvent.body).isEqualTo("successfully uploaded")

        val contentToStore = """{"postDistricts":{"CODE2":"M","CODE1":"H","CODE3":"L"}}"""
        assertThat(datedSigner.count).isEqualTo(2)
        assertThat(datedSigner.content[0]).isEqualTo(Bytes.concat("date:".toByteArray(StandardCharsets.UTF_8), contentToStore.toByteArray(StandardCharsets.UTF_8)))
        assertThat(s3Storage.count).isEqualTo(4)
        assertThat(s3Storage.bucket.value).isEqualTo("my-bucket")

        verify(exactly = 1) { awsCloudFront.invalidateCache("my-distribution", "invalidation-pattern") }
    }

    @Test
    fun `not found when path is wrong`() {
        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath("dodgy")
            .withBearerToken("anything")
            .withJson(payload)
            .build()

        val responseEvent = handler.handleRequest(requestEvent, ContextBuilder.aContext())

        assertThat(responseEvent.statusCode).isEqualTo(HttpStatusCode.NOT_FOUND_404.code)
        assertThat(responseEvent.body).isEqualTo(null)
        verifyNoMockInteractions()
    }

    @Test
    fun `not allowed when method is wrong`() {
        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.GET)
            .withPath("/upload/high-risk-postal-districts")
            .withBearerToken("anything")
            .withJson(payload)
            .build()

        val responseEvent = handler.handleRequest(requestEvent, ContextBuilder.aContext())

        assertThat(responseEvent.statusCode).isEqualTo(HttpStatusCode.METHOD_NOT_ALLOWED_405.code)
        assertThat(responseEvent.body).isEqualTo(null)
        verifyNoMockInteractions()
    }

    @Test
    fun `unprocessable when wrong content type`() {
        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath("/upload/high-risk-postal-districts")
            .withBearerToken("anything")
            .withCsv("some random csv")
            .build()

        val responseEvent = handler.handleRequest(requestEvent, ContextBuilder.aContext())

        assertValidationError(responseEvent, "validation error: unable to deserialize payload")
    }

    @Test
    fun `unprocessable when no body`() {
        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath("/upload/high-risk-postal-districts")
            .withBearerToken("anything")
            .withJson(null)
            .build()

        val responseEvent = handler.handleRequest(requestEvent, ContextBuilder.aContext())

        assertValidationError(responseEvent, "validation error: unable to deserialize payload")
    }

    @Test
    fun `unprocessable when empty body`() {
        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath("/upload/high-risk-postal-districts")
            .withBearerToken("anything")
            .withJson("")
            .build()

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