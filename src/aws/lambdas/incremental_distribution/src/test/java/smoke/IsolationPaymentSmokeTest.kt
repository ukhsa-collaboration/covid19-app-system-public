package smoke

import org.assertj.core.api.Assertions.assertThat
import org.http4k.client.JavaHttpClient
import org.http4k.filter.debug
import org.junit.jupiter.api.Test
import smoke.actors.IpcToken
import smoke.actors.MobileApp
import smoke.actors.SIPGateway
import smoke.actors.UserCountry
import smoke.actors.UserCountry.England
import smoke.env.SmokeTests
import uk.nhs.nhsx.core.DateFormatValidator
import java.time.OffsetDateTime
import java.time.ZoneId

class IsolationPaymentSmokeTest {
    private val client = JavaHttpClient()
    private val config = SmokeTests.loadConfig()
    private val mobileApp = MobileApp(client.debug(), config)
    private val sipGateway = SIPGateway(config)

    private val riskyEncounterDate = OffsetDateTime.now(ZoneId.of("UTC")).minusDays(4).withNano(0)

    private val isolationPeriodEndDate = OffsetDateTime.now(ZoneId.of("UTC")).plusDays(4).withNano(0)

    @Test
    fun `happy path - submit isolation payment, update, verify and consume ipcToken`() {
        val ipcToken = createValidToken()
        mobileApp.updateIsolationToken(ipcToken, riskyEncounterDate, isolationPeriodEndDate)
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
        mobileApp.updateIsolationToken(IpcToken("1111111111111111111111111111111111111111111111111111111111111111"), riskyEncounterDate, isolationPeriodEndDate)
    }

    @Test
    fun `update isolation token returns 400 if syntactically invalid token or date is passed`() {
        mobileApp.updateIsolationTokenInvalid(IpcToken("<script>"), riskyEncounterDate.format(DateFormatValidator.formatter), isolationPeriodEndDate.format(DateFormatValidator.formatter))

        mobileApp.updateIsolationTokenInvalid(IpcToken("1111111111111111111111111111111111111111111111111111111111111111"), "<script>", isolationPeriodEndDate.format(DateFormatValidator.formatter))

        mobileApp.updateIsolationTokenInvalid(IpcToken("1111111111111111111111111111111111111111111111111111111111111111"), riskyEncounterDate.format(DateFormatValidator.formatter), "<script>")
    }

    @Test
    fun `cannot consume ipcToken more than one time`() {
        val ipcToken = createValidToken();
        mobileApp.updateIsolationToken(ipcToken, riskyEncounterDate, isolationPeriodEndDate)
        verifyValidToken(ipcToken)
        consumeValidToken(ipcToken)
        consumeInvalidToken(ipcToken)
    }

    @Test
    fun `verify fails if token is invalid or not updated yet`() {
        verifyInvalidToken(IpcToken("invalid-token"))
        val ipcToken = createValidToken();
        verifyInvalidToken(ipcToken)
        mobileApp.updateIsolationToken(ipcToken, riskyEncounterDate, isolationPeriodEndDate)
        verifyValidToken(ipcToken)
        verifyValidToken(ipcToken)
    }

    @Test
    fun `update has no effect if token is already verified`() {
        val ipcToken = createValidToken();
        mobileApp.updateIsolationToken(ipcToken, riskyEncounterDate, isolationPeriodEndDate)
        verifyValidToken(ipcToken)

        val otherDate = OffsetDateTime.now();
        mobileApp.updateIsolationToken(ipcToken, otherDate, otherDate)

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
        assertThat(OffsetDateTime.parse(verifyPayload["riskyEncounterDate"])).isEqualTo(riskyEncounterDate)
        assertThat(OffsetDateTime.parse(verifyPayload["isolationPeriodEndDate"])).isEqualTo(isolationPeriodEndDate)
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
