package uk.nhs.nhsx.isolationpayment

import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException
import uk.nhs.nhsx.core.Clock
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
import uk.nhs.nhsx.isolationpayment.model.TokenGenerationResponse.OK
import uk.nhs.nhsx.isolationpayment.model.TokenStateInternal
import uk.nhs.nhsx.isolationpayment.model.TokenUpdateRequest
import uk.nhs.nhsx.isolationpayment.model.TokenUpdateResponse
import uk.nhs.nhsx.virology.IpcTokenId
import java.time.Period
import java.util.Optional
import java.util.function.Supplier

class IsolationPaymentMobileService(
    private val systemClock: Clock,
    private val tokenGenerator: Supplier<IpcTokenId>,
    private val persistence: IsolationPaymentPersistence,
    private val isolationPaymentWebsite: String,
    private val tokenExpiryInWeeks: Int,
    private val countriesWhitelisted: List<String>,
    private val auditLogPrefix: String,
    private val events: Events
) {

    fun handleIsolationPaymentOrder(request: TokenGenerationRequest): TokenGenerationResponse {
        val isEnabled = countriesWhitelisted.contains(request.country.value)

        if (!isEnabled) {
            events(CreateIPCTokenNotEnabled(auditLogPrefix, request.country))
            return TokenGenerationResponse.Disabled()
        }

        val isolationToken = IsolationToken(
            tokenId = tokenGenerator.get(),
            tokenStatus = TokenStateInternal.INT_CREATED.value,
            createdTimestamp = systemClock().epochSecond,
            expireAt = systemClock().plus(Period.ofWeeks(tokenExpiryInWeeks)).epochSecond
        )

        return try {
            persistence.insertIsolationToken(isolationToken)
            events(CreateIPCTokenSucceeded(auditLogPrefix, isolationToken))
            OK(isolationToken.tokenId)
        } catch (e: Exception) {
            events(CreateIPCTokenFailed(auditLogPrefix, isolationToken, e))
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
            events(
                UpdateIPCTokenFailed(
                    auditLogPrefix,
                    NotFound,
                    request.ipcToken,
                    redirectUrl = websiteUrlWithQuery
                )
            )
        } else if (TokenStateInternal.INT_CREATED.value == isolationToken.get().tokenStatus) {
            val newIsolationPeriodEndDate = request.isolationPeriodEndDate.epochSecond

            val updatedToken = isolationToken.get().copy(
                riskyEncounterDate = request.riskyEncounterDate.epochSecond,
                isolationPeriodEndDate = newIsolationPeriodEndDate,
                updatedTimestamp = systemClock().epochSecond,
                tokenStatus = TokenStateInternal.INT_UPDATED.value,
                expireAt = newIsolationPeriodEndDate
            )

            try {
                persistence.updateIsolationToken(updatedToken, TokenStateInternal.INT_CREATED)
                events(
                    UpdateIPCTokenSucceeded(
                        auditLogPrefix,
                        isolationToken.get(),
                        updatedToken,
                        websiteUrlWithQuery
                    )
                )
            } catch (e: ConditionalCheckFailedException) {
                events(
                    UpdateIPCTokenFailed(
                        auditLogPrefix,
                        ConditionalCheck,
                        request.ipcToken,
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
            events(
                UpdateIPCTokenFailed(
                    auditLogPrefix,
                    WrongState,
                    request.ipcToken,
                    isolationToken.orElse(null),
                    redirectUrl = websiteUrlWithQuery
                )
            )
        }
        return TokenUpdateResponse(websiteUrlWithQuery)
    }
}
