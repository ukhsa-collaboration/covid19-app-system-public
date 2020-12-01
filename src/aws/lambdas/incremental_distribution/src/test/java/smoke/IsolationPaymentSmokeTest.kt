package smoke

import com.fasterxml.jackson.core.type.TypeReference
import org.assertj.core.api.Assertions.assertThat
import org.http4k.asString
import org.http4k.client.JavaHttpClient
import org.http4k.core.then
import org.http4k.filter.DebuggingFilters
import org.http4k.unquoted
import org.junit.jupiter.api.Test
import smoke.clients.AwsLambda
import smoke.clients.IsolationPaymentClient
import smoke.env.SmokeTests
import uk.nhs.nhsx.core.DateFormatValidator
import uk.nhs.nhsx.core.Jackson
import uk.nhs.nhsx.isolationpayment.model.TokenGenerationRequest
import uk.nhs.nhsx.isolationpayment.model.TokenGenerationResponse
import uk.nhs.nhsx.isolationpayment.model.TokenUpdateRequest
import uk.nhs.nhsx.isolationpayment.model.TokenUpdateResponse
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.*

class IsolationPaymentSmokeTest {

    private val client = JavaHttpClient()
    private val config = SmokeTests.loadConfig()
    private val isolationPaymentClient = IsolationPaymentClient(DebuggingFilters.PrintRequestAndResponse().then(client), config)

    private val riskyEncounterDate = OffsetDateTime.now().minusDays(4);
    private val riskyEncounterDateString = riskyEncounterDate.format(DateFormatValidator.formatter);

    private val isolationPeriodEndDate = OffsetDateTime.now().plusDays(4);
    private val isolationPeriodEndDateString = isolationPeriodEndDate.format(DateFormatValidator.formatter);

    @Test
    fun `happy path - submit isolation payment, update, verify and consume ipcToken`() {
        val ipcToken = createValidToken()
        updateToken(ipcToken)
        verifyValidToken(ipcToken)
        consumeValidToken(ipcToken)
    }

    @Test
    fun `isolation payment order creation is not enabled for non whitelisted countries`() {
        val isolationCreationResponse = createInvalidToken()
        assertThat(isolationCreationResponse.ipcToken).isNull()
        assertThat(isolationCreationResponse.isEnabled).isFalse
    }

    @Test
    fun `update isolation token returns website URL even when an invalid token is passed`() {
        updateToken("invalid-token")
    }

    @Test
    fun `cannot consume ipcToken more than one time`() {
        val ipcToken = createValidToken();
        updateToken(ipcToken)
        verifyValidToken(ipcToken)
        consumeValidToken(ipcToken)
        consumeInvalidToken(ipcToken)
    }

    @Test
    fun `verify fails if token is invalid or not updated yet`() {
        verifyInvalidToken("invalid-token")
        val ipcToken = createValidToken();
        verifyInvalidToken(ipcToken)
        updateToken(ipcToken)
        verifyValidToken(ipcToken)
        verifyValidToken(ipcToken)
    }

    @Test
    fun `update has no effect if token is already verified`() {
        val ipcToken = createValidToken();
        updateToken(ipcToken)
        verifyValidToken(ipcToken)

        val otherDate = OffsetDateTime.now();
        val otherDateString = otherDate.format(DateFormatValidator.formatter);
        updateToken(ipcToken, otherDateString, otherDateString)

        verifyValidToken(ipcToken)
    }

    fun createValidToken(): String {
        val isolationCreationResponse = createToken("England")
        assertThat(isolationCreationResponse.ipcToken).isNotEmpty
        assertThat(isolationCreationResponse.isEnabled).isTrue
        return isolationCreationResponse.ipcToken
    }

    fun createInvalidToken() = createToken("Switzerland")

    fun createToken(country: String): TokenGenerationResponse {
        return isolationPaymentClient.submitIsolationTokenCreate(
            TokenGenerationRequest(country)
        )
    }

    fun updateToken(ipcToken: String) {
        updateToken(ipcToken, riskyEncounterDateString, isolationPeriodEndDateString);
    }

