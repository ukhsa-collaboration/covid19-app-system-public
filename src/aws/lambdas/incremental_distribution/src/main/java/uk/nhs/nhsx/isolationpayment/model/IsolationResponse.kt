package uk.nhs.nhsx.isolationpayment.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import uk.nhs.nhsx.virology.IpcTokenId
import java.time.Instant

@JsonInclude(NON_NULL)
data class IsolationResponse(
    val ipcToken: IpcTokenId,
    val state: String,
    val riskyEncounterDate: Instant? = null,
    val isolationPeriodEndDate: Instant? = null,
    val createdTimestamp: Instant? = null,
    val updatedTimestamp: Instant? = null
) {
    val contractVersion = 1
}
