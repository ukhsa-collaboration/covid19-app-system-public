package smoke

import org.assertj.core.api.Assertions.assertThat
import org.http4k.client.JavaHttpClient
import org.http4k.filter.debug
import org.junit.jupiter.api.Test
import smoke.actors.MobileApp
import smoke.actors.SIPGateway
import smoke.env.SmokeTests
import uk.nhs.nhsx.domain.Country
import uk.nhs.nhsx.domain.Country.Companion.England
import uk.nhs.nhsx.domain.IpcTokenId
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit.SECONDS

class IsolationPaymentSmokeTest {
    private val client = JavaHttpClient()
    private val config = SmokeTests.loadConfig()
    private val mobileApp = MobileApp(client.debug(), config)
    private val sipGateway = SIPGateway(config)

    private val riskyEncounterDate = Instant.now().minus(Duration.ofDays(4)).truncatedTo(SECONDS)
    private val isolationPeriodEndDate = Instant.now().plus(Duration.ofDays(4)).truncatedTo(SECONDS)

    @Test
    fun `happy path - submit isolation payment, update, verify and consume ipcToken`() {
        val ipcToken = createValidToken()
        mobileApp.updateIsolationToken(ipcToken, riskyEncounterDate, isolationPeriodEndDate)
        verifyValidToken(ipcToken)
        consumeValidToken(ipcToken)
    }

    @Test
    fun `isolation payment order creation is not enabled for non whitelisted countries`() {
        val createIsolationToken = mobileApp.createNonWhiteListedIsolationToken(Country.of("Switzerland"))
        assertThat(createIsolationToken.isEnabled).isFalse
    }

    @Test
    fun `update isolation token returns website URL even when an non-existing token is passed`() {
        mobileApp.updateIsolationToken(IpcTokenId.of("1".repeat(64)), riskyEncounterDate, isolationPeriodEndDate)
    }

    @Test
    fun `cannot consume ipcToken more than one time`() {
        val ipcToken = createValidToken()
        mobileApp.updateIsolationToken(ipcToken, riskyEncounterDate, isolationPeriodEndDate)
        verifyValidToken(ipcToken)
        consumeValidToken(ipcToken)
        consumeInvalidToken(ipcToken)
    }

    @Test
    fun `verify fails if token is unknown or not updated yet`() {
        verifyInvalidToken(IpcTokenId.of("Z".repeat(64)))
        val ipcToken = createValidToken()
        verifyInvalidToken(ipcToken)
        mobileApp.updateIsolationToken(ipcToken, riskyEncounterDate, isolationPeriodEndDate)
        verifyValidToken(ipcToken)
        verifyValidToken(ipcToken)
    }

    @Test
    fun `update has no effect if token is already verified`() {
        val ipcToken = createValidToken()
        mobileApp.updateIsolationToken(ipcToken, riskyEncounterDate, isolationPeriodEndDate)
        verifyValidToken(ipcToken)

        val otherDate = Instant.now()
        mobileApp.updateIsolationToken(ipcToken, otherDate, otherDate)

        verifyValidToken(ipcToken)
    }

    private fun createValidToken(): IpcTokenId {
        val response = mobileApp.createIsolationToken(England)
        assertThat(response.ipcToken).isNotNull
        assertThat(response.isEnabled).isTrue
        return response.ipcToken
    }

    private fun verifyValidToken(ipcToken: IpcTokenId) {
        val verifyPayload = sipGateway.verifiesIpcToken(ipcToken)
        assertThat(verifyPayload["state"]).isEqualTo("valid")
        assertThat(Instant.parse(verifyPayload["riskyEncounterDate"])).isEqualTo(riskyEncounterDate)
        assertThat(Instant.parse(verifyPayload["isolationPeriodEndDate"])).isEqualTo(isolationPeriodEndDate)
        assertThat(verifyPayload).containsKey("createdTimestamp")
        assertThat(verifyPayload).containsKey("updatedTimestamp")
    }

    private fun verifyInvalidToken(ipcToken: IpcTokenId) {
        val verifyPayload = sipGateway.verifiesIpcToken(ipcToken)
        assertThat(verifyPayload["state"]).isEqualTo("invalid")
    }

    private fun consumeValidToken(ipcToken: IpcTokenId) {
        val consumePayload = sipGateway.consumesIpcToken(ipcToken)
        assertThat(consumePayload["state"]).isEqualTo("consumed")
    }

    private fun consumeInvalidToken(ipcToken: IpcTokenId) {
        val consumePayload = sipGateway.consumesIpcToken(ipcToken)
        assertThat(consumePayload["state"]).isEqualTo("invalid")
    }

}
