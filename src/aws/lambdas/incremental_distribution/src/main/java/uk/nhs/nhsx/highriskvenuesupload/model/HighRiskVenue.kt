package uk.nhs.nhsx.highriskvenuesupload.model

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_EMPTY)
data class HighRiskVenue(
    val id: VenueId,
    val riskyWindow: RiskyWindow,
    val messageType: MessageType,
    val optionalParameter: String? = null
) {
    init {
        if (!MESSAGE_TYPES_WITH_OPTIONAL_PARAMETER.contains(messageType) && !(optionalParameter.isNullOrEmpty())) {
            throw IllegalArgumentException("validation error: Message type $messageType does not support optional parameter")
        }

        if (MESSAGE_TYPES_WITH_OPTIONAL_PARAMETER.contains(messageType) && (optionalParameter.isNullOrEmpty())) {
            throw  IllegalArgumentException("validation error: Message type $messageType must include an optional parameter")
        }
    }

    companion object {
        private val MESSAGE_TYPES_WITH_OPTIONAL_PARAMETER = emptyList<MessageType>()
    }
}
