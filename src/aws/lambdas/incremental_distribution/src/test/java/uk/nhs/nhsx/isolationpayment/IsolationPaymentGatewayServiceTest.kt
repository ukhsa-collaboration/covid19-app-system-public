package uk.nhs.nhsx.isolationpayment

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verifySequence
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.isolationpayment.model.IsolationToken
import uk.nhs.nhsx.isolationpayment.model.TokenStateExternal
import uk.nhs.nhsx.isolationpayment.model.TokenStateInternal
import uk.nhs.nhsx.virology.IpcTokenId
import java.time.Instant
import java.util.*
import java.util.function.Supplier

class IsolationPaymentGatewayServiceTest {

    private val clock = Supplier { Instant.parse("2020-12-01T00:00:00Z") }
    private val createdStateToken = IsolationToken(IpcTokenId.of("1".repeat(64)), TokenStateInternal.INT_CREATED.value, 0, 0, 0, 0, 0, 0, 0)
    private val validStateToken = IsolationToken(IpcTokenId.of("1".repeat(64)), TokenStateInternal.INT_UPDATED.value, 0, 0, 0, 0, 0, 0, 0)
    private val auditLogSuffix = "audit-log-suffix"
    private val persistence = mockk<IsolationPaymentPersistence>()
    private val service = IsolationPaymentGatewayService(clock, persistence, auditLogSuffix, RecordingEvents())

    @Test
    fun `consumes isolation token deleting existing token`() {
        every { persistence.getIsolationToken(any()) } returns Optional.of(validStateToken)
        every { persistence.deleteIsolationToken(any(), any()) } just Runs

        val response = service.consumeIsolationToken(validStateToken.tokenId)

        assertThat(response.ipcToken).isEqualTo(validStateToken.tokenId)
        assertThat(response.state).isEqualTo(TokenStateExternal.EXT_CONSUMED.value)

        verifySequence {
            persistence.getIsolationToken(validStateToken.tokenId)
            persistence.deleteIsolationToken(validStateToken.tokenId, TokenStateInternal.INT_UPDATED)
        }
    }

    @Test
    fun `consumes isolation token handling not found token id`() {
        every { persistence.getIsolationToken(any()) } returns Optional.empty()

        val response = service.consumeIsolationToken(IpcTokenId.of("1".repeat(64)))

        assertThat(response.ipcToken).isEqualTo(IpcTokenId.of("1".repeat(64)))
        assertThat(response.state).isEqualTo(TokenStateExternal.EXT_INVALID.value)

        verifySequence {
            persistence.getIsolationToken(IpcTokenId.of("1".repeat(64)))
        }
    }

    @Test
    fun `consumes isolation token handling token in created state`() {
        every { persistence.getIsolationToken(any()) } returns Optional.of(createdStateToken)

        val response = service.consumeIsolationToken(createdStateToken.tokenId)

        assertThat(response.ipcToken).isEqualTo(createdStateToken.tokenId)
        assertThat(response.state).isEqualTo(TokenStateExternal.EXT_INVALID.value)

        verifySequence {
            persistence.getIsolationToken(createdStateToken.tokenId)
        }
    }

    @Test
    fun `verifies isolation token and updates existing token validated timestamp`() {
        every { persistence.getIsolationToken(any()) } returns Optional.of(validStateToken)
        every { persistence.updateIsolationToken(any(), any()) } just Runs


        val response = service.verifyIsolationToken(validStateToken.tokenId)

        assertThat(response.ipcToken).isEqualTo(validStateToken.tokenId)
        assertThat(response.state).isEqualTo(TokenStateExternal.EXT_VALID.value)

        val slot = slot<IsolationToken>()
        verifySequence {
            persistence.getIsolationToken(validStateToken.tokenId)
            persistence.updateIsolationToken(capture(slot), TokenStateInternal.INT_UPDATED)
        }

        assertThat(slot.captured.tokenId).isEqualTo(validStateToken.tokenId)

        val newValidatedTimestamp = clock.get().epochSecond
        assertThat(slot.captured.validatedTimestamp).isEqualTo(newValidatedTimestamp)
        assertThat(slot.captured.riskyEncounterDate).isEqualTo(validStateToken.riskyEncounterDate)
        assertThat(slot.captured.isolationPeriodEndDate).isEqualTo(validStateToken.isolationPeriodEndDate)
        assertThat(slot.captured.createdTimestamp).isEqualTo(validStateToken.createdTimestamp)
        assertThat(slot.captured.updatedTimestamp).isEqualTo(validStateToken.updatedTimestamp)
        assertThat(slot.captured.consumedTimestamp).isEqualTo(validStateToken.consumedTimestamp)
        assertThat(slot.captured.expireAt).isEqualTo(validStateToken.expireAt)
    }

    @Test
    fun `verifies isolation token handling not found token id`() {
        every { persistence.getIsolationToken(any()) } returns Optional.empty()

        val response = service.verifyIsolationToken(IpcTokenId.of("1".repeat(64)))

        assertThat(response.ipcToken).isEqualTo(IpcTokenId.of("1".repeat(64)))
        assertThat(response.state).isEqualTo(TokenStateExternal.EXT_INVALID.value)

        verifySequence {
            persistence.getIsolationToken(IpcTokenId.of("1".repeat(64)))
        }
    }

    @Test
    fun `verifies isolation token handling token in created state`() {
        every { persistence.getIsolationToken(any()) } returns Optional.of(createdStateToken)

        val response = service.verifyIsolationToken(createdStateToken.tokenId)

        assertThat(response.ipcToken).isEqualTo(createdStateToken.tokenId)
        assertThat(response.state).isEqualTo(TokenStateExternal.EXT_INVALID.value)
    }
}
