package uk.nhs.nhsx.highriskvenuesupload

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import uk.nhs.nhsx.highriskvenuesupload.model.HighRiskVenue
import uk.nhs.nhsx.highriskvenuesupload.model.HighRiskVenues
import uk.nhs.nhsx.highriskvenuesupload.model.RiskyWindow
import java.util.*

class RiskyVenuesCsvParserTest {

    @Test
    fun validHeaderWithNoRows() {
        val result = HighRiskVenueCsvParser()
            .toJson("# venue_id, start_time, end_time")
        assertEquals(emptyList<Any>(), venuesFrom(result))
    }

    @Test
    fun validCsv() {
        val csv = """
            # venue_id, start_time, end_time
            "CD2", "2019-07-04T13:33:03Z", "2019-07-04T15:56:00Z"
            "CD3", "2019-07-06T19:33:03Z", "2019-07-06T21:01:07Z"
            "CD4", "2019-07-08T20:05:52Z", "2019-07-08T22:35:56Z"
            """.trimIndent()
        val result = HighRiskVenueCsvParser()
            .toJson(csv)
        val venue1 = HighRiskVenue("CD2",
            RiskyWindow("2019-07-04T13:33:03Z", "2019-07-04T15:56:00Z"))
        val venue2 = HighRiskVenue("CD3",
            RiskyWindow("2019-07-06T19:33:03Z", "2019-07-06T21:01:07Z"))
        val venue3 = HighRiskVenue("CD4",
            RiskyWindow("2019-07-08T20:05:52Z", "2019-07-08T22:35:56Z"))
        val venues = listOf(venue1, venue2, venue3)
        assertEquals(venues, venuesFrom(result))
    }

    @Test
    fun validCsvWithMessageTypeAndOptionalParameter() {
        val csv = """
            # venue_id, start_time, end_time, message_type, optional_parameter
            "CD2", "2019-07-04T13:33:03Z", "2019-07-14T23:33:03Z", "M1", ""
            "CD3", "2019-07-05T13:33:03Z", "2019-07-15T23:33:03Z", "M2", ""
            "CD4", "2019-07-06T13:33:03Z", "2019-07-16T23:33:03Z", "M3", "07911 123456"
            """.trimIndent()
        val result = HighRiskVenueCsvParser(true)
            .toJson(csv)
        val venue1 = HighRiskVenue("CD2",
            RiskyWindow("2019-07-04T13:33:03Z", "2019-07-14T23:33:03Z"), "M1", null)
        val venue2 = HighRiskVenue("CD3",
            RiskyWindow("2019-07-05T13:33:03Z", "2019-07-15T23:33:03Z"), "M2", null)
        val venue3 = HighRiskVenue("CD4",
            RiskyWindow("2019-07-06T13:33:03Z", "2019-07-16T23:33:03Z"), "M3", "07911 123456")
        val venues = listOf(venue1, venue2, venue3)
        assertEquals(venues, venuesFrom(result))
    }

    @Test
    fun validCsvWithAdditionalWhitespace() {
        val csv = """       # venue_id ,     start_time    ,    end_time 
 "CD2" , "2019-07-04T13:33:03Z" ,  "2019-07-04T15:56:00Z"
"CD3" ,"2019-07-06T19:33:03Z" , "2019-07-06T21:01:07Z"
   "CD4" , "2019-07-08T20:05:52Z" ,"2019-07-08T22:35:56Z""""
        val result = HighRiskVenueCsvParser()
            .toJson(csv)
        val venue1 = HighRiskVenue("CD2",
            RiskyWindow("2019-07-04T13:33:03Z", "2019-07-04T15:56:00Z"))
        val venue2 = HighRiskVenue("CD3",
            RiskyWindow("2019-07-06T19:33:03Z", "2019-07-06T21:01:07Z"))
        val venue3 = HighRiskVenue("CD4",
            RiskyWindow("2019-07-08T20:05:52Z", "2019-07-08T22:35:56Z"))
        val venues = listOf(venue1, venue2, venue3)
        assertEquals(venues, venuesFrom(result))
    }

