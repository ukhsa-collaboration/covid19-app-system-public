package smoke

import org.assertj.core.api.Assertions.assertThat
import org.http4k.client.JavaHttpClient
import org.junit.jupiter.api.Test
import smoke.actors.MobileApp
import smoke.actors.SIPGateway
import smoke.actors.IpcToken
import smoke.actors.UserCountry
import smoke.actors.UserCountry.England
import smoke.env.SmokeTests
import uk.nhs.nhsx.core.DateFormatValidator
import java.time.OffsetDateTime

class IsolationPaymentSmokeTest {
    private val client = JavaHttpClient()
    private val config = SmokeTests.loadConfig()
    private val mobileApp = MobileApp(client, config)
    private val sipGateway = SIPGateway(config)

    private val riskyEncounterDate = OffsetDateTime.now().minusDays(4);
    private val riskyEncounterDateString = riskyEncounterDate.format(DateFormatValidator.formatter);

    private val isolationPeriodEndDate = OffsetDateTime.now().plusDays(4);
    private val isolationPeriodEndDateString = isolationPeriodEndDate.format(DateFormatValidator.formatter);

    @Test
    fun `happy path - submit isolation payment, update, verify and consume ipcToken`() {
        val ipcToken = createValidToken()
        mobileApp.updateIsolationToken(ipcToken, riskyEncounterDateString, isolationPeriodEndDateString)
        verifyValidToken(ipcToken)
        consumeValidToken(ipcToken)
    }

    @Test
    fun `isolation payment order creation is not enabled for non whitelisted countries`() {
        val isolationCreationResponse = mobileApp.createIsolationToken(UserCountry.Other("Switzerland"))
        assertThat(isolationCreationResponse.ipcToken).isNull()
        assertThat(isolationCreationResponse.isEnabled).isFalse
    }

    @Test
    fun `update isolation token returns website URL even when an non-existing token is passed`() {
        mobileApp.updateIsolationToken(IpcToken("1111111111111111111111111111111111111111111111111111111111111111"), riskyEncounterDateString, isolationPeriodEndDateString)
    }

    @Test
    fun `update isolation token returns 400 if syntactically invalid token or date is passed`() {
        mobileApp.updateIsolationTokenInvalid(IpcToken("<script>"), riskyEncounterDateString, isolationPeriodEndDateString)

        mobileApp.updateIsolationTokenInvalid(IpcToken("1111111111111111111111111111111111111111111111111111111111111111"), "<script>", isolationPeriodEndDateString)

        mobileApp.updateIsolationTokenInvalid(IpcToken("1111111111111111111111111111111111111111111111111111111111111111"), riskyEncounterDateString, "<script>")
    }

    @Test
    fun `cannot consume ipcToken more than one time`() {
        val ipcToken = createValidToken();
        mobileApp.updateIsolationToken(ipcToken, riskyEncounterDateString, isolationPeriodEndDateString)
        verifyValidToken(ipcToken)
        consumeValidToken(ipcToken)
        consumeInvalidToken(ipcToken)
    }

    @Test
    fun `verify fails if token is invalid or not updated yet`() {
        verifyInvalidToken(IpcToken("invalid-token"))
        val ipcToken = createValidToken();
        verifyInvalidToken(ipcToken)
        mobileApp.updateIsolationToken(ipcToken, riskyEncounterDateString, isolationPeriodEndDateString)
        verifyValidToken(ipcToken)
        verifyValidToken(ipcToken)
    }

    @Test
    fun `update has no effect if token is already verified`() {
        val ipcToken = createValidToken();
        mobileApp.updateIsolationToken(ipcToken, riskyEncounterDateString, isolationPeriodEndDateString)
        verifyValidToken(ipcToken)

        val otherDate = OffsetDateTime.now();
        val otherDateString = otherDate.format(DateFormatValidator.formatter);
        mobileApp.updateIsolationToken(ipcToken, otherDateString, otherDateString)

        verifyValidToken(ipcToken)
    }

    private fun createValidToken(): IpcToken {
        val isolationCreationResponse = mobileApp.createIsolationToken(England)
        assertThat(isolationCreationResponse.ipcToken).isNotEmpty
        assertThat(isolationCreationResponse.isEnabled).isTrue
        return IpcToken(isolationCreationResponse.ipcToken)
    }

    private fun verifyValidToken(ipcToken: IpcToken) {
        val verifyPayload = sipGateway.verifiesIpcToken(ipcToken)
        assertThat(verifyPayload["state"]).isEqualTo("valid")
        assertThat(verifyPayload["riskyEncounterDate"]).isEqualTo(riskyEncounterDateString)
        assertThat(verifyPayload["isolationPeriodEndDate"]).isEqualTo(isolationPeriodEndDateString)
        assertThat(verifyPayload).containsKey("createdTimestamp")
        assertThat(verifyPayload).containsKey("updatedTimestamp")
    }

    private fun verifyInvalidToken(ipcToken: IpcToken) {
        val verifyPayload = sipGateway.verifiesIpcToken(ipcToken)
        assertThat(verifyPayload["state"]).isEqualTo("invalid")
    }

    private fun consumeValidToken(ipcToken: IpcToken) {
        val consumePayload = sipGateway.consumesIpcToken(ipcToken)
        assertThat(consumePayload["state"]).isEqualTo("consumed")
    }

    private fun consumeInvalidToken(ipcToken: IpcToken) {
        val consumePayload = sipGateway.consumesIpcToken(ipcToken)
        assertThat(consumePayload["state"]).isEqualTo("invalid")
    }

}
