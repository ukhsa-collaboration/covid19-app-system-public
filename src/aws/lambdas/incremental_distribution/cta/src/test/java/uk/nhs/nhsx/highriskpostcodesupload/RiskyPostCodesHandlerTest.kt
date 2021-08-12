@file:Suppress("SameParameterValue")

package uk.nhs.nhsx.highriskpostcodesupload

import com.amazonaws.HttpMethod.GET
import com.amazonaws.HttpMethod.POST
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.core.Status.Companion.METHOD_NOT_ALLOWED
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.UNPROCESSABLE_ENTITY
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.first
import strikt.assertions.getValue
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.Environment.Access.Companion.TEST
import uk.nhs.nhsx.core.SystemClock.CLOCK
import uk.nhs.nhsx.core.aws.cloudfront.AwsCloudFront
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.core.events.RiskyPostDistrictUpload
import uk.nhs.nhsx.testhelper.ContextBuilder.Companion.aContext
import uk.nhs.nhsx.testhelper.ProxyRequestBuilder.request
import uk.nhs.nhsx.testhelper.TestDatedSigner
import uk.nhs.nhsx.testhelper.assertions.AwsRuntimeAssertions.ProxyResponse.body
import uk.nhs.nhsx.testhelper.assertions.AwsRuntimeAssertions.ProxyResponse.status
import uk.nhs.nhsx.testhelper.assertions.contains
import uk.nhs.nhsx.testhelper.assertions.isSameAs
import uk.nhs.nhsx.testhelper.mocks.FakeCsvUploadServiceS3
import uk.nhs.nhsx.testhelper.mocks.getBucket
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

    private val environment = Environment.fromName("test", TEST.apply(environmentSettings))
    private val awsCloudFront = mockk<AwsCloudFront>()
    private val s3Storage = FakeCsvUploadServiceS3()
    private val datedSigner = TestDatedSigner("date")
    private val events = RecordingEvents()
    private val handler = HighRiskPostcodesUploadHandler(
        environment = environment,
        clock = CLOCK,
        events = events,
        authenticator = { true },
        signer = datedSigner,
        s3Storage = s3Storage,
        awsCloudFront = awsCloudFront,
        healthAuthenticator = { true })

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

        val responseEvent = handler.handleRequest(requestEvent, aContext())

        expectThat(responseEvent) {
            status.isSameAs(ACCEPTED)
            body.isEqualTo("successfully uploaded")
        }

        val contentToStore = """{"postDistricts":{"CODE1":"H","CODE2":"M","CODE3":"L"}}"""
        expectThat(datedSigner) {
            get(TestDatedSigner::count).isEqualTo(2)
            get(TestDatedSigner::content)
                .first()
                .isEqualTo("date:".toByteArray() + contentToStore.toByteArray())
        }

        expectThat(s3Storage) {
            getBucket("my-bucket").hasSize(4)
        }

        expectThat(events).contains(RiskyPostDistrictUpload::class)

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

        val responseEvent = handler.handleRequest(requestEvent, aContext())

        expectThat(responseEvent) {
            status.isSameAs(NOT_FOUND)
            body.isNull()
        }

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

        val responseEvent = handler.handleRequest(requestEvent, aContext())

        expectThat(responseEvent) {
            status.isSameAs(METHOD_NOT_ALLOWED)
            body.isNull()
        }

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

        val responseEvent = handler.handleRequest(requestEvent, aContext())

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

        val responseEvent = handler.handleRequest(requestEvent, aContext())

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

        val responseEvent = handler.handleRequest(requestEvent, aContext())

        assertValidationError(responseEvent, "validation error: unable to deserialize payload")
    }

    private fun assertValidationError(responseEvent: APIGatewayProxyResponseEvent, reason: String) {
        expectThat(responseEvent) {
            status.isSameAs(UNPROCESSABLE_ENTITY)
            body.isEqualTo(reason)
        }
        verifyNoMockInteractions()
    }

    private fun verifyNoMockInteractions() {
        verify(exactly = 0) { awsCloudFront.invalidateCache(any(), any()) }
    }
}
