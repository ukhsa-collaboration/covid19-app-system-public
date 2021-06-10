package uk.nhs.nhsx.isolationpayment

import uk.nhs.nhsx.core.Clock
import uk.nhs.nhsx.core.events.ConsumeIsolationTokenFailed
import uk.nhs.nhsx.core.events.ConsumeIsolationTokenSucceeded
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.TokenFailureReason.NotFound
import uk.nhs.nhsx.core.events.TokenFailureReason.WrongState
import uk.nhs.nhsx.core.events.UpdateIsolationTokenSucceeded
import uk.nhs.nhsx.core.events.VerifyIsolationTokenFailed
import uk.nhs.nhsx.isolationpayment.model.IsolationResponse
import uk.nhs.nhsx.isolationpayment.model.IsolationToken
import uk.nhs.nhsx.isolationpayment.model.TokenStateExternal.EXT_CONSUMED
import uk.nhs.nhsx.isolationpayment.model.TokenStateExternal.EXT_INVALID
import uk.nhs.nhsx.isolationpayment.model.TokenStateExternal.EXT_VALID
import uk.nhs.nhsx.isolationpayment.model.TokenStateInternal.INT_UPDATED
import uk.nhs.nhsx.domain.IpcTokenId
import java.time.Instant.ofEpochSecond
import java.util.Optional

class IsolationPaymentGatewayService(
    private val systemClock: Clock,
    private val persistence: IsolationPaymentPersistence,
    private val auditLogPrefix: String,
    private val events: Events
) {

    fun verifyIsolationToken(ipcToken: IpcTokenId): IsolationResponse {
        val isolationToken: Optional<IsolationToken> = try {
            persistence.getIsolationToken(ipcToken)
        } catch (e: Exception) {
            throw RuntimeException("$auditLogPrefix VerifyToken exception: tokenId=$ipcToken", e)
        }

        if (isolationToken.isEmpty) {
            events(
                VerifyIsolationTokenFailed(
                    auditLogPrefix,
                    NotFound,
                    ipcToken
                )
            )
            return IsolationResponse(ipcToken, EXT_INVALID.value)
        }

        if (INT_UPDATED.value != isolationToken.get().tokenStatus) {
            events(
                VerifyIsolationTokenFailed(
                    auditLogPrefix,
                    WrongState,
                    ipcToken,
                    isolationToken.get()
                )
            )

            return IsolationResponse(ipcToken, EXT_INVALID.value)
        }

        val updatedToken = isolationToken.get().copy(validatedTimestamp = systemClock().epochSecond)

        return try {
            persistence.updateIsolationToken(updatedToken, INT_UPDATED)
            events(
                UpdateIsolationTokenSucceeded(
                    auditLogPrefix,
                    isolationToken.get(),
                    updatedToken
                )
            )

            IsolationResponse(
                ipcToken,
                EXT_VALID.value,
                updatedToken.riskyEncounterDate?.let(::ofEpochSecond),
                updatedToken.isolationPeriodEndDate?.let(::ofEpochSecond),
                ofEpochSecond(updatedToken.createdTimestamp),
                updatedToken.updatedTimestamp?.let(::ofEpochSecond),
            )
        } catch (e: Exception) {
            throw RuntimeException("$auditLogPrefix VerifyToken exception: existing.ipcToken${isolationToken.get()} !updated.ipcToken=$updatedToken")
        }
    }

    fun consumeIsolationToken(ipcToken: IpcTokenId): IsolationResponse {
        val isolationToken: Optional<IsolationToken> = try {
            persistence.getIsolationToken(ipcToken)
        } catch (e: Exception) {
            throw RuntimeException("$auditLogPrefix ConsumeToken exception: tokenId=$ipcToken", e)
        }

        if (isolationToken.isEmpty) {
            events(
                ConsumeIsolationTokenFailed(
                    auditLogPrefix,
                    NotFound,
                    ipcToken
                )
            )

            return IsolationResponse(ipcToken, EXT_INVALID.value)
        }

        if (INT_UPDATED.value != isolationToken.get().tokenStatus) {
            events(
                ConsumeIsolationTokenFailed(
                    auditLogPrefix,
                    reason = WrongState,
                    ipcToken,
                    isolationToken.get()
                )
            )

            return IsolationResponse(ipcToken, EXT_INVALID.value)
        }

        return try {
            persistence.deleteIsolationToken(ipcToken, INT_UPDATED)
            events(
                ConsumeIsolationTokenSucceeded(
                    auditLogPrefix,
                    ipcToken,
                    isolationToken.get()
                )
            )
            IsolationResponse(ipcToken, EXT_CONSUMED.value)
        } catch (e: Exception) {
            throw RuntimeException("$auditLogPrefix ConsumeToken exception: ipcToken=${isolationToken.get()}", e)
        }
    }
}
