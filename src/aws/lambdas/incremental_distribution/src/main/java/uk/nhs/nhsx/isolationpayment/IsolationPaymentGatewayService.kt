package uk.nhs.nhsx.isolationpayment

import uk.nhs.nhsx.core.DateFormatValidator
import uk.nhs.nhsx.core.events.ConsumeIsolationTokenFailed
import uk.nhs.nhsx.core.events.ConsumeIsolationTokenSucceeded
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.TokenFailureReason.NotFound
import uk.nhs.nhsx.core.events.TokenFailureReason.WrongState
import uk.nhs.nhsx.core.events.UpdateIsolationTokenSucceeded
import uk.nhs.nhsx.core.events.VerifyIsolationTokenFailed
import uk.nhs.nhsx.isolationpayment.model.IsolationResponse
import uk.nhs.nhsx.isolationpayment.model.IsolationToken
import uk.nhs.nhsx.isolationpayment.model.TokenStateExternal
import uk.nhs.nhsx.isolationpayment.model.TokenStateInternal
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.*
import java.util.function.Supplier

class IsolationPaymentGatewayService(
    private val systemClock: Supplier<Instant>,
    private val persistence: IsolationPaymentPersistence,
    private val auditLogPrefix: String,
    private val events: Events
) {

    fun verifyIsolationToken(ipcToken: String): IsolationResponse {
        val isolationToken: Optional<IsolationToken> = try {
            persistence.getIsolationToken(ipcToken)
        } catch (e: Exception) {
            throw RuntimeException("$auditLogPrefix VerifyToken exception: tokenId=$ipcToken", e)
        }

        if (isolationToken.isEmpty) {
            events.emit(
                javaClass, VerifyIsolationTokenFailed(
                    auditLogPrefix,
                    NotFound,
                    IsolationToken().apply { tokenId = ipcToken }
                )
            )
            return IsolationResponse(ipcToken, TokenStateExternal.EXT_INVALID.value)
        }

        if (TokenStateInternal.INT_UPDATED.value != isolationToken.get().tokenStatus) {
            events.emit(
                javaClass, VerifyIsolationTokenFailed(
                    auditLogPrefix,
                    WrongState,
                    isolationToken.get()
                )
            )

            return IsolationResponse(ipcToken, TokenStateExternal.EXT_INVALID.value)
        }

        val updatedToken = IsolationToken.clonedToken(isolationToken.get()).apply {
            validatedTimestamp = systemClock.get().epochSecond
        }

        return try {
            persistence.updateIsolationToken(updatedToken, TokenStateInternal.INT_UPDATED)
            events.emit(
                javaClass, UpdateIsolationTokenSucceeded(
                    auditLogPrefix,
                    isolationToken.get(),
                    updatedToken
                )
            )

            IsolationResponse(
                ipcToken,
                TokenStateExternal.EXT_VALID.value,
                convertToString(updatedToken.riskyEncounterDate),
                convertToString(updatedToken.isolationPeriodEndDate),
                convertToString(updatedToken.createdTimestamp),
                convertToString(updatedToken.updatedTimestamp)
            )
        } catch (e: Exception) {
            throw RuntimeException("$auditLogPrefix VerifyToken exception: existing.ipcToken${isolationToken.get()} !updated.ipcToken=$updatedToken")
        }
    }

    private fun convertToString(date: Long): String {
        return DateFormatValidator.formatter
            .withZone(ZoneId.from(ZoneOffset.UTC))
            .format(Instant.ofEpochSecond(date))
    }

    fun consumeIsolationToken(ipcToken: String): IsolationResponse {
        val isolationToken: Optional<IsolationToken> = try {
            persistence.getIsolationToken(ipcToken)
        } catch (e: Exception) {
            throw RuntimeException("$auditLogPrefix ConsumeToken exception: tokenId=$ipcToken", e)
        }

        if (isolationToken.isEmpty) {
            events.emit(
                javaClass, ConsumeIsolationTokenFailed(
                    auditLogPrefix,
                    NotFound,
                    IsolationToken().apply { tokenId = ipcToken })
            )

            return IsolationResponse(ipcToken, TokenStateExternal.EXT_INVALID.value)
        }

        if (TokenStateInternal.INT_UPDATED.value != isolationToken.get().tokenStatus) {
            events.emit(
                javaClass, ConsumeIsolationTokenFailed(
                    auditLogPrefix,
                    reason = WrongState,
                    isolationToken.get()
                )
            )

            return IsolationResponse(ipcToken, TokenStateExternal.EXT_INVALID.value)
        }

        return try {
            persistence.deleteIsolationToken(ipcToken, TokenStateInternal.INT_UPDATED)
            events.emit(
                javaClass, ConsumeIsolationTokenSucceeded(
                    auditLogPrefix,
                    isolationToken.get()
                )
            )
            IsolationResponse(ipcToken, TokenStateExternal.EXT_CONSUMED.value)
        } catch (e: Exception) {
            throw RuntimeException("$auditLogPrefix ConsumeToken exception: ipcToken=${isolationToken.get()}", e)
        }
    }
}
