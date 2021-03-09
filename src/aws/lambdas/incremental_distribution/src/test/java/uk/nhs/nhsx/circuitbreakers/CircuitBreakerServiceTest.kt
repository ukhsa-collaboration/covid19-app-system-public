package uk.nhs.nhsx.circuitbreakers

import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.circuitbreakers.CircuitBreakerResult.ResultType
import uk.nhs.nhsx.circuitbreakers.CircuitBreakerService.extractPollingToken
import uk.nhs.nhsx.core.Jackson

class CircuitBreakerServiceTest {

    private val initial = { ApprovalStatus.PENDING }
    private val poll = { ApprovalStatus.NO }
    private val circuitBreakerService = CircuitBreakerService(initial, poll)

    @Test
    fun `test get approval token`() {
        val result: CircuitBreakerResult = circuitBreakerService.approvalToken
        assertThat(result.type).isEqualTo(ResultType.Ok)
        val approvalValue = JSONObject(result.responseBody).getString("approval")
        assertThat(approvalValue).isEqualTo(ApprovalStatus.PENDING.statusName)
        assertThat(result.responseBody).isNotEmpty
    }

    @Test
    fun `test get resolution with valid token for venues`() {
        val path = "/circuit-breaker/venue/resolution/QkFDQzlBREUtN0ZBMC00RTFELUE3NUMtRTZBMUFGNkMyRjNECg"
        val responseEvent = circuitBreakerService.getResolution(path)
        val resolutionResponse = resolutionFromResponse(responseEvent)

        assertThat(responseEvent.type).isEqualTo(ResultType.Ok)
        assertThat(resolutionResponse.approval).isEqualTo(ApprovalStatus.NO.statusName)
    }

    @Test
    fun testGetResolutionWithValidTokenForExposure() {
        val path =
            "/circuit-breaker/exposure-notification/resolution/QkFDQzlBREUtN0ZBMC00RTFELUE3NUMtRTZBMUFGNkMyRjNECg"
        val responseEvent = circuitBreakerService.getResolution(path)
        val resolutionResponse = resolutionFromResponse(responseEvent)

        assertThat(responseEvent.type).isEqualTo(ResultType.Ok)
        assertThat(resolutionResponse.approval).isEqualTo(ApprovalStatus.NO.statusName)
    }

    @Test
    fun `test get resolution with empty token for venues`() {
        val path = "/circuit-breaker/venue/resolution/"
        val responseEvent = circuitBreakerService.getResolution(path)

        assertThat(responseEvent.type).isEqualTo(ResultType.MissingPollingTokenError)
    }

    @Test
    fun `test get resolution with empty token for exposure`() {
        val path = "/circuit-breaker/exposure-notification/resolution/"
        val responseEvent = circuitBreakerService.getResolution(path)

        assertThat(responseEvent.type).isEqualTo(ResultType.MissingPollingTokenError)
    }

    @Test
    fun `test get resolution with null token for venues`() {
        val path = "/circuit-breaker/venue/resolution"
        val responseEvent = circuitBreakerService.getResolution(path)

        assertThat(responseEvent.type).isEqualTo(ResultType.MissingPollingTokenError)
    }

    @Test
    fun `test get resolution with null token for exposure`() {
        val path = "/circuit-breaker/exposure-notification/resolution"
        val responseEvent = circuitBreakerService.getResolution(path)

        assertThat(responseEvent.type).isEqualTo(ResultType.MissingPollingTokenError)
    }

    @Test
    fun `extract polling token`() {
        assertThat(extractPollingToken("/circuit-breaker/exposure-notification/resolution/token123")).contains("token123")
        assertThat(extractPollingToken("/circuit-breaker/venue/resolution/token789")).contains("token789")
        assertThat(extractPollingToken("token789")).isEmpty
        assertThat(extractPollingToken(null)).isEmpty
        assertThat(extractPollingToken("")).isEmpty
        assertThat(extractPollingToken("/")).isEmpty
        assertThat(extractPollingToken("/circuit-breaker/exposure-notification/resolution/")).isEmpty
    }

    private fun resolutionFromResponse(responseEvent: CircuitBreakerResult) =
        Jackson.readOrNull<ResolutionResponse>(responseEvent.responseBody)
            ?: throw IllegalStateException("Could not deserialize: " + responseEvent.responseBody)
}
