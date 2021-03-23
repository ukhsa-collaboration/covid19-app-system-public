package uk.nhs.nhsx.isolationpayment.model

import uk.nhs.nhsx.virology.IpcTokenId

data class IsolationToken(
    val tokenId: IpcTokenId,
    val tokenStatus: String,
    val riskyEncounterDate: Long? = null,
    val isolationPeriodEndDate: Long? = null,
    val createdTimestamp: Long,
    val updatedTimestamp: Long? = null,
    val validatedTimestamp: Long? = null,
    val consumedTimestamp: Long? = null,
    val expireAt: Long
)
