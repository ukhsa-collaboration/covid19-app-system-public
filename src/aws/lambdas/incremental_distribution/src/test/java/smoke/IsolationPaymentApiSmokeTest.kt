package smoke

import org.assertj.core.api.Assertions.assertThat
import org.http4k.client.JavaHttpClient
import org.http4k.core.Status
import org.http4k.filter.debug
import org.junit.jupiter.api.Test
import smoke.actors.IpcToken
import smoke.actors.MobileApp
import smoke.actors.UserCountry
import smoke.actors.WelshSIPGateway
import smoke.env.SmokeTests
import uk.nhs.nhsx.isolationpayment.model.TokenStateExternal
import java.time.OffsetDateTime

class IsolationPaymentApiSmokeTest {
    private val client = JavaHttpClient()
    private val config = SmokeTests.loadConfig()
    private val mobileApp = MobileApp(client.debug(), config)
    private val welshSIPGateway = WelshSIPGateway(client.debug(), config)
    private val riskyEncounterDate = OffsetDateTime.now().minusDays(4).withNano(0)

    private val isolationPeriodEndDate = OffsetDateTime.now().plusDays(4).withNano(0)

    @Test
    fun `Consume token successfully via endpoint`() {
        val ipcToken = createValidToken();
        mobileApp.updateIsolationToken(ipcToken, riskyEncounterDate, isolationPeriodEndDate)
        val isolationConsumeResponse = welshSIPGateway.consumeToken(ipcToken, Status.OK);
        assertThat(isolationConsumeResponse.ipcToken).isEqualTo(ipcToken.value)
        assertThat(isolationConsumeResponse.state).isEqualTo(TokenStateExternal.EXT_CONSUMED.value)

    }

    @Test
    fun `Verify token successfully via endpoint`() {
        val ipcToken = createValidToken();
        mobileApp.updateIsolationToken(ipcToken, riskyEncounterDate, isolationPeriodEndDate)
        val isolationVerifyResponse = welshSIPGateway.verifyToken(ipcToken, Status.OK);
        assertThat(isolationVerifyResponse.ipcToken).isEqualTo(ipcToken.value)
        assertThat(isolationVerifyResponse.state).isEqualTo(TokenStateExternal.EXT_VALID.value)

    }

    @Test
    fun `Consume token returns 422 when invalid via endpoint`() {
        val ipcToken = IpcToken("invalid-token")
        val isolationConsumeResponse = welshSIPGateway.consumeToken(ipcToken,Status.UNPROCESSABLE_ENTITY);
        assertThat(isolationConsumeResponse.ipcToken).isEqualTo(ipcToken.value)
        assertThat(isolationConsumeResponse.state).isEqualTo(TokenStateExternal.EXT_INVALID.value)

    }
    @Test
    fun `Verify token returns 422 when invalid via endpoint`() {
        val ipcToken = IpcToken("invalid-token")
        val isolationVerifyResponse = welshSIPGateway.verifyToken(ipcToken, Status.UNPROCESSABLE_ENTITY);
        assertThat(isolationVerifyResponse.ipcToken).isEqualTo(ipcToken.value)
        assertThat(isolationVerifyResponse.state).isEqualTo(TokenStateExternal.EXT_INVALID.value)

    }
    private fun createValidToken(): IpcToken {
        val isolationCreationResponse = mobileApp.createIsolationToken(UserCountry.England)
        assertThat(isolationCreationResponse.ipcToken).isNotEmpty
        assertThat(isolationCreationResponse.isEnabled).isTrue
        return IpcToken(isolationCreationResponse.ipcToken)
    }

}
