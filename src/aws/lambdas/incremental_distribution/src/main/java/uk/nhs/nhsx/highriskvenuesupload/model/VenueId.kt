package uk.nhs.nhsx.highriskvenuesupload.model

import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import dev.forkhandles.values.regex

private const val VENUE_ID_PATTERN = "[CDEFHJKMPRTVWXY2345689]{1,12}"

class VenueId(id: String) : StringValue(id) {
    companion object : StringValueFactory<VenueId>(
        ::VenueId,
        VENUE_ID_PATTERN.regex.withMessage("validation error: VenueId must match $VENUE_ID_PATTERN")
    )
}
