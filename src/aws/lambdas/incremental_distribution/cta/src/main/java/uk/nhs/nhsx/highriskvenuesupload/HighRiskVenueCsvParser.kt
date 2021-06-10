package uk.nhs.nhsx.highriskvenuesupload

import org.apache.commons.csv.CSVFormat
import uk.nhs.nhsx.core.Json.toJson
import uk.nhs.nhsx.domain.MessageType
import uk.nhs.nhsx.domain.OptionalHighRiskVenueParam
import uk.nhs.nhsx.domain.VenueId
import uk.nhs.nhsx.highriskvenuesupload.model.HighRiskVenue
import uk.nhs.nhsx.highriskvenuesupload.model.HighRiskVenues
import uk.nhs.nhsx.highriskvenuesupload.model.RiskyWindow
import java.io.StringReader

class HighRiskVenueCsvParser {
    private object Header {
        const val VenueId = "venue_id"
        const val StartTime = "start_time"
        const val EndTime = "end_time"
        const val MessageType = "message_type"
        const val OptionalParameter = "optional_parameter"
        val headerNames = listOf(VenueId, StartTime, EndTime, MessageType, OptionalParameter)
    }

    fun toJson(csv: String): VenuesParsingResult = try {
        VenuesParsingResult.ok(toJson(parse(csv)))
    } catch (e: Exception) {
        VenuesParsingResult.failure(e.message ?: "Something went wrong")
    }

    private fun parse(csv: String): HighRiskVenues {
        if (csv.isBlank()) {
            throw VenuesParsingException("No payload")
        }

        if (csv.toByteArray().size > CSV_CONTENT_MAX_SIZE) {
            throw VenuesParsingException("Csv content is more than 1MB")
        }

        val venues = CSVFormat.RFC4180
            .withFirstRecordAsHeader()
            .withIgnoreHeaderCase()
            .withIgnoreSurroundingSpaces()
            .parse(StringReader(csv.stripHeaderComment()))
            .use { p ->
                if (Header.headerNames != p.headerNames) {
                    throw VenuesParsingException("Invalid header. Expected ${Header.headerNames}")
                }

                p.records.map {
                    if (!it.isConsistent) {
                        throw VenuesParsingException("Invalid data in row ${it.parser.recordNumber}")
                    }

                    val venueId = it.get(Header.VenueId)
                    val startTime = it.get(Header.StartTime)
                    val endTime = it.get(Header.EndTime)
                    val messageType = it.get(Header.MessageType)
                    val parameter = it.get(Header.OptionalParameter)?.takeIf(String::isNotBlank)
                        ?.let(OptionalHighRiskVenueParam::of)

                    HighRiskVenue(
                        VenueId.of(venueId),
                        RiskyWindow.of(startTime, endTime),
                        MessageType.of(messageType),
                        parameter
                    )
                }
            }

        return HighRiskVenues(venues)
    }

    private fun String.stripHeaderComment(): String =
        if (trimStart().startsWith("#")) substringAfter("#") else this

    private class VenuesParsingException(
        message: String,
        prefix: String = "validation error"
    ) : RuntimeException("$prefix: $message")

    companion object {
        private const val CSV_CONTENT_MAX_SIZE = 1048576
    }
}
