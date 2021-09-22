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
import uk.nhs.nhsx.isolationpayment.model.TokenStateInternal.INT_CREATED
import uk.nhs.nhsx.isolationpayment.model.TokenUpdateRequest
import uk.nhs.nhsx.isolationpayment.model.TokenUpdateResponse
import uk.nhs.nhsx.isolationpayment.model.isStateEqual
import java.time.Period

class IsolationPaymentMobileService(
    private val systemClock: Clock,
    private val tokenGenerator: IpcTokenIdGenerator,
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
            tokenId = tokenGenerator.nextToken(),
            tokenStatus = INT_CREATED.value,
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
        val isolationToken = persistence.getIsolationToken(request.ipcToken)

        when {
            // API contract: we don't report this back to the client
            isolationToken == null -> events(
                UpdateIPCTokenFailed(
                    auditPrefix = auditLogPrefix,
                    reason = NotFound,
                    tokenId = request.ipcToken,
                    redirectUrl = websiteUrlWithQuery
                )
            )

            isolationToken.isStateEqual(INT_CREATED) -> {
                val newIsolationPeriodEndDate = request.isolationPeriodEndDate.epochSecond

                val updatedToken = isolationToken.copy(
                    riskyEncounterDate = request.riskyEncounterDate.epochSecond,
                    isolationPeriodEndDate = newIsolationPeriodEndDate,
                    updatedTimestamp = systemClock().epochSecond,
                    tokenStatus = TokenStateInternal.INT_UPDATED.value,
                    expireAt = newIsolationPeriodEndDate
                )

                try {
                    persistence.updateIsolationToken(updatedToken, INT_CREATED)
                    events(UpdateIPCTokenSucceeded(auditLogPrefix, isolationToken, updatedToken, websiteUrlWithQuery))
                } catch (e: ConditionalCheckFailedException) {
                    events(
                        UpdateIPCTokenFailed(
                            auditPrefix = auditLogPrefix,
                            reason = ConditionalCheck,
                            tokenId = request.ipcToken,
                            isolationToken = isolationToken,
                            updatedToken = updatedToken,
                            redirectUrl = websiteUrlWithQuery,
                            exception = e
                        )
                    )
                } catch (e: Exception) {
                    throw RuntimeException(
                        "$auditLogPrefix UpdateToken exception: ipcToken=${isolationToken}, !updated.ipcToken=$updatedToken",
                        e
                    )
                }
            }

            // API contract: we don't report this back to the client
            else -> events(
                UpdateIPCTokenFailed(
                    auditPrefix = auditLogPrefix,
                    reason = WrongState,
                    tokenId = request.ipcToken,
                    isolationToken = isolationToken,
                    redirectUrl = websiteUrlWithQuery
                )
            )
        }

        return TokenUpdateResponse(websiteUrlWithQuery)
    }
}
