package uk.nhs.nhsx.virology

import com.amazonaws.HttpMethod
import com.amazonaws.HttpMethod.GET
import com.amazonaws.HttpMethod.POST
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.natpryce.snodge.json.defaultJsonMutagens
import com.natpryce.snodge.json.forStrings
import com.natpryce.snodge.mutants
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.CONFLICT
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.METHOD_NOT_ALLOWED
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNPROCESSABLE_ENTITY
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import uk.nhs.nhsx.core.TestEnvironments.TEST
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.core.events.UnprocessableJson
import uk.nhs.nhsx.core.events.VirologyResults
import uk.nhs.nhsx.core.events.VirologyTokenGen
import uk.nhs.nhsx.core.events.VirologyTokenStatus
import uk.nhs.nhsx.domain.CtaToken
import uk.nhs.nhsx.domain.TestEndDate
import uk.nhs.nhsx.domain.TestKit.LAB_RESULT
import uk.nhs.nhsx.domain.TestKit.RAPID_RESULT
import uk.nhs.nhsx.domain.TestKit.RAPID_SELF_REPORTED
import uk.nhs.nhsx.domain.TestResult.Positive
import uk.nhs.nhsx.testhelper.ContextBuilder.Companion.aContext
import uk.nhs.nhsx.testhelper.ProxyRequestBuilder.request
import uk.nhs.nhsx.testhelper.assertions.AwsRuntimeAssertions.ProxyResponse.body
import uk.nhs.nhsx.testhelper.assertions.AwsRuntimeAssertions.ProxyResponse.status
import uk.nhs.nhsx.testhelper.assertions.contains
import uk.nhs.nhsx.testhelper.assertions.isEqualToJson
import uk.nhs.nhsx.testhelper.assertions.isSameAs
import uk.nhs.nhsx.testhelper.data.TestData.rapidLabResultV2
import uk.nhs.nhsx.testhelper.data.TestData.rapidSelfReportedResultV2
import uk.nhs.nhsx.testhelper.data.TestData.testLabResultV1
import uk.nhs.nhsx.testhelper.data.TestData.tokenGenPayloadV1
import uk.nhs.nhsx.testhelper.data.TestData.tokenGenPayloadV2
import uk.nhs.nhsx.testhelper.data.TestData.tokenGenSelfReportedPayloadV2
import uk.nhs.nhsx.testhelper.data.TestData.tokenStatusPayloadV2
import uk.nhs.nhsx.testhelper.withBearerToken
import uk.nhs.nhsx.testhelper.withCustomOai
import uk.nhs.nhsx.testhelper.withJson
import uk.nhs.nhsx.testhelper.withMethod
import uk.nhs.nhsx.testhelper.withRequestId
import uk.nhs.nhsx.virology.persistence.VirologyResultPersistOperation.OrderNotFound
import uk.nhs.nhsx.virology.persistence.VirologyResultPersistOperation.Success
import uk.nhs.nhsx.virology.persistence.VirologyResultPersistOperation.TransactionFailed
import uk.nhs.nhsx.virology.result.VirologyTokenGenRequestV2
import uk.nhs.nhsx.virology.result.VirologyTokenGenResponse
import uk.nhs.nhsx.virology.result.VirologyTokenStatusRequest
import uk.nhs.nhsx.virology.result.VirologyTokenStatusResponse
import kotlin.random.Random

class VirologyUploadHandlerTest {

    private val npexPath = "/upload/virology-test/npex-result"
    private val npexV2Path = "/upload/virology-test/v2/npex-result"
    private val tokenGenWlsPathV1 = "/upload/virology-test/wls-result-tokengen"
    private val tokenGenEngPathV1 = "/upload/virology-test/eng-result-tokengen"
    private val fioranoPathV1 = "/upload/virology-test/fiorano-result"
    private val fioranoPathV2 = "/upload/virology-test/v2/fiorano-result"
    private val tokenGenEngPathV2 = "/upload/virology-test/v2/eng-result-tokengen"
    private val tokenGenWlsPathV2 = "/upload/virology-test/v2/wls-result-tokengen"
    private val tokenStatusEngPathV2 = "/upload/virology-test/v2/eng-result-tokenstatus"
    private val tokenStatusWlsPathV2 = "/upload/virology-test/v2/wls-result-tokenstatus"
    private val events = RecordingEvents()
    private val service = mockk<VirologyService>()

    @Test
    fun `accepts npex test result v1 returns 202`() {
        every { service.acceptTestResult(any()) } returns Success()

        val response = sendAndReceive(path = npexPath, payload = testLabResultV1)

        expectThat(response).status.isSameAs(ACCEPTED)
        expectThat(response).body.isEqualTo("successfully processed")
        expectThat(events).contains(VirologyResults::class, TestResultUploaded::class)
    }

