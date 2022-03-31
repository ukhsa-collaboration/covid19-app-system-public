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
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import uk.nhs.nhsx.circuitbreakers.ApprovalStatus.NO
import uk.nhs.nhsx.circuitbreakers.ApprovalStatus.PENDING
import uk.nhs.nhsx.circuitbreakers.CircuitBreakerResult.ResultType.MissingPollingTokenError
import uk.nhs.nhsx.circuitbreakers.CircuitBreakerResult.ResultType.Ok
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
    fun `get resolution with valid token for venues`() {
        val path = "/circuit-breaker/venue/resolution/QkFDQzlBREUtN0ZBMC00RTFELUE3NUMtRTZBMUFGNkMyRjNECg"
        val responseEvent = circuitBreakerService.getResolution(path)
        val resolutionResponse = resolutionFromResponse(responseEvent)

        expect {
            that(responseEvent).get(CircuitBreakerResult::type).isEqualTo(Ok)
            that(resolutionResponse).get(ResolutionResponse::approval).isEqualTo(NO.statusName)
        }
    }

    @Test
    fun `get resolution with valid token for exposure`() {
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
    fun `get resolution with empty token for venues`() {
        val path = "/circuit-breaker/venue/resolution/"
        val responseEvent = circuitBreakerService.getResolution(path)

        expectThat(responseEvent).get(CircuitBreakerResult::type).isEqualTo(MissingPollingTokenError)
    }

    @Test
    fun `get resolution with empty token for exposure`() {
        val path = "/circuit-breaker/exposure-notification/resolution/"
        val responseEvent = circuitBreakerService.getResolution(path)

        expectThat(responseEvent).get(CircuitBreakerResult::type).isEqualTo(MissingPollingTokenError)
    }

    @Test
    fun `get resolution with null token for venues`() {
        val path = "/circuit-breaker/venue/resolution"
        val responseEvent = circuitBreakerService.getResolution(path)

        expectThat(responseEvent).get(CircuitBreakerResult::type).isEqualTo(MissingPollingTokenError)
    }

    @Test
    fun `get resolution with null token for exposure`() {
        val path = "/circuit-breaker/exposure-notification/resolution"
        val responseEvent = circuitBreakerService.getResolution(path)

        expectThat(responseEvent).get(CircuitBreakerResult::type).isEqualTo(MissingPollingTokenError)
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(
        strings = [
            "token789",
            "",
            "/",
            "/circuit-breaker/exposure-notification/resolution/",
            "/circuit-breaker/exposure-notification/resolution/ddd/ff"
        ]
    )
    fun `extracts absent polling token`(input: String?) {
        expectThat(ApprovalTokenExtractor(input)).isNull()
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "/circuit-breaker/exposure-notification/resolution/QkFDQzlBREUtN0ZBMC00RTFELUE3NUMtRTZBMUFGNkMyRjNECg",
            "/circuit-breaker/exposure-notification/resolution/sGJbXfXiT9vTsuynqtFYM2KDWu9wpTIrZxd9Hxtv7E2vadgPsH",
        ]
    )
    fun `extracts polling token`(input: String) {
        expectThat(ApprovalTokenExtractor(input))
            .isNotNull()
            .isEqualTo(input.substringAfterLast("/"))
    }

    private fun resolutionFromResponse(responseEvent: CircuitBreakerResult) =
        Json.readJsonOrThrow<ResolutionResponse>(responseEvent.responseBody)
}
