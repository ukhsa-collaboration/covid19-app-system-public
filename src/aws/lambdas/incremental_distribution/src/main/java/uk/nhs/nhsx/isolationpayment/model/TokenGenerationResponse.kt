package uk.nhs.nhsx.isolationpayment.model

import com.fasterxml.jackson.annotation.JsonProperty
import uk.nhs.nhsx.virology.IpcTokenId

sealed class TokenGenerationResponse {
    data class Disabled(@field:JsonProperty("isEnabled") val isEnabled: Boolean = false) : TokenGenerationResponse()

    data class OK(
        val ipcToken: IpcTokenId,
        @field:JsonProperty("isEnabled") val isEnabled: Boolean = true
    ) : TokenGenerationResponse()
}
