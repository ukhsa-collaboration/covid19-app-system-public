package smoke

import assertions.IsolationPaymentAssertions.hasValidIpcToken
import org.http4k.cloudnative.env.Environment
import org.http4k.core.Status
import org.http4k.filter.debug
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import smoke.actors.MobileApp
import smoke.actors.WelshSIPGateway
import smoke.actors.createHandler
import smoke.env.SmokeTests
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import uk.nhs.nhsx.domain.Country.Companion.England
import uk.nhs.nhsx.domain.IpcTokenId
import uk.nhs.nhsx.isolationpayment.model.IsolationResponse
import uk.nhs.nhsx.isolationpayment.model.TokenStateExternal.EXT_CONSUMED
import uk.nhs.nhsx.isolationpayment.model.TokenStateExternal.EXT_INVALID
import uk.nhs.nhsx.isolationpayment.model.TokenStateExternal.EXT_VALID
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit.SECONDS

class IsolationPaymentApiSmokeTest {
    private val client = createHandler(Environment.ENV)
    private val config = SmokeTests.loadConfig()
    private val mobileApp = MobileApp(client.debug(), config)
    private val welshSIPGateway = WelshSIPGateway(client.debug(), config)
    private val riskyEncounterDate = Instant.now().minus(Duration.ofDays(4)).truncatedTo(SECONDS)
    private val isolationPeriodEndDate = Instant.now().plus(Duration.ofDays(4)).truncatedTo(SECONDS)

    @Test @Disabled
    fun `Consume token successfully via endpoint`() {
        val ipcToken = createValidToken()
        mobileApp.updateIsolationToken(ipcToken, riskyEncounterDate, isolationPeriodEndDate)
        val isolationConsumeResponse = welshSIPGateway.consumeToken(ipcToken, Status.OK)

        expectThat(isolationConsumeResponse) {
            get(IsolationResponse::ipcToken).isEqualTo(ipcToken)
            get(IsolationResponse::state).isEqualTo(EXT_CONSUMED.value)
        }
    }

    @Test @Disabled
    fun `Verify token successfully via endpoint`() {
        val ipcToken = createValidToken()
        mobileApp.updateIsolationToken(ipcToken, riskyEncounterDate, isolationPeriodEndDate)
        val isolationVerifyResponse = welshSIPGateway.verifyToken(ipcToken, Status.OK)

        expectThat(isolationVerifyResponse) {
            get(IsolationResponse::ipcToken).isEqualTo(ipcToken)
            get(IsolationResponse::state).isEqualTo(EXT_VALID.value)
        }
    }

    @Test @Disabled
    fun `Consume token returns 422 when unknown via endpoint`() {
        val ipcToken = IpcTokenId.of("Y".repeat(64))
        val isolationConsumeResponse = welshSIPGateway.consumeToken(ipcToken, Status.UNPROCESSABLE_ENTITY)

        expectThat(isolationConsumeResponse) {
            get(IsolationResponse::ipcToken).isEqualTo(ipcToken)
            get(IsolationResponse::state).isEqualTo(EXT_INVALID.value)
        }
    }

    @Test @Disabled
    fun `Verify token returns 422 when unknown via endpoint`() {
        val ipcToken = IpcTokenId.of("Z".repeat(64))
        val isolationVerifyResponse: IsolationResponse = welshSIPGateway.verifyToken(ipcToken, Status.UNPROCESSABLE_ENTITY)

        expectThat(isolationVerifyResponse) {
            get(IsolationResponse::ipcToken).isEqualTo(ipcToken)
            get(IsolationResponse::state).isEqualTo(EXT_INVALID.value)
        }
    }

    private fun createValidToken() = mobileApp
        .createIsolationToken(England)
        .apply { expectThat(this).hasValidIpcToken() }
        .ipcToken
}
