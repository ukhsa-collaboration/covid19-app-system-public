package uk.nhs.nhsx.circuitbreakers

import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.circuitbreakers.CircuitBreakerService.extractPollingToken
import uk.nhs.nhsx.core.Jackson

class CircuitBreakerServiceTest {

    private val initial = { ApprovalStatus.PENDING }
    private val poll = { ApprovalStatus.NO }
    private val circuitBreakerService = CircuitBreakerService(initial, poll)

    @Test
    fun testGetApprovalToken() {
        val result: CircuitBreakerResult = circuitBreakerService.approvalToken
        assertThat(result.type).isEqualTo(CircuitBreakerResult.ResultType.Ok)
        val approvalValue = JSONObject(result.responseBody).getString("approval")
        assertThat(approvalValue).isEqualTo(ApprovalStatus.PENDING.getName())
        assertThat(result.responseBody).isNotEmpty()
    }

    @Test
    fun testGetResolutionWithValidTokenForVenues() {
        val path = "/circuit-breaker/venue/resolution/QkFDQzlBREUtN0ZBMC00RTFELUE3NUMtRTZBMUFGNkMyRjNECg"
        val responseEvent = circuitBreakerService.getResolution(path)
        val resolutionResponse = resolutionFromResponse(responseEvent)

        assertThat(responseEvent.type).isEqualTo(CircuitBreakerResult.ResultType.Ok)
        assertThat(resolutionResponse.approval).isEqualTo(ApprovalStatus.NO.getName())
    }

    @Test
    fun testGetResolutionWithValidTokenForExposure() {
        val path = "/circuit-breaker/exposure-notification/resolution/QkFDQzlBREUtN0ZBMC00RTFELUE3NUMtRTZBMUFGNkMyRjNECg"
        val responseEvent = circuitBreakerService.getResolution(path)
        val resolutionResponse = resolutionFromResponse(responseEvent)

        assertThat(responseEvent.type).isEqualTo(CircuitBreakerResult.ResultType.Ok)
        assertThat(resolutionResponse.approval).isEqualTo(ApprovalStatus.NO.getName())
    }

    @Test
    fun testGetResolutionWithEmptyTokenForVenues() {
        val path = "/circuit-breaker/venue/resolution/"
        val responseEvent = circuitBreakerService.getResolution(path)

        assertThat(responseEvent.type).isEqualTo(CircuitBreakerResult.ResultType.MissingPollingTokenError)
    }

    @Test
    fun testGetResolutionWithEmptyTokenForExposure() {
        val path = "/circuit-breaker/exposure-notification/resolution/"
        val responseEvent = circuitBreakerService.getResolution(path)

        assertThat(responseEvent.type).isEqualTo(CircuitBreakerResult.ResultType.MissingPollingTokenError)
    }

    @Test
    fun testGetResolutionWithNullTokenForVenues() {
        val path = "/circuit-breaker/venue/resolution"
        val responseEvent = circuitBreakerService.getResolution(path)

        assertThat(responseEvent.type).isEqualTo(CircuitBreakerResult.ResultType.MissingPollingTokenError)
    }

    @Test
    fun testGetResolutionWithNullTokenForExposure() {
        val path = "/circuit-breaker/exposure-notification/resolution"
        val responseEvent = circuitBreakerService.getResolution(path)

        assertThat(responseEvent.type).isEqualTo(CircuitBreakerResult.ResultType.MissingPollingTokenError)
    }

    @Test
    fun extractPollingToken() {
        assertThat(extractPollingToken("/circuit-breaker/exposure-notification/resolution/token123"))
                .contains("token123")
        assertThat(extractPollingToken("/circuit-breaker/venue/resolution/token789"))
                .contains("token789")
        assertThat(extractPollingToken("token789")).isEmpty()
        assertThat(extractPollingToken(null)).isEmpty()
        assertThat(extractPollingToken("")).isEmpty()
        assertThat(extractPollingToken("/")).isEmpty()
        assertThat(extractPollingToken("/circuit-breaker/exposure-notification/resolution/")).isEmpty()
    }

    private fun resolutionFromResponse(responseEvent: CircuitBreakerResult): ResolutionResponse {
        return Jackson.deserializeMaybe(responseEvent.responseBody, ResolutionResponse::class.java)
                .orElseThrow { IllegalStateException("Could not deserialize: " + responseEvent.responseBody) }
    }
}