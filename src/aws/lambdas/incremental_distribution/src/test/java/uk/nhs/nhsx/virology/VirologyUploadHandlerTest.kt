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
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.TestEnvironments
import uk.nhs.nhsx.core.events.*
import uk.nhs.nhsx.core.exceptions.HttpStatusCode
import uk.nhs.nhsx.core.exceptions.HttpStatusCode.ACCEPTED_202
import uk.nhs.nhsx.core.exceptions.HttpStatusCode.UNPROCESSABLE_ENTITY_422
import uk.nhs.nhsx.domain.CtaToken
import uk.nhs.nhsx.domain.TestEndDate
import uk.nhs.nhsx.domain.TestKit.*
import uk.nhs.nhsx.domain.TestResult.Positive
import uk.nhs.nhsx.testhelper.ContextBuilder.Companion.aContext
import uk.nhs.nhsx.testhelper.ProxyRequestBuilder
import uk.nhs.nhsx.testhelper.data.TestData.rapidLabResultV2
import uk.nhs.nhsx.testhelper.data.TestData.rapidSelfReportedResultV2
import uk.nhs.nhsx.testhelper.data.TestData.testLabResultV1
import uk.nhs.nhsx.testhelper.data.TestData.tokenGenPayloadV1
import uk.nhs.nhsx.testhelper.data.TestData.tokenGenPayloadV2
import uk.nhs.nhsx.testhelper.data.TestData.tokenGenSelfReportedPayloadV2
import uk.nhs.nhsx.testhelper.data.TestData.tokenStatusPayloadV2
import uk.nhs.nhsx.testhelper.matchers.ProxyResponseAssertions.hasBody
import uk.nhs.nhsx.testhelper.matchers.ProxyResponseAssertions.hasStatus
import uk.nhs.nhsx.virology.persistence.VirologyResultPersistOperation
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
        every { service.acceptTestResult(any()) } returns VirologyResultPersistOperation.Success()

        val response = sendAndReceive(path = npexPath, payload = testLabResultV1)

        assertThat(response, hasStatus(ACCEPTED_202))
        assertThat(response, hasBody(equalTo("successfully processed")))
        events.contains(VirologyResults::class, TestResultUploaded::class)
    }

    @Test
    fun `test order is not present returns 400`() {
        every { service.acceptTestResult(any()) } returns VirologyResultPersistOperation.OrderNotFound()

        val response = sendAndReceive(path = npexPath, payload = testLabResultV1)

        assertThat(response, hasStatus(HttpStatusCode.BAD_REQUEST_400))
        assertThat(response, hasBody(nullValue(String::class.java)))
        events.contains(VirologyResults::class, TestResultUploaded::class)
    }

    @Test
    fun `transaction failure returns 409`() {
        every { service.acceptTestResult(any()) } returns VirologyResultPersistOperation.TransactionFailed()

        val response = sendAndReceive(path = npexPath, payload = testLabResultV1)

        assertThat(response, hasStatus(HttpStatusCode.CONFLICT_409))
        assertThat(response, hasBody(nullValue(String::class.java)))
        events.contains(VirologyResults::class, TestResultUploaded::class)
    }

    @Test
    fun `invalid path returns 404`() {
        val response = sendAndReceive(path = "/upload/incorrect/npex-result", payload = testLabResultV1)

        assertThat(response, hasStatus(HttpStatusCode.NOT_FOUND_404))
        assertThat(response, hasBody(equalTo(null)))
    }

    @Test
    fun `invalid method returns 405`() {
        val response = sendAndReceive(path = npexPath, payload = testLabResultV1, method = GET)

        assertThat(response, hasStatus(HttpStatusCode.METHOD_NOT_ALLOWED_405))
        assertThat(response, hasBody(equalTo(null)))
    }

    @Test
    fun `empty body returns 422`() {
        val response = sendAndReceive(path = npexPath, payload = "")

        assertThat(response, hasStatus(UNPROCESSABLE_ENTITY_422))
        assertThat(response, hasBody(equalTo(null)))
        events.contains(VirologyResults::class, UnprocessableJson::class)
    }

    @Test
    fun `bad date returns 422`() {
        val response = sendAndReceive(
            path = npexPath,
            payload = """{"ctaToken": "cc8f0b6z","testEndDate": "2020-04-23T00:FOO:00Z","testResult": "NEGATIVE"}"""
        )

        assertThat(response, hasStatus(UNPROCESSABLE_ENTITY_422))
        assertThat(response, hasBody(equalTo(null)))
        events.contains(VirologyResults::class, UnprocessableJson::class)
    }

    @Test
    fun `random payload does not cause 500`() {
        every { service.acceptTestResult(any()) } returns VirologyResultPersistOperation.Success()

        Random.mutants(defaultJsonMutagens().forStrings(), 100, testLabResultV1)
            .filter { it != testLabResultV1 }
            .forEach { json: String ->
                val response = sendAndReceive(path = npexPath, payload = json)
                assertThat(response, not(hasStatus(HttpStatusCode.INTERNAL_SERVER_ERROR_500)))
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

        assertThat(response, hasStatus(HttpStatusCode.OK_200))
        assertThat(response, hasBody(equalTo("""{"ctaToken":"cc8f0b6z"}""")))
        events.contains(VirologyTokenGen::class, CtaTokenGen::class)
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

        assertThat(response, hasStatus(HttpStatusCode.OK_200))
        assertThat(response, hasBody(equalTo("""{"ctaToken":"cc8f0b6z"}""")))
        events.contains(VirologyTokenGen::class, CtaTokenGen::class)
    }

    @Test
    fun `accepts test result v1 returns 422 on invalid testKit`() {
        val response = sendAndReceive(path = npexPath, payload = rapidLabResultV2)

        assertThat(response, hasStatus(UNPROCESSABLE_ENTITY_422))
        events.contains(VirologyResults::class)
    }

    @Test
    fun `accepts npex rapid lab test result v2 returns 202`() {
        every { service.acceptTestResult(any()) } returns VirologyResultPersistOperation.Success()

        val response = sendAndReceive(path = npexV2Path, payload = rapidLabResultV2)

        assertThat(response, hasStatus(ACCEPTED_202))
        assertThat(response, hasBody(equalTo("successfully processed")))
        events.contains(VirologyResults::class, TestResultUploaded::class)
    }

    @Test
    fun `v2 returns 422 when receiving v1 payload`() {
        val response = sendAndReceive(path = npexV2Path, payload = testLabResultV1)

        assertThat(response, hasStatus(UNPROCESSABLE_ENTITY_422))
        events.contains(VirologyResults::class)
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

        assertThat(response, hasStatus(UNPROCESSABLE_ENTITY_422))
        events.contains(VirologyResults::class)
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

        assertThat(response, hasStatus(UNPROCESSABLE_ENTITY_422))
        events.contains(VirologyResults::class)
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

        assertThat(response, hasStatus(UNPROCESSABLE_ENTITY_422))
        events.contains(VirologyResults::class)
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

        assertThat(response, hasStatus(UNPROCESSABLE_ENTITY_422))
        events.contains(VirologyResults::class)
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

        assertThat(response, hasStatus(UNPROCESSABLE_ENTITY_422))
        events.contains(VirologyResults::class)
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

        assertThat(response, hasStatus(UNPROCESSABLE_ENTITY_422))
        events.contains(VirologyResults::class)
    }

    @Test
    fun `accepts fiorano test result v1 returns 202`() {
        every { service.acceptTestResult(any()) } returns VirologyResultPersistOperation.Success()

        val response = sendAndReceive(path = fioranoPathV1, payload = testLabResultV1)

        assertThat(response, hasStatus(ACCEPTED_202))
        assertThat(response, hasBody(equalTo("successfully processed")))
        events.contains(VirologyResults::class, TestResultUploaded::class)
    }

    @Test
    fun `accepts fiorano test result v2 returns 202`() {
        every { service.acceptTestResult(any()) } returns VirologyResultPersistOperation.Success()

        val response = sendAndReceive(path = fioranoPathV2, payload = rapidLabResultV2)

        assertThat(response, hasStatus(ACCEPTED_202))
        assertThat(response, hasBody(equalTo("successfully processed")))
        events.contains(VirologyResults::class, TestResultUploaded::class)
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

        assertThat(response, hasStatus(HttpStatusCode.OK_200))
        assertThat(response, hasBody(equalTo("""{"ctaToken":"cc8f0b6z"}""")))
        events.contains(VirologyTokenGen::class, CtaTokenGen::class)
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

        assertThat(response, hasStatus(HttpStatusCode.OK_200))
        assertThat(response, hasBody(equalTo("""{"ctaToken":"cc8f0b6z"}""")))
        events.contains(VirologyTokenGen::class, CtaTokenGen::class)
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

        assertThat(response, hasStatus(HttpStatusCode.OK_200))
        assertThat(response, hasBody(equalTo("""{"tokenStatus":"consumable"}""")))
        events.contains(TokenStatusCheck::class, VirologyTokenStatus::class)
    }

    @Test
    fun `accepts v2 wales token status request`() {
        every { service.checkStatusOfToken(any(),any()) } returns VirologyTokenStatusResponse(
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

        assertThat(response, hasStatus(HttpStatusCode.OK_200))
        assertThat(response, hasBody(equalTo("""{"tokenStatus":"consumable"}""")))
        events.contains(TokenStatusCheck::class, VirologyTokenStatus::class)
    }

    @Test
    fun `accepts rapid self reported test result v2 returns 202`() {
        every { service.acceptTestResult(any()) } returns VirologyResultPersistOperation.Success()

        val response = sendAndReceive(path = npexV2Path, payload = rapidSelfReportedResultV2)

        assertThat(response, hasStatus(ACCEPTED_202))
        assertThat(response, hasBody(equalTo("successfully processed")))
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

        assertThat(response, hasStatus(HttpStatusCode.OK_200))
        assertThat(response, hasBody(equalTo("""{"ctaToken":"cc8f0b6z"}""")))
    }

    private fun sendAndReceive(
        path: String,
        payload: String,
        method: HttpMethod = POST
    ): APIGatewayProxyResponseEvent {
        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(method)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath(path)
            .withJson(payload)
            .withBearerToken("anything")
            .build()

        val environment = TestEnvironments.TEST.apply(
            mapOf(
                "MAINTENANCE_MODE" to "false",
                "custom_oai" to "OAI"
            )
        )

        val handler = VirologyUploadHandler(environment, events, { true }, service, { true })
        return handler.handleRequest(requestEvent, aContext())
    }
}