    @Test
    fun `test order is not present returns 400`() {
        every { service.acceptTestResult(any()) } returns OrderNotFound()

        val response = sendAndReceive(path = npexPath, payload = testLabResultV1)

        expectThat(response).status.isSameAs(BAD_REQUEST)
        expectThat(response).body.isNull()
        expectThat(events).contains(VirologyResults::class, TestResultUploaded::class)
    }

    @Test
    fun `transaction failure returns 409`() {
        every { service.acceptTestResult(any()) } returns TransactionFailed()

        val response = sendAndReceive(path = npexPath, payload = testLabResultV1)

        expectThat(response).status.isSameAs(CONFLICT)
        expectThat(response).body.isNull()
        expectThat(events).contains(VirologyResults::class, TestResultUploaded::class)
    }

    @Test
    fun `invalid path returns 404`() {
        val response = sendAndReceive(path = "/upload/incorrect/npex-result", payload = testLabResultV1)

        expectThat(response).status.isSameAs(NOT_FOUND)
        expectThat(response).body.isNull()
    }

    @Test
    fun `invalid method returns 405`() {
        val response = sendAndReceive(path = npexPath, payload = testLabResultV1, method = GET)

        expectThat(response).status.isSameAs(METHOD_NOT_ALLOWED)
        expectThat(response).body.isNull()
    }

    @Test
    fun `empty body returns 422`() {
        val response = sendAndReceive(path = npexPath, payload = "")

        expectThat(response).status.isSameAs(UNPROCESSABLE_ENTITY)
        expectThat(response).body.isNull()
        expectThat(events).contains(VirologyResults::class, UnprocessableJson::class)
    }

    @Test
    fun `bad date returns 422`() {
        val response = sendAndReceive(
            path = npexPath,
            payload = """{"ctaToken": "cc8f0b6z","testEndDate": "2020-04-23T00:FOO:00Z","testResult": "NEGATIVE"}"""
        )

        expectThat(response).status.isSameAs(UNPROCESSABLE_ENTITY)
        expectThat(response).body.isNull()
        expectThat(events).contains(VirologyResults::class, UnprocessableJson::class)
    }

    @Test
    fun `random payload does not cause 500`() {
        every { service.acceptTestResult(any()) } returns Success()

        Random.mutants(defaultJsonMutagens().forStrings(), 100, testLabResultV1)
            .filter { it != testLabResultV1 }
            .forEach { json: String ->
                val response = sendAndReceive(path = npexPath, payload = json)
                expectThat(response).status.not().isSameAs(INTERNAL_SERVER_ERROR)
            }
    }

    @Test
    fun `accepts english token gen request`() {
        every { service.acceptTestResultGeneratingTokens(any()) } returns VirologyTokenGenResponse(
            CtaToken.of("cc8f0b6z")
        )

        val response = sendAndReceive(path = tokenGenEngPathV1, payload = tokenGenPayloadV1)

        verify(exactly = 1) {
            service.acceptTestResultGeneratingTokens(
                VirologyTokenGenRequestV2(
                    TestEndDate.of(2020, 9, 7),
                    Positive,
                    LAB_RESULT
                )
            )
        }

        expectThat(response).status.isSameAs(OK)
        expectThat(response).body.isEqualToJson("""{"ctaToken":"cc8f0b6z"}""")
        expectThat(events).contains(VirologyTokenGen::class, CtaTokenGen::class)
    }

    @Test
    fun `accepts welsh token gen request`() {
        every { service.acceptTestResultGeneratingTokens(any()) } returns VirologyTokenGenResponse(CtaToken.of("cc8f0b6z"))

        val response = sendAndReceive(path = tokenGenWlsPathV1, payload = tokenGenPayloadV1)

        verify(exactly = 1) {
            service.acceptTestResultGeneratingTokens(
                VirologyTokenGenRequestV2(
                    TestEndDate.of(2020, 9, 7),
                    Positive,
                    LAB_RESULT
                )
            )
        }

        expectThat(response).status.isSameAs(OK)
        expectThat(response).body.isEqualToJson("""{"ctaToken":"cc8f0b6z"}""")
        expectThat(events).contains(VirologyTokenGen::class, CtaTokenGen::class)
    }

    @Test
    fun `accepts test result v1 returns 422 on invalid testKit`() {
        val response = sendAndReceive(path = npexPath, payload = rapidLabResultV2)

        expectThat(response).status.isSameAs(UNPROCESSABLE_ENTITY)
        expectThat(events).contains(VirologyResults::class)
    }

    @Test
    fun `accepts npex rapid lab test result v2 returns 202`() {
        every { service.acceptTestResult(any()) } returns Success()

        val response = sendAndReceive(path = npexV2Path, payload = rapidLabResultV2)

        expectThat(response).status.isSameAs(ACCEPTED)
        expectThat(response).body.isEqualTo("successfully processed")
        expectThat(events).contains(VirologyResults::class, TestResultUploaded::class)
    }

