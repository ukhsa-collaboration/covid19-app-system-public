package uk.nhs.nhsx.highriskvenuesupload.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import uk.nhs.nhsx.domain.MessageType
import uk.nhs.nhsx.domain.OptionalHighRiskVenueParam
import uk.nhs.nhsx.domain.VenueId


@JsonInclude(NON_NULL)
data class HighRiskVenue(
    val id: VenueId,
    val riskyWindow: RiskyWindow,
    val messageType: MessageType,
    val optionalParameter: OptionalHighRiskVenueParam? = null
) {
    init {
        if (!MESSAGE_TYPES_WITH_OPTIONAL_PARAMETER.contains(messageType) && !(optionalParameter?.value.isNullOrEmpty())) {
            throw IllegalArgumentException("validation error: Message type $messageType does not support optional parameter")
        }

        if (MESSAGE_TYPES_WITH_OPTIONAL_PARAMETER.contains(messageType) && (optionalParameter?.value.isNullOrEmpty())) {
            throw  IllegalArgumentException("validation error: Message type $messageType must include an optional parameter")
        }
    }

    companion object {
        private val MESSAGE_TYPES_WITH_OPTIONAL_PARAMETER = emptyList<MessageType>()
    }
}
