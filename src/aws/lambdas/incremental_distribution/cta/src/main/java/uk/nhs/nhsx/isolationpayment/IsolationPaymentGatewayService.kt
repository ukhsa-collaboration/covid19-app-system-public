package uk.nhs.nhsx.isolationpayment

import uk.nhs.nhsx.core.Clock
import uk.nhs.nhsx.core.events.ConsumeIsolationTokenFailed
import uk.nhs.nhsx.core.events.ConsumeIsolationTokenSucceeded
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.TokenFailureReason.NotFound
import uk.nhs.nhsx.core.events.TokenFailureReason.WrongState
import uk.nhs.nhsx.core.events.UpdateIsolationTokenSucceeded
import uk.nhs.nhsx.core.events.VerifyIsolationTokenFailed
import uk.nhs.nhsx.domain.IpcTokenId
import uk.nhs.nhsx.isolationpayment.model.IsolationResponse
import uk.nhs.nhsx.isolationpayment.model.TokenStateExternal.EXT_CONSUMED
import uk.nhs.nhsx.isolationpayment.model.TokenStateExternal.EXT_INVALID
import uk.nhs.nhsx.isolationpayment.model.TokenStateExternal.EXT_VALID
import uk.nhs.nhsx.isolationpayment.model.TokenStateInternal.INT_UPDATED
import uk.nhs.nhsx.isolationpayment.model.isStateNotEqual

class IsolationPaymentGatewayService(
    private val systemClock: Clock,
    private val persistence: IsolationPaymentPersistence,
    private val auditLogPrefix: String,
    private val events: Events
) {

    fun verifyIsolationToken(ipcToken: IpcTokenId): IsolationResponse {
        val isolationToken = persistence.getIsolationToken(ipcToken)

        return when {
            isolationToken == null -> {
                events(VerifyIsolationTokenFailed(auditLogPrefix, NotFound, ipcToken))
                IsolationResponse.of(ipcToken, EXT_INVALID)
            }
            isolationToken.isStateNotEqual(INT_UPDATED) -> {
                events(VerifyIsolationTokenFailed(auditLogPrefix, WrongState, ipcToken, isolationToken))
                IsolationResponse.of(ipcToken, EXT_INVALID)
            }
            else -> {
                val updatedToken = isolationToken.copy(validatedTimestamp = systemClock().epochSecond)
                try {
                    persistence.updateIsolationToken(updatedToken, INT_UPDATED)
                    events(UpdateIsolationTokenSucceeded(auditLogPrefix, isolationToken, updatedToken))
                    IsolationResponse.of(ipcToken, EXT_VALID, updatedToken)
                } catch (e: Exception) {
                    throw RuntimeException("$auditLogPrefix VerifyToken exception: existing.ipcToken${isolationToken} !updated.ipcToken=$updatedToken")
                }
            }
        }
    }

    fun consumeIsolationToken(ipcToken: IpcTokenId): IsolationResponse {
        val isolationToken = persistence.getIsolationToken(ipcToken)

        return when {
            isolationToken == null -> {
                events(ConsumeIsolationTokenFailed(auditLogPrefix, NotFound, ipcToken))
                IsolationResponse.of(ipcToken, EXT_INVALID)
            }
            isolationToken.isStateNotEqual(INT_UPDATED) -> {
                events(ConsumeIsolationTokenFailed(auditLogPrefix, reason = WrongState, ipcToken, isolationToken))
                IsolationResponse.of(ipcToken, EXT_INVALID)
            }
            else -> try {
                persistence.deleteIsolationToken(ipcToken, INT_UPDATED)
                events(ConsumeIsolationTokenSucceeded(auditLogPrefix, ipcToken, isolationToken))
                IsolationResponse.of(ipcToken, EXT_CONSUMED)
            } catch (e: Exception) {
                throw RuntimeException("$auditLogPrefix ConsumeToken exception: ipcToken=${isolationToken}", e)
            }
        }
    }
}
