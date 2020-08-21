package uk.nhs.nhsx.circuitbreakers

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import uk.nhs.nhsx.circuitbreakers.CircuitBreakerService.extractPollingToken
import uk.nhs.nhsx.core.Jackson
import uk.nhs.nhsx.core.exceptions.ApiResponseException

class CircuitBreakerServiceTest {

    private val initial = { ApprovalStatus.PENDING }
    private val poll = { ApprovalStatus.NO }

    private val circuitBreakerService = CircuitBreakerService(initial, poll)

    @Test
    fun testGetApprovalTokenForValidPayloadForVenues() {
        val responseEvent = circuitBreakerService.approvalToken
        val tokenResponse = tokenFromResponse(responseEvent)

        assertThat(responseEvent.statusCode).isEqualTo(200)
        assertThat(tokenResponse.approval).isEqualTo(ApprovalStatus.PENDING.getName())
        assertThat(tokenResponse.approvalToken).isNotEmpty()
    }

    @Test
    fun testGetApprovalTokenForValidPayloadForExposure() {
        val responseEvent = circuitBreakerService.approvalToken
        val tokenResponse = tokenFromResponse(responseEvent)

        assertThat(responseEvent.statusCode).isEqualTo(200)
        assertThat(tokenResponse.approval).isEqualTo(ApprovalStatus.PENDING.getName())
        assertThat(tokenResponse.approvalToken).isNotEmpty()
    }

    @Test
    fun testGetResolutionWithValidTokenForVenues() {
        val path = "/circuit-breaker/venue/resolution/QkFDQzlBREUtN0ZBMC00RTFELUE3NUMtRTZBMUFGNkMyRjNECg"
        val responseEvent = circuitBreakerService.getResolution(path)
        val resolutionResponse = resolutionFromResponse(responseEvent)

        assertThat(responseEvent.statusCode).isEqualTo(200)
        assertThat(resolutionResponse.approval).isEqualTo(ApprovalStatus.NO.getName())
    }

    @Test
    fun testGetResolutionWithValidTokenForExposure() {
        val path = "/circuit-breaker/exposure-notification/resolution/QkFDQzlBREUtN0ZBMC00RTFELUE3NUMtRTZBMUFGNkMyRjNECg"
        val responseEvent = circuitBreakerService.getResolution(path)
        val resolutionResponse = resolutionFromResponse(responseEvent)

        assertThat(responseEvent.statusCode).isEqualTo(200)
        assertThat(resolutionResponse.approval).isEqualTo(ApprovalStatus.NO.getName())
    }

    @Test
    fun testGetResolutionWithEmptyTokenForVenues() {
        val path = "/circuit-breaker/venue/resolution/"

        assertThatThrownBy { circuitBreakerService.getResolution(path) }
            .isInstanceOf(ApiResponseException::class.java)
            .hasMessage("Circuit Breaker request submitted without approval token")
    }

    @Test
    fun testGetResolutionWithEmptyTokenForExposure() {
        val path = "/circuit-breaker/exposure-notification/resolution/"

        assertThatThrownBy { circuitBreakerService.getResolution(path) }
            .isInstanceOf(ApiResponseException::class.java)
            .hasMessage("Circuit Breaker request submitted without approval token")
    }

    @Test
    fun testGetResolutionWithNullTokenForVenues() {
        val path = "/circuit-breaker/venue/resolution"

        assertThatThrownBy { circuitBreakerService.getResolution(path) }
            .isInstanceOf(ApiResponseException::class.java)
            .hasMessage("Circuit Breaker request submitted without approval token")
    }

    @Test
    fun testGetResolutionWithNullTokenForExposure() {
        val path = "/circuit-breaker/exposure-notification/resolution"

        assertThatThrownBy { circuitBreakerService.getResolution(path) }
            .isInstanceOf(ApiResponseException::class.java)
            .hasMessage("Circuit Breaker request submitted without approval token")
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

    private fun tokenFromResponse(responseEvent: APIGatewayProxyResponseEvent): TokenResponse {
        return Jackson.deserializeMaybe(responseEvent.body, TokenResponse::class.java)
            .orElseThrow { IllegalStateException("Could not deserialize: " + responseEvent.body) }
    }

    private fun resolutionFromResponse(responseEvent: APIGatewayProxyResponseEvent): ResolutionResponse {
        return Jackson.deserializeMaybe(responseEvent.body, ResolutionResponse::class.java)
            .orElseThrow { IllegalStateException("Could not deserialize: " + responseEvent.body) }
    }
}