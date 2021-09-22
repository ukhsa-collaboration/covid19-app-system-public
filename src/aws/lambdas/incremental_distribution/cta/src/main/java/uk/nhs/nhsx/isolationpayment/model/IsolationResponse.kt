package uk.nhs.nhsx.isolationpayment.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import uk.nhs.nhsx.domain.IpcTokenId
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

    companion object {
        fun of(
            tokenId: IpcTokenId,
            state: TokenStateExternal
        ) = IsolationResponse(tokenId, state.value)

        fun of(
            tokenId: IpcTokenId,
            state: TokenStateExternal,
            isolationToken: IsolationToken
        ) = with(isolationToken) {
            IsolationResponse(
                ipcToken = tokenId,
                state = state.value,
                riskyEncounterDate = riskyEncounterDate?.let(Instant::ofEpochSecond),
                isolationPeriodEndDate = isolationPeriodEndDate?.let(Instant::ofEpochSecond),
                createdTimestamp = Instant.ofEpochSecond(createdTimestamp),
                updatedTimestamp = updatedTimestamp?.let(Instant::ofEpochSecond),
            )
        }
    }
}
