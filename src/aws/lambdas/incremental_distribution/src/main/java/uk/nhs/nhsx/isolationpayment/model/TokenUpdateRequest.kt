package uk.nhs.nhsx.isolationpayment.model

import uk.nhs.nhsx.virology.IpcTokenId
import java.time.Instant

data class TokenUpdateRequest(
    val ipcToken: IpcTokenId,
    val riskyEncounterDate: Instant,
    val isolationPeriodEndDate: Instant
)