    @Test
    fun throwsIfCsvWithHyphenInVenue() {
        val csv = """
            # venue_id, start_time, end_time 
            "8CHARS-Y", "2019-07-04T13:33:03Z", "2019-07-04T23:33:03Z"
            """.trimIndent()
        val result = HighRiskVenueCsvParser()
            .toJson(csv)
        assertThat(result.failureMaybe())
            .isEqualTo(Optional.of("validation error: Invalid characters on the venueId: 8CHARS-Y"))
    }

    @Test
    fun throwsIfCsvMessageTypeIsInvalid() {
        val csv = """
            # venue_id, start_time, end_time, message_type, optional_parameter
            "CD2", "2019-07-04T13:33:03Z", "2019-07-04T23:33:03Z", "d1", ""
            """.trimIndent()
        val result = HighRiskVenueCsvParser(true)
            .toJson(csv)
        assertThat(result.failureMaybe())
            .isEqualTo(Optional.of("validation error: Invalid characters on the messageType: d1"))
    }

    @Test
    fun throwsIfCsvMessageTypeHasInvalidOptionalParameter() {
        val csv = """
            # venue_id, start_time, end_time, message_type, optional_parameter
            "CD2", "2019-07-04T13:33:03Z", "2019-07-04T23:33:03Z", "M1", "0712 34567"
            """.trimIndent()
        val result = HighRiskVenueCsvParser(true)
            .toJson(csv)
        assertThat(result.failureMaybe())
            .isEqualTo(Optional.of("validation error: Message type M1 does not support optional parameter"))
    }

    @Test
    fun throwsIfCsvMessageTypeHasNoOptionalParameter() {
        val csv = """
            # venue_id, start_time, end_time, message_type, optional_parameter
            "CD2", "2019-07-04T13:33:03Z", "2019-07-04T23:33:03Z", "M3", ""
            """.trimIndent()
        val result = HighRiskVenueCsvParser(true)
            .toJson(csv)
        assertThat(result.failureMaybe())
            .isEqualTo(Optional.of("validation error: Message type M3 must include an optional parameter"))
    }

    @Test
    @Throws(Exception::class)
    fun validCsvToJsonUnixLineEnding() {
        val csv = """
            # venue_id, start_time, end_time
            "CD2", "2019-07-04T13:33:03Z", "2019-07-04T15:56:00Z"
            "CD3", "2019-07-06T19:33:03Z", "2019-07-06T21:01:07Z"
            "CD4", "2019-07-08T20:05:52Z", "2019-07-08T22:35:56Z"
            """.trimIndent()
        val expectedJson = """{
    "venues" : [
        {
            "id": "CD2",
            "riskyWindow": {
              "from": "2019-07-04T13:33:03Z",
              "until": "2019-07-04T15:56:00Z"
            },
           "messageType": "M1"
        },{
            "id": "CD3",
            "riskyWindow": {
              "from": "2019-07-06T19:33:03Z",
              "until": "2019-07-06T21:01:07Z"
            },
           "messageType": "M1"
        },{
            "id": "CD4",
            "riskyWindow": {
              "from": "2019-07-08T20:05:52Z",
              "until": "2019-07-08T22:35:56Z"
            },
           "messageType": "M1"
        }

    ]
}"""
        val result = HighRiskVenueCsvParser().toJson(csv)
        JSONAssert.assertEquals(expectedJson, result.jsonOrThrow(), JSONCompareMode.STRICT)
    }

