package uk.nhs.nhsx.isolationpayment

import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException
import uk.nhs.nhsx.core.DateFormatValidator
import uk.nhs.nhsx.core.events.CreateIPCTokenFailed
import uk.nhs.nhsx.core.events.CreateIPCTokenNotEnabled
import uk.nhs.nhsx.core.events.CreateIPCTokenSucceeded
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.TokenFailureReason.ConditionalCheck
import uk.nhs.nhsx.core.events.TokenFailureReason.NotFound
import uk.nhs.nhsx.core.events.TokenFailureReason.WrongState
import uk.nhs.nhsx.core.events.UpdateIPCTokenFailed
import uk.nhs.nhsx.core.events.UpdateIPCTokenSucceeded
import uk.nhs.nhsx.isolationpayment.model.IsolationToken
import uk.nhs.nhsx.isolationpayment.model.TokenGenerationRequest
import uk.nhs.nhsx.isolationpayment.model.TokenGenerationResponse
import uk.nhs.nhsx.isolationpayment.model.TokenStateInternal
import uk.nhs.nhsx.isolationpayment.model.TokenUpdateRequest
import uk.nhs.nhsx.isolationpayment.model.TokenUpdateResponse
import java.time.Instant
import java.time.Period
import java.util.*
import java.util.function.Supplier

class IsolationPaymentMobileService(
    private val systemClock: Supplier<Instant>,
    private val tokenGenerator: Supplier<String>,
    private val persistence: IsolationPaymentPersistence,
    private val isolationPaymentWebsite: String,
    private val tokenExpiryInWeeks: Int,
    private val countriesWhitelisted: List<String>,
    private val auditLogPrefix: String,
    private val events: Events
) {

    fun handleIsolationPaymentOrder(request: TokenGenerationRequest): TokenGenerationResponse {
        val isEnabled = countriesWhitelisted.contains(request.country)

        if (!isEnabled) {
            events.emit(javaClass, CreateIPCTokenNotEnabled(auditLogPrefix, request.country))
            return TokenGenerationResponse(false)
        }

        val isolationToken = IsolationToken().apply {
            tokenId = tokenGenerator.get()
            tokenStatus = TokenStateInternal.INT_CREATED.value
            createdTimestamp = systemClock.get().epochSecond
            expireAt = systemClock.get().plus(Period.ofWeeks(tokenExpiryInWeeks)).epochSecond
        }

        return try {
            persistence.insertIsolationToken(isolationToken)
            events.emit(javaClass, CreateIPCTokenSucceeded(auditLogPrefix, isolationToken))
            TokenGenerationResponse(true, isolationToken.tokenId)
        } catch (e: Exception) {
            events.emit(javaClass, CreateIPCTokenFailed(auditLogPrefix, isolationToken, e))
            throw RuntimeException(e)
        }
    }

    fun handleIsolationPaymentUpdate(request: TokenUpdateRequest): TokenUpdateResponse {
        val websiteUrlWithQuery = isolationPaymentWebsite + request.ipcToken

        val isolationToken: Optional<IsolationToken> = try {
            persistence.getIsolationToken(request.ipcToken)
        } catch (e: Exception) {
            throw RuntimeException("$auditLogPrefix UpdateToken exception: ipcToken=${request.ipcToken}", e)
        }

        if (isolationToken.isEmpty) { //API contract: we don't report this back to the client
            events.emit(
                javaClass, UpdateIPCTokenFailed(
                    auditLogPrefix,
                    NotFound,
                    IsolationToken().apply { tokenId = request.ipcToken },
                    redirectUrl = websiteUrlWithQuery
                )
            )
        } else if (TokenStateInternal.INT_CREATED.value == isolationToken.get().tokenStatus) {
            val updatedToken = IsolationToken.clonedToken(isolationToken.get())

            try {
                updatedToken.apply {
                    riskyEncounterDate = toDateOrThrow(request.riskyEncounterDate)
                    isolationPeriodEndDate = toDateOrThrow(request.isolationPeriodEndDate)
                    updatedTimestamp = systemClock.get().epochSecond
                    tokenStatus = TokenStateInternal.INT_UPDATED.value
                    expireAt = updatedToken.isolationPeriodEndDate
                }
            } catch (e: Exception) {
                throw RuntimeException(
                    "$auditLogPrefix ConsumeToken exception: tokenId=${request.ipcToken}",
                    e
                )
            }
            try {
                persistence.updateIsolationToken(updatedToken, TokenStateInternal.INT_CREATED)
                events.emit(
                    javaClass, UpdateIPCTokenSucceeded(
                        auditLogPrefix,
                        isolationToken.get(),
                        updatedToken,
                        websiteUrlWithQuery
                    )
                )
            } catch (e: ConditionalCheckFailedException) {
                events.emit(
                    javaClass, UpdateIPCTokenFailed(
                        auditLogPrefix,
                        ConditionalCheck,
                        isolationToken.orElse(null),
                        updatedToken = updatedToken,
                        redirectUrl = websiteUrlWithQuery,
                        exception = e
                    )
                )
            } catch (e: Exception) {
                throw RuntimeException(
                    "$auditLogPrefix UpdateToken exception: ipcToken=${isolationToken.get()}, !updated.ipcToken=$updatedToken",
                    e
                )
            }
        } else { //API contract: we don't report this back to the client
            events.emit(
                javaClass, UpdateIPCTokenFailed(
                    auditLogPrefix,
                    WrongState,
                    isolationToken.orElse(null),
                    redirectUrl = websiteUrlWithQuery
                )
            )
        }
        return TokenUpdateResponse(websiteUrlWithQuery)
    }

    private fun toDateOrThrow(input: String?) =
        DateFormatValidator.toZonedDateTimeMaybe(input).map { it.toEpochSecond() }.orElseThrow()
}
