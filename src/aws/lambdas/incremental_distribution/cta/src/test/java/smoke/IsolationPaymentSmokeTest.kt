package smoke

import assertions.IsolationPaymentAssertions.hasValidIpcToken
import org.http4k.cloudnative.env.Environment
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import smoke.actors.MobileApp
import smoke.actors.SIPGateway
import smoke.actors.createHandler
import smoke.env.SmokeTests
import strikt.api.expectThat
import strikt.assertions.containsKeys
import strikt.assertions.getValue
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import uk.nhs.nhsx.domain.Country
import uk.nhs.nhsx.domain.Country.Companion.England
import uk.nhs.nhsx.domain.IpcTokenId
import uk.nhs.nhsx.isolationpayment.model.TokenGenerationResponse
import java.time.Duration.ofDays
import java.time.Instant
import java.time.temporal.ChronoUnit.SECONDS

class IsolationPaymentSmokeTest {
    private val client = createHandler(Environment.ENV)
    private val config = SmokeTests.loadConfig()
    private val mobileApp = MobileApp(client, config)
    private val sipGateway = SIPGateway(config)

    private val riskyEncounterDate = Instant.now().minus(ofDays(4)).truncatedTo(SECONDS)
    private val isolationPeriodEndDate = Instant.now().plus(ofDays(4)).truncatedTo(SECONDS)

    @Test @Disabled
    fun `happy path - submit isolation payment, update, verify and consume ipcToken`() {
        val ipcToken = createValidToken()
        mobileApp.updateIsolationToken(ipcToken, riskyEncounterDate, isolationPeriodEndDate)
        verifyValidToken(ipcToken)
        consumeValidToken(ipcToken)
    }

    @Test @Disabled
    fun `isolation payment order creation is not enabled for non whitelisted countries`() {
        val createIsolationToken = mobileApp.createNonWhiteListedIsolationToken(Country.of("Switzerland"))

        expectThat(createIsolationToken)
            .get(TokenGenerationResponse.Disabled::isEnabled)
            .isFalse()
    }

    @Test @Disabled
    fun `update isolation token returns website URL even when an non-existing token is passed`() {
        mobileApp.updateIsolationToken(IpcTokenId.of("1".repeat(64)), riskyEncounterDate, isolationPeriodEndDate)
    }

    @Test @Disabled
    fun `cannot consume ipcToken more than one time`() {
        val ipcToken = createValidToken()
        mobileApp.updateIsolationToken(ipcToken, riskyEncounterDate, isolationPeriodEndDate)
        verifyValidToken(ipcToken)
        consumeValidToken(ipcToken)
        consumeInvalidToken(ipcToken)
    }

    @Test @Disabled
    fun `verify fails if token is unknown or not updated yet`() {
        verifyInvalidToken(IpcTokenId.of("Z".repeat(64)))
        val ipcToken = createValidToken()
        verifyInvalidToken(ipcToken)
        mobileApp.updateIsolationToken(ipcToken, riskyEncounterDate, isolationPeriodEndDate)
        verifyValidToken(ipcToken)
        verifyValidToken(ipcToken)
    }

    @Test @Disabled
    fun `update has no effect if token is already verified`() {
        val ipcToken = createValidToken()
        mobileApp.updateIsolationToken(ipcToken, riskyEncounterDate, isolationPeriodEndDate)
        verifyValidToken(ipcToken)

        val otherDate = Instant.now()
        mobileApp.updateIsolationToken(ipcToken, otherDate, otherDate)

        verifyValidToken(ipcToken)
    }

    private fun createValidToken() = mobileApp
        .createIsolationToken(England)
        .apply { expectThat(this).hasValidIpcToken() }
        .ipcToken

    private fun verifyValidToken(ipcToken: IpcTokenId) {
        val verifyPayload = sipGateway.verifiesIpcToken(ipcToken)

        expectThat(verifyPayload) {
            getValue("state").isEqualTo("valid")
            getValue("riskyEncounterDate").get(Instant::parse).isEqualTo(riskyEncounterDate)
            getValue("isolationPeriodEndDate").get(Instant::parse).isEqualTo(isolationPeriodEndDate)
            containsKeys("createdTimestamp", "updatedTimestamp")
        }
    }

    private fun verifyInvalidToken(ipcToken: IpcTokenId) {
        val verifyPayload = sipGateway.verifiesIpcToken(ipcToken)
        expectThat(verifyPayload).getValue("state").isEqualTo("invalid")
    }

    private fun consumeValidToken(ipcToken: IpcTokenId) {
        val consumePayload = sipGateway.consumesIpcToken(ipcToken)
        expectThat(consumePayload).getValue("state").isEqualTo("consumed")
    }

    private fun consumeInvalidToken(ipcToken: IpcTokenId) {
        val consumePayload = sipGateway.consumesIpcToken(ipcToken)
        expectThat(consumePayload).getValue("state").isEqualTo("invalid")
    }
}
