package uk.nhs.nhsx.isolationpayment

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verifySequence
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.domain.IpcTokenId
import uk.nhs.nhsx.isolationpayment.model.IsolationResponse
import uk.nhs.nhsx.isolationpayment.model.IsolationToken
import uk.nhs.nhsx.isolationpayment.model.TokenStateExternal.EXT_CONSUMED
import uk.nhs.nhsx.isolationpayment.model.TokenStateExternal.EXT_INVALID
import uk.nhs.nhsx.isolationpayment.model.TokenStateExternal.EXT_VALID
import uk.nhs.nhsx.isolationpayment.model.TokenStateInternal.INT_CREATED
import uk.nhs.nhsx.isolationpayment.model.TokenStateInternal.INT_UPDATED
import uk.nhs.nhsx.testhelper.assertions.captured
import java.time.Instant
import java.util.*

class IsolationPaymentGatewayServiceTest {

    private val clock = { Instant.parse("2020-12-01T00:00:00Z") }

    private val createdStateToken = IsolationToken(
        tokenId = IpcTokenId.of("1".repeat(64)),
        tokenStatus = INT_CREATED.value,
        riskyEncounterDate = 0,
        isolationPeriodEndDate = 0,
        createdTimestamp = 0,
        updatedTimestamp = 0,
        validatedTimestamp = 0,
        consumedTimestamp = 0,
        expireAt = 0
    )

    private val validStateToken = IsolationToken(
        tokenId = IpcTokenId.of("1".repeat(64)),
        tokenStatus = INT_UPDATED.value,
        riskyEncounterDate = 0,
        isolationPeriodEndDate = 0,
        createdTimestamp = 0,
        updatedTimestamp = 0,
        validatedTimestamp = 0,
        consumedTimestamp = 0,
        expireAt = 0
    )

    private val auditLogSuffix = "audit-log-suffix"
    private val persistence = mockk<IsolationPaymentPersistence>()
    private val service = IsolationPaymentGatewayService(clock, persistence, auditLogSuffix, RecordingEvents())

    @Test
    fun `consumes isolation token deleting existing token`() {
        every { persistence.getIsolationToken(any()) } returns Optional.of(validStateToken)
        every { persistence.deleteIsolationToken(any(), any()) } just runs

        val response = service.consumeIsolationToken(validStateToken.tokenId)

        expectThat(response) {
            get(IsolationResponse::ipcToken).isEqualTo(validStateToken.tokenId)
            get(IsolationResponse::state).isEqualTo(EXT_CONSUMED.value)
        }

        verifySequence {
            persistence.getIsolationToken(validStateToken.tokenId)
            persistence.deleteIsolationToken(validStateToken.tokenId, INT_UPDATED)
        }
    }

    @Test
    fun `consumes isolation token handling not found token id`() {
        every { persistence.getIsolationToken(any()) } returns Optional.empty()

        val ipcToken = IpcTokenId.of("2".repeat(64))
        val response = service.consumeIsolationToken(ipcToken)

        expectThat(response) {
            get(IsolationResponse::ipcToken).isEqualTo(ipcToken)
            get(IsolationResponse::state).isEqualTo(EXT_INVALID.value)
        }

        verifySequence {
            persistence.getIsolationToken(ipcToken)
        }
    }

    @Test
    fun `consumes isolation token handling token in created state`() {
        every { persistence.getIsolationToken(any()) } returns Optional.of(createdStateToken)

        val response = service.consumeIsolationToken(createdStateToken.tokenId)

        expectThat(response) {
            get(IsolationResponse::ipcToken).isEqualTo(createdStateToken.tokenId)
            get(IsolationResponse::state).isEqualTo(EXT_INVALID.value)
        }

        verifySequence {
            persistence.getIsolationToken(createdStateToken.tokenId)
        }
    }

    @Test
    fun `verifies isolation token and updates existing token validated timestamp`() {
        every { persistence.getIsolationToken(any()) } returns Optional.of(validStateToken)
        every { persistence.updateIsolationToken(any(), any()) } just runs

        val response = service.verifyIsolationToken(validStateToken.tokenId)

        expectThat(response) {
            get(IsolationResponse::ipcToken).isEqualTo(validStateToken.tokenId)
            get(IsolationResponse::state).isEqualTo(EXT_VALID.value)
        }

        val isolationToken = slot<IsolationToken>()
        verifySequence {
            persistence.getIsolationToken(validStateToken.tokenId)
            persistence.updateIsolationToken(capture(isolationToken), INT_UPDATED)
        }

        expectThat(isolationToken)
            .captured
            .isEqualTo(validStateToken.copy(validatedTimestamp = clock().epochSecond))
    }

    @Test
    fun `verifies isolation token handling not found token id`() {
        every { persistence.getIsolationToken(any()) } returns Optional.empty()

        val ipcToken = IpcTokenId.of("3".repeat(64))
        val response = service.verifyIsolationToken(ipcToken)

        expectThat(response) {
            get(IsolationResponse::ipcToken).isEqualTo(ipcToken)
            get(IsolationResponse::state).isEqualTo(EXT_INVALID.value)
        }

        verifySequence {
            persistence.getIsolationToken(ipcToken)
        }
    }

    @Test
    fun `verifies isolation token handling token in created state`() {
        every { persistence.getIsolationToken(any()) } returns Optional.of(createdStateToken)

        val response = service.verifyIsolationToken(createdStateToken.tokenId)

        expectThat(response) {
            get(IsolationResponse::ipcToken).isEqualTo(createdStateToken.tokenId)
            get(IsolationResponse::state).isEqualTo(EXT_INVALID.value)
        }
    }
}