    @Test
    @Throws(Exception::class)
    fun validCsvToJsonWindowsLineEnding() {
        val csv = """
            # venue_id, start_time, end_time
            "CD2", "2019-07-04T13:33:03Z", "2019-07-04T15:56:00Z"
            "CD3", "2019-07-06T19:33:03Z", "2019-07-06T21:01:07Z"
            "CD4", "2019-07-08T20:05:52Z", "2019-07-08T22:35:56Z"
            """.trimIndent()
        val expectedJson = """{
    "venues" : [
        {
            "id": "CD2",
            "riskyWindow": {
              "from": "2019-07-04T13:33:03Z",
              "until": "2019-07-04T15:56:00Z"
            },
           "messageType": "M1"
        },{
            "id": "CD3",
            "riskyWindow": {
              "from": "2019-07-06T19:33:03Z",
              "until": "2019-07-06T21:01:07Z"
            },
           "messageType": "M1"
        },{
            "id": "CD4",
            "riskyWindow": {
              "from": "2019-07-08T20:05:52Z",
              "until": "2019-07-08T22:35:56Z"
            },
           "messageType": "M1"
        }

    ]
}"""
        val result = HighRiskVenueCsvParser().toJson(csv)
        JSONAssert.assertEquals(expectedJson, result.jsonOrThrow(), JSONCompareMode.STRICT)
    }

    @Test
    fun throwsIfNullCsv() {
        val result = HighRiskVenueCsvParser().toJson(null)
        assertThat(result.failureMaybe())
            .isEqualTo(Optional.of("validation error: No payload"))
    }

    @Test
    fun throwsIfEmptyCsv() {
        val result = HighRiskVenueCsvParser().toJson("")
        assertThat(result.failureMaybe())
            .isEqualTo(Optional.of("validation error: No payload"))
    }

    @Test
    fun throwsIfWhitespacesCsv() {
        val result = HighRiskVenueCsvParser().toJson("     ")
        assertThat(result.failureMaybe())
            .isEqualTo(Optional.of("validation error: No payload"))
    }

    @Test
    fun throwsIfCsvTooManyCols() {
        val csv = """
            # venue_id, start_time, end_time, vibe
            "ID1", "2019-07-04T13:33:03Z", "2019-07-04T15:56:00Z", L
            "ID2", "2019-07-06T19:33:03Z", "2019-07-04T21:01:07Z", M
            "ID3", "2019-07-08T20:05:52Z", "2019-07-04T22:35:56Z", H
            """.trimIndent()
        val result = HighRiskVenueCsvParser().toJson(csv)
        assertThat(result.failureMaybe())
            .isEqualTo(Optional.of("validation error: Invalid header"))
    }

    @Test
    fun throwsIfInvalidHeader() {
        val csv = """
            # venue_id, left, end_time
            "ID1", "2019-07-04T13:33:03Z", "2019-07-04T15:56:00Z"
            "ID2", "2019-07-06T19:33:03Z", "2019-07-04T21:01:07Z"
            "ID3", "2019-07-08T20:05:52Z", "2019-07-04T22:35:56Z"
            """.trimIndent()
        val result = HighRiskVenueCsvParser().toJson(csv)
        assertThat(result.failureMaybe())
            .isEqualTo(Optional.of("validation error: Invalid header"))
    }

    @Test
    fun throwsIfDifferentInvalidRow() {
        val csv = """
            # venue_id, start_time, end_time
            "CD2", "2019-07-04T13:33:03Z", "2019-07-04T15:56:00Z"
            "CD3", "2019-07-06T19:33:03Z", "2019-07-06T21:01:07Z"
            "CD4", "2019-07-08T20:05:52Z", "2019-07-08T22:35:56Z", H
            """.trimIndent()
        val result = HighRiskVenueCsvParser().toJson(csv)
        assertThat(result.failureMaybe())
            .isEqualTo(Optional.of("validation error: Invalid data row on line number: 4"))
    }

    @Test
    fun throwsIfInvalidRow() {
        val csv = """
            # venue_id, start_time, end_time
            "CD2", "2019-07-04T13:33:03Z", "2019-07-04T15:56:00Z"
            "CD3", "2019-07-06T19:33:03Z", "2019-07-06T21:01:07Z"
            "CD4", "2019-07-08T20:05:52Z"
            """.trimIndent()
        val result = HighRiskVenueCsvParser().toJson(csv)
        assertThat(result.failureMaybe())
            .isEqualTo(Optional.of("validation error: Invalid data row on line number: 4"))
    }

    @Test
    fun throwsIfInvalidStartDate() {
        val csv = """
            # venue_id, start_time, end_time
            "CD2", "2019-07-04T13:33:03Z", "2019-07-04T15:56:00Z"
            "CD3", "2019-07-08T20:05F:52Z", "2019-07-04T22:35:56Z"
            """.trimIndent()
        val result = HighRiskVenueCsvParser().toJson(csv)
        assertThat(result.failureMaybe())
            .isEqualTo(Optional.of(
                "validation error: Date 2019-07-08T20:05F:52Z did " +
                    "not conform to expected format yyyy-MM-dd'T'HH:mm:ss'Z'"
            ))
    }

    @Test
    fun throwsIfInvalidEndDate() {
        val csv = """
            # venue_id, start_time, end_time
            "ID1", "2019-07-04T13:33:03Z", "2019-07-04T005:56:00Z"
            "ID3", "2019-07-08T20:05:52Z", "2019-07-04T22:35:56Z"
            """.trimIndent()
        val result = HighRiskVenueCsvParser().toJson(csv)
        assertThat(result.failureMaybe())
            .isEqualTo(Optional.of(
                "validation error: Date 2019-07-04T005:56:00Z " +
                    "did not conform to expected format yyyy-MM-dd'T'HH:mm:ss'Z'"
            ))
    }

    @Test
    fun throwsIfStartDateAfterEndDate() {
        val csv = """
            # venue_id, start_time, end_time
            "ID1", "2022-07-04T13:33:03Z", "2019-07-04T23:33:03Z"
            """.trimIndent()
        val result = HighRiskVenueCsvParser().toJson(csv)
        assertThat(result.failureMaybe())
            .isEqualTo(Optional.of("validation error: Start date must be <= end date"))
    }

    @Test
    fun rowWithoutVenueIdThrows() {
        val csv = """
            # venue_id, start_time, end_time
            , "2019-07-04T15:56:00Z", "2019-07-04T15:56:00Z"
            """.trimIndent()
        val result = HighRiskVenueCsvParser().toJson(csv)
        assertThat(result.failureMaybe())
            .isEqualTo(Optional.of("validation error: Invalid data row on line number: 2"))
    }

    private fun venuesFrom(result: VenuesParsingResult): List<HighRiskVenue> {
        val riskyVenueMapper = ObjectMapper()
            .deactivateDefaultTyping()
            .registerModule(ParameterNamesModule(JsonCreator.Mode.PROPERTIES))
            .registerModule(Jdk8Module())
        return riskyVenueMapper.readValue(result.jsonOrThrow(), HighRiskVenues::class.java).venues
    }

    @Test
    fun throwsIfVenueIdHasMoreCharacters() {
        val csv = """
            # venue_id, start_time, end_time
            "CDEFH23456789", "2019-07-04T13:33:03Z", "2019-07-04T15:56:00Z"
            """.trimIndent()
        val result = HighRiskVenueCsvParser().toJson(csv)
        assertThat(result.failureMaybe())
            .isEqualTo(Optional.of("validation error: Length of VenueId is greater than 12 characters"))
    }

    @Test
    fun throwsIfVenueIdHasOtherCharacters() {
        val csv = """
            # venue_id, start_time, end_time
            "ID123456789", "2019-07-04T13:33:03Z", "2019-07-04T15:56:00Z"
            """.trimIndent()
        val result = HighRiskVenueCsvParser().toJson(csv)
        assertThat(result.failureMaybe())
            .isEqualTo(Optional.of("validation error: Invalid characters on the venueId: ID123456789"))
    }
}