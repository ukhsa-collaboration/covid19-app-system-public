package uk.nhs.nhsx.highriskvenuesupload.model

import java.time.Instant
import java.time.format.DateTimeParseException

data class RiskyWindow(val from: Instant, val until: Instant) {

    init {
        require(from.isBefore(until)) {
            "validation error: Start date must be <= end date"
        }
    }

    companion object {
        fun of(from: String, until: String): RiskyWindow = try {
            RiskyWindow(Instant.parse(from), Instant.parse(until))
        } catch (e: DateTimeParseException) {
            throw IllegalArgumentException("validation error: Date did not conform to expected format yyyy-MM-dd'T'HH:mm:ss'Z'")
        }
    }
}
