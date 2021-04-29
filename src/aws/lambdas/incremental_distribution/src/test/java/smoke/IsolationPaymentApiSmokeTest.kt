package smoke

import org.assertj.core.api.Assertions.assertThat
import org.http4k.client.JavaHttpClient
import org.http4k.core.Status
import org.http4k.filter.debug
import org.junit.jupiter.api.Test
import smoke.actors.MobileApp
import smoke.actors.WelshSIPGateway
import smoke.env.SmokeTests
import uk.nhs.nhsx.isolationpayment.model.TokenStateExternal
import uk.nhs.nhsx.domain.Country.Companion.England
import uk.nhs.nhsx.domain.IpcTokenId
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit.SECONDS

class IsolationPaymentApiSmokeTest {
    private val client = JavaHttpClient()
    private val config = SmokeTests.loadConfig()
    private val mobileApp = MobileApp(client.debug(), config)
    private val welshSIPGateway = WelshSIPGateway(client.debug(), config)
    private val riskyEncounterDate = Instant.now().minus(Duration.ofDays(4)).truncatedTo(SECONDS)
    private val isolationPeriodEndDate = Instant.now().plus(Duration.ofDays(4)).truncatedTo(SECONDS)

    @Test
    fun `Consume token successfully via endpoint`() {
        val ipcToken = createValidToken()
        mobileApp.updateIsolationToken(ipcToken, riskyEncounterDate, isolationPeriodEndDate)
        val isolationConsumeResponse = welshSIPGateway.consumeToken(ipcToken, Status.OK)
        assertThat(isolationConsumeResponse.ipcToken).isEqualTo(ipcToken)
        assertThat(isolationConsumeResponse.state).isEqualTo(TokenStateExternal.EXT_CONSUMED.value)

    }

    @Test
    fun `Verify token successfully via endpoint`() {
        val ipcToken = createValidToken()
        mobileApp.updateIsolationToken(ipcToken, riskyEncounterDate, isolationPeriodEndDate)
        val isolationVerifyResponse = welshSIPGateway.verifyToken(ipcToken, Status.OK)
        assertThat(isolationVerifyResponse.ipcToken).isEqualTo(ipcToken)
        assertThat(isolationVerifyResponse.state).isEqualTo(TokenStateExternal.EXT_VALID.value)

    }

    @Test
    fun `Consume token returns 422 when unknown via endpoint`() {
        val ipcToken = IpcTokenId.of("Y".repeat(64))
        val isolationConsumeResponse = welshSIPGateway.consumeToken(ipcToken, Status.UNPROCESSABLE_ENTITY)
        assertThat(isolationConsumeResponse.ipcToken).isEqualTo(ipcToken)
        assertThat(isolationConsumeResponse.state).isEqualTo(TokenStateExternal.EXT_INVALID.value)
    }

    @Test
    fun `Verify token returns 422 when unknown via endpoint`() {
        val ipcToken = IpcTokenId.of("Z".repeat(64))
        val isolationVerifyResponse = welshSIPGateway.verifyToken(ipcToken, Status.UNPROCESSABLE_ENTITY)
        assertThat(isolationVerifyResponse.ipcToken).isEqualTo(ipcToken)
        assertThat(isolationVerifyResponse.state).isEqualTo(TokenStateExternal.EXT_INVALID.value)
    }

    private fun createValidToken(): IpcTokenId {
        val response = mobileApp.createIsolationToken(England)
        assertThat(response.ipcToken).isNotNull
        assertThat(response.isEnabled).isTrue
        return response.ipcToken
    }
}