    @Test
    fun `v2 returns 422 when receiving v1 payload`() {
        val response = sendAndReceive(path = npexV2Path, payload = testLabResultV1)

        expectThat(response).status.isSameAs(UNPROCESSABLE_ENTITY)
        expectThat(events).contains(VirologyResults::class)
    }

    @Test
    fun `v2 returns 422 when receiving NEGATIVE for RAPID_RESULT testKit`() {
        val payload = """{
            "ctaToken": "cc8f0b6z",
            "testEndDate": "2020-04-23T00:00:00Z",
            "testResult": "NEGATIVE",
            "testKit": "RAPID_RESULT"
        }"""

        val response = sendAndReceive(path = npexV2Path, payload = payload)

        expectThat(response).status.isSameAs(UNPROCESSABLE_ENTITY)
        expectThat(events).contains(VirologyResults::class)
    }

    @Test
    fun `v2 returns 422 when receiving VOID for RAPID_RESULT testKit`() {
        val payload = """{
            "ctaToken": "cc8f0b6z",
            "testEndDate": "2020-04-23T00:00:00Z",
            "testResult": "VOID",
            "testKit": "RAPID_RESULT"
        }"""

        val response = sendAndReceive(path = npexV2Path, payload = payload)

        expectThat(response).status.isSameAs(UNPROCESSABLE_ENTITY)
        expectThat(events).contains(VirologyResults::class)
    }

    @Test
    fun `v2 returns 422 when receiving NEGATIVE for RAPID_SELF_REPORTED testKit`() {
        val payload = """{
            "ctaToken": "cc8f0b6z",
            "testEndDate": "2020-04-23T00:00:00Z",
            "testResult": "NEGATIVE",
            "testKit": "RAPID_SELF_REPORTED"
        }"""

        val response = sendAndReceive(path = npexV2Path, payload = payload)

        expectThat(response).status.isSameAs(UNPROCESSABLE_ENTITY)
        expectThat(events).contains(VirologyResults::class)
    }

    @Test
    fun `v2 returns 422 when receiving VOID for RAPID_SELF_REPORTED testKit`() {
        val payload = """{
            "ctaToken": "cc8f0b6z",
            "testEndDate": "2020-04-23T00:00:00Z",
            "testResult": "VOID",
            "testKit": "RAPID_SELF_REPORTED"
        }"""

        val response = sendAndReceive(path = npexV2Path, payload = payload)

        expectThat(response).status.isSameAs(UNPROCESSABLE_ENTITY)
        expectThat(events).contains(VirologyResults::class)
    }

    @Test
    fun `v2 returns 422 when receiving PLOD for RAPID_RESULT testKit`() {
        val payload = """{
            "ctaToken": "cc8f0b6z",
            "testEndDate": "2020-04-23T00:00:00Z",
            "testResult": "PLOD",
            "testKit": "RAPID_RESULT"
        }"""

        val response = sendAndReceive(path = npexV2Path, payload = payload)

        expectThat(response).status.isSameAs(UNPROCESSABLE_ENTITY)
        expectThat(events).contains(VirologyResults::class)
    }

    @Test
    fun `v2 returns 422 when receiving PLOD for RAPID_SELF_REPORTED testKit`() {
        val payload = """{
            "ctaToken": "cc8f0b6z",
            "testEndDate": "2020-04-23T00:00:00Z",
            "testResult": "PLOD",
            "testKit": "RAPID_SELF_REPORTED"
        }"""

        val response = sendAndReceive(path = npexV2Path, payload = payload)

        expectThat(response).status.isSameAs(UNPROCESSABLE_ENTITY)
        expectThat(events).contains(VirologyResults::class)
    }

    @Test
    fun `accepts fiorano test result v1 returns 202`() {
        every { service.acceptTestResult(any()) } returns Success()

        val response = sendAndReceive(path = fioranoPathV1, payload = testLabResultV1)

        expectThat(response).status.isSameAs(ACCEPTED)
        expectThat(response).body.isEqualTo("successfully processed")
        expectThat(events).contains(VirologyResults::class, TestResultUploaded::class)
    }

    @Test
    fun `accepts fiorano test result v2 returns 202`() {
        every { service.acceptTestResult(any()) } returns Success()

        val response = sendAndReceive(path = fioranoPathV2, payload = rapidLabResultV2)

        expectThat(response).status.isSameAs(ACCEPTED)
        expectThat(response).body.isEqualTo("successfully processed")
        expectThat(events).contains(VirologyResults::class, TestResultUploaded::class)
    }

    @Test
    fun `accepts v2 english rapid token gen request`() {
        every { service.acceptTestResultGeneratingTokens(any()) } returns VirologyTokenGenResponse(
            CtaToken.of("cc8f0b6z")
        )

        val response = sendAndReceive(path = tokenGenEngPathV2, payload = tokenGenPayloadV2)

        verify(exactly = 1) {
            service.acceptTestResultGeneratingTokens(
                VirologyTokenGenRequestV2(
                    TestEndDate.of(2020, 9, 7),
                    Positive,
                    RAPID_RESULT
                )
            )
        }

        expectThat(response).status.isSameAs(OK)
        expectThat(response).body.isEqualToJson("""{"ctaToken":"cc8f0b6z"}""")
        expectThat(events).contains(VirologyTokenGen::class, CtaTokenGen::class)
    }

    @Test
    fun `accepts v2 welsh token gen request`() {
        every { service.acceptTestResultGeneratingTokens(any()) } returns VirologyTokenGenResponse(
            CtaToken.of("cc8f0b6z")
        )

        val response = sendAndReceive(path = tokenGenWlsPathV2, payload = tokenGenPayloadV2)

        verify(exactly = 1) {
            service.acceptTestResultGeneratingTokens(
                VirologyTokenGenRequestV2(
                    TestEndDate.of(2020, 9, 7),
                    Positive,
                    RAPID_RESULT
                )
            )
        }

        expectThat(response).status.isSameAs(OK)
        expectThat(response).body.isEqualToJson("""{"ctaToken":"cc8f0b6z"}""")
        expectThat(events).contains(VirologyTokenGen::class, CtaTokenGen::class)
    }

    @Test
    fun `accepts v2 english token status request`() {
        every { service.checkStatusOfToken(any(), any()) } returns VirologyTokenStatusResponse(
            "consumable"
        )

        val response = sendAndReceive(path = tokenStatusEngPathV2, payload = tokenStatusPayloadV2)

        verify(exactly = 1) {
            service.checkStatusOfToken(
                VirologyTokenStatusRequest(
                    "cc8f0b6z"
                ),
                VirologyUploadHandler.VirologyTokenExchangeSource.Eng
            )
        }

        expectThat(response).status.isSameAs(OK)
        expectThat(response).body.isEqualToJson("""{"tokenStatus":"consumable"}""")
        expectThat(events).contains(TokenStatusCheck::class, VirologyTokenStatus::class)
    }

    @Test
    fun `accepts v2 wales token status request`() {
        every { service.checkStatusOfToken(any(), any()) } returns VirologyTokenStatusResponse(
            "consumable"
        )

        val response = sendAndReceive(path = tokenStatusWlsPathV2, payload = tokenStatusPayloadV2)

        verify(exactly = 1) {
            service.checkStatusOfToken(
                VirologyTokenStatusRequest(
                    "cc8f0b6z"
                ),
                VirologyUploadHandler.VirologyTokenExchangeSource.Wls
            )
        }

        expectThat(response).status.isSameAs(OK)
        expectThat(response).body.isEqualToJson("""{"tokenStatus":"consumable"}""")
        expectThat(events).contains(TokenStatusCheck::class, VirologyTokenStatus::class)
    }

    @Test
    fun `accepts rapid self reported test result v2 returns 202`() {
        every { service.acceptTestResult(any()) } returns Success()

        val response = sendAndReceive(path = npexV2Path, payload = rapidSelfReportedResultV2)

        expectThat(response).status.isSameAs(ACCEPTED)
        expectThat(response).body.isEqualTo("successfully processed")
    }

    @Test
    fun `accepts english rapid self reported token gen request v2`() {
        every { service.acceptTestResultGeneratingTokens(any()) } returns VirologyTokenGenResponse(
            CtaToken.of("cc8f0b6z")
        )

        val response = sendAndReceive(path = tokenGenEngPathV2, payload = tokenGenSelfReportedPayloadV2)

        verify(exactly = 1) {
            service.acceptTestResultGeneratingTokens(
                VirologyTokenGenRequestV2(
                    TestEndDate.of(2020, 9, 7),
                    Positive,
                    RAPID_SELF_REPORTED
                )
            )
        }

        expectThat(response).status.isSameAs(OK)
        expectThat(response).body.isEqualToJson("""{"ctaToken":"cc8f0b6z"}""")
    }

    private fun sendAndReceive(
        path: String,
        payload: String,
        method: HttpMethod = POST
    ): APIGatewayProxyResponseEvent {
        val requestEvent = request()
            .withMethod(method)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath(path)
            .withJson(payload)
            .withBearerToken("anything")

        val environment = TEST.apply(
            mapOf(
                "MAINTENANCE_MODE" to "false",
                "custom_oai" to "OAI"
            )
        )

        return VirologyUploadHandler(
            environment = environment,
            events = events,
            authenticator = { true },
            virologyService = service,
            healthAuthenticator = { true }
        ).handleRequest(requestEvent, aContext())
    }
}