    fun updateToken(ipcToken: String, riskyEncounterDateString: String, isolationPeriodEndDateString: String) {
        val isolationTokenUpdateResponse = isolationPaymentClient.submitIsolationTokenUpdate(
            TokenUpdateRequest(ipcToken, riskyEncounterDateString, isolationPeriodEndDateString)
        )
        assertThat(isolationTokenUpdateResponse.websiteUrlWithQuery).endsWith(ipcToken)
    }

    fun verifyValidToken(ipcToken: String) {
        val verifyPayload = verifyToken(ipcToken)
        assertThat(verifyPayload["state"]).isEqualTo("valid")
        assertThat(verifyPayload["riskyEncounterDate"]).isEqualTo(riskyEncounterDateString)
        assertThat(verifyPayload["isolationPeriodEndDate"]).isEqualTo(isolationPeriodEndDateString)
        assertThat(verifyPayload).containsKey("createdTimestamp")
        assertThat(verifyPayload).containsKey("updatedTimestamp")
    }

    fun verifyInvalidToken(ipcToken: String) {
        val verifyPayload = verifyToken(ipcToken)
        assertThat(verifyPayload["state"]).isEqualTo("invalid")
    }

    fun verifyToken(ipcToken: String): Map<String, String> {
        val verifyPayload = lambdaCall(
            config.isolationPaymentVerifyLambdaFunctionName,
            """{ "contractVersion": 1, "ipcToken": "$ipcToken" }"""
        )
        assertThat(verifyPayload["ipcToken"]).isEqualTo(ipcToken)
        return verifyPayload
    }

    fun consumeValidToken(ipcToken: String) {
        val consumePayload = consumeToken(ipcToken)
        assertThat(consumePayload["state"]).isEqualTo("consumed")
    }

    fun consumeInvalidToken(ipcToken: String) {
        val consumePayload = consumeToken(ipcToken)
        assertThat(consumePayload["state"]).isEqualTo("invalid")
    }

    fun consumeToken(ipcToken: String): Map<String, String> {
        val consumePayload = lambdaCall(
            config.isolationPaymentConsumeLambdaFunctionName,
            """{ "contractVersion": 1, "ipcToken": "$ipcToken" }"""
        )
        assertThat(consumePayload["ipcToken"]).isEqualTo(ipcToken)
        return consumePayload
    }

    fun lambdaCall(functionName: String, payload: String): Map<String, String> {
        val verifyResult = AwsLambda.invokeFunction(
            functionName = functionName,
            payload = payload
        )
        return Jackson.readJson(verifyResult.payload.asString().unquoted(), object : TypeReference<Map<String, String>>() {})
    }

    /* @Test
    fun `isolation payment order creation is not enabled when token creation is disabled`() {
        val isTokenCreationEnabled = AwsLambda.getTokenCreationEnabled(config.isolationPaymentOrderLambdaFunctionName)
        if(isTokenCreationEnabled){
            AwsLambda.disableTokenCreationEnabled(config.isolationPaymentOrderLambdaFunctionName)
        }

        isolationPaymentClient.submitIsolationTokenCreateTokenCreationDisabled(
            TokenGenerationRequest("Germany")
        )
        // FIXME assertions

        if(isTokenCreationEnabled) {
            AwsLambda.enableTokenCreationEnabled(config.isolationPaymentOrderLambdaFunctionName)
        }
    } */

    /* @Test
    fun `isolation payment order update is not enabled when token creation is disabled`() {
        val isTokenCreationEnabled = AwsLambda.getTokenCreationEnabled(config.isolationPaymentOrderLambdaFunctionName)
        if(isTokenCreationEnabled){
            AwsLambda.disableTokenCreationEnabled(config.isolationPaymentOrderLambdaFunctionName)
        }

        isolationPaymentClient.submitIsolationTokenUpdateTokenCreationDisabled(
            TokenUpdateRequest("dummy", "2020-08-24T21:59:00Z", "2020-08-24T21:59:00Z")
        )
        // FIXME assertions

        if(isTokenCreationEnabled) {
            AwsLambda.enableTokenCreationEnabled(config.isolationPaymentOrderLambdaFunctionName)
        }
    } */
}