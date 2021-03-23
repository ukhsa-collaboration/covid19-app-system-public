package uk.nhs.nhsx.core.events

import uk.nhs.nhsx.core.events.EventCategory.Audit
import uk.nhs.nhsx.isolationpayment.model.IsolationToken
import uk.nhs.nhsx.virology.Country
import uk.nhs.nhsx.virology.IpcTokenId

data class CreateIPCTokenSucceeded(
    val auditPrefix: String,
    val isolationToken: IsolationToken
): Event(Audit)

data class CreateIPCTokenNotEnabled(
    val auditPrefix: String,
    val country: Country
) : Event(Audit)

data class CreateIPCTokenFailed(
    val auditPrefix: String,
    val isolationToken: IsolationToken,
    val exception: Exception? = null
) : Event(Audit)

data class UpdateIPCTokenSucceeded(
    val auditPrefix: String,
    val isolationToken: IsolationToken,
    val updatedToken: IsolationToken,
    val redirectUrl: String
) : Event(Audit)

enum class TokenFailureReason {
    WrongState, NotFound, ConditionalCheck
}

data class UpdateIPCTokenFailed(
    val auditPrefix: String,
    val reason: TokenFailureReason,
    val tokenId: IpcTokenId,
    val isolationToken: IsolationToken? = null,
    val updatedToken: IsolationToken? = null,
    val redirectUrl: String? = null,
    val exception: Exception? = null
) : Event(Audit)

data class VerifyIsolationTokenFailed(
    val auditPrefix: String,
    val reason: TokenFailureReason,
    val tokenId: IpcTokenId,
    val isolationToken: IsolationToken? = null
) : Event(Audit)

data class UpdateIsolationTokenSucceeded(
    val auditPrefix: String,
    val isolationToken: IsolationToken,
    val updatedToken: IsolationToken
) : Event(Audit)

data class ConsumeIsolationTokenSucceeded(
    val auditPrefix: String,
    val tokenId: IpcTokenId,
    val isolationToken: IsolationToken
) : Event(Audit)

data class ConsumeIsolationTokenFailed(
    val auditPrefix: String,
    val reason: TokenFailureReason,
    val tokenId: IpcTokenId,
    val isolationToken: IsolationToken? = null
) : Event(Audit)
