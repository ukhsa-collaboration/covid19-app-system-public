package uk.nhs.nhsx.circuitbreakers

import org.json.JSONObject
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import strikt.api.expect
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEmpty
import strikt.java.isAbsent
import strikt.java.isPresent
import uk.nhs.nhsx.circuitbreakers.ApprovalStatus.NO
import uk.nhs.nhsx.circuitbreakers.ApprovalStatus.PENDING
import uk.nhs.nhsx.circuitbreakers.CircuitBreakerResult.ResultType.MissingPollingTokenError
import uk.nhs.nhsx.circuitbreakers.CircuitBreakerResult.ResultType.Ok
import uk.nhs.nhsx.circuitbreakers.CircuitBreakerService.Companion.extractPollingToken
import uk.nhs.nhsx.core.Json

class CircuitBreakerServiceTest {

    private val initial = { PENDING }
    private val poll = { NO }
    private val circuitBreakerService = CircuitBreakerService(initial, poll)

    @Test
    fun `test get approval token`() {
        val result = circuitBreakerService.getApprovalToken()

        expect {
            that(result).get(CircuitBreakerResult::type)
                .isEqualTo(Ok)

            that(result).get(CircuitBreakerResult::responseBody)
                .isNotEmpty()
                .get(::JSONObject)
                .get { getString("approval") }
                .isEqualTo(PENDING.statusName)
        }
    }

    @Test
    fun `test get resolution with valid token for venues`() {
        val path = "/circuit-breaker/venue/resolution/QkFDQzlBREUtN0ZBMC00RTFELUE3NUMtRTZBMUFGNkMyRjNECg"
        val responseEvent = circuitBreakerService.getResolution(path)
        val resolutionResponse = resolutionFromResponse(responseEvent)

        expect {
            that(responseEvent).get(CircuitBreakerResult::type).isEqualTo(Ok)
            that(resolutionResponse).get(ResolutionResponse::approval).isEqualTo(NO.statusName)
        }
    }

    @Test
    fun testGetResolutionWithValidTokenForExposure() {
        val path =
            "/circuit-breaker/exposure-notification/resolution/QkFDQzlBREUtN0ZBMC00RTFELUE3NUMtRTZBMUFGNkMyRjNECg"
        val responseEvent = circuitBreakerService.getResolution(path)
        val resolutionResponse = resolutionFromResponse(responseEvent)

        expect {
            that(responseEvent).get(CircuitBreakerResult::type).isEqualTo(Ok)
            that(resolutionResponse).get(ResolutionResponse::approval).isEqualTo(NO.statusName)
        }
    }

    @Test
    fun `test get resolution with empty token for venues`() {
        val path = "/circuit-breaker/venue/resolution/"
        val responseEvent = circuitBreakerService.getResolution(path)

        expectThat(responseEvent).get(CircuitBreakerResult::type).isEqualTo(MissingPollingTokenError)
    }

    @Test
    fun `test get resolution with empty token for exposure`() {
        val path = "/circuit-breaker/exposure-notification/resolution/"
        val responseEvent = circuitBreakerService.getResolution(path)

        expectThat(responseEvent).get(CircuitBreakerResult::type).isEqualTo(MissingPollingTokenError)
    }

    @Test
    fun `test get resolution with null token for venues`() {
        val path = "/circuit-breaker/venue/resolution"
        val responseEvent = circuitBreakerService.getResolution(path)

        expectThat(responseEvent).get(CircuitBreakerResult::type).isEqualTo(MissingPollingTokenError)
    }

    @Test
    fun `test get resolution with null token for exposure`() {
        val path = "/circuit-breaker/exposure-notification/resolution"
        val responseEvent = circuitBreakerService.getResolution(path)

        expectThat(responseEvent).get(CircuitBreakerResult::type).isEqualTo(MissingPollingTokenError)
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = ["token789", "", "/", "/circuit-breaker/exposure-notification/resolution/"])
    fun `extracts absent polling token`(input: String?) {
        expectThat(extractPollingToken(input)).isAbsent()
    }

    @ParameterizedTest
    @ValueSource(strings = ["/circuit-breaker/exposure-notification/resolution/token123", "/circuit-breaker/venue/resolution/token789"])
    fun `extracts polling token`(input: String) {
        expectThat(extractPollingToken(input))
            .isPresent()
            .isEqualTo(input.substringAfterLast("/"))
    }

    private fun resolutionFromResponse(responseEvent: CircuitBreakerResult) =
        Json.readJsonOrThrow<ResolutionResponse>(responseEvent.responseBody)
}
