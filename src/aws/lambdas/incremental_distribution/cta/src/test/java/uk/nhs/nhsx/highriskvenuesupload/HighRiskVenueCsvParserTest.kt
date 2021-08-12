package uk.nhs.nhsx.highriskvenuesupload

import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import uk.nhs.nhsx.core.AppServicesJson
import uk.nhs.nhsx.domain.MessageType
import uk.nhs.nhsx.domain.VenueId
import uk.nhs.nhsx.highriskvenuesupload.VenuesParsingResult.Failure
import uk.nhs.nhsx.highriskvenuesupload.VenuesParsingResult.Success
import uk.nhs.nhsx.highriskvenuesupload.model.HighRiskVenue
import uk.nhs.nhsx.highriskvenuesupload.model.HighRiskVenues
import uk.nhs.nhsx.highriskvenuesupload.model.RiskyWindow
import uk.nhs.nhsx.testhelper.assertions.isEqualToJson

class HighRiskVenueCsvParserTest {

    @Test
    fun `valid header with no rows`() {
        val result = HighRiskVenueCsvParser()
            .toJson("# venue_id, start_time, end_time, message_type, optional_parameter")

        expectThat(venuesFrom(result)).isEmpty()
    }

    @Test
    fun `valid csv with message type and optional parameter`() {
        val csv = """
            # venue_id, start_time, end_time, message_type, optional_parameter
            "CD2", "2019-07-04T13:33:03Z", "2019-07-14T23:33:03Z", "M1", ""
            "CD3", "2019-07-05T13:33:03Z", "2019-07-15T23:33:03Z", "M2", ""
            "CD4", "2019-07-06T13:33:03Z", "2019-07-16T23:33:03Z", "M1", ""
            """.trimIndent()

        val result = HighRiskVenueCsvParser().toJson(csv)

        expectThat(venuesFrom(result)).isEqualTo(
            listOf(
                HighRiskVenue(
                    VenueId.of("CD2"),
                    RiskyWindow.of("2019-07-04T13:33:03Z", "2019-07-14T23:33:03Z"),
                    MessageType.of("M1")
                ),
                HighRiskVenue(
                    VenueId.of("CD3"),
                    RiskyWindow.of("2019-07-05T13:33:03Z", "2019-07-15T23:33:03Z"),
                    MessageType.of("M2")
                ),
                HighRiskVenue(
                    VenueId.of("CD4"),
                    RiskyWindow.of("2019-07-06T13:33:03Z", "2019-07-16T23:33:03Z"),
                    MessageType.of("M1")
                )
            )
        )
    }

    @Test
    fun `valid csv with additional whitespace`() {
        val csv = """       # venue_id ,     start_time    ,    end_time , message_type,   optional_parameter
 "CD2" , "2019-07-04T13:33:03Z" ,  "2019-07-04T15:56:00Z", "M1", ""
"CD3" ,"2019-07-06T19:33:03Z" , "2019-07-06T21:01:07Z", "M2",  ""
   "CD4" , "2019-07-08T20:05:52Z" ,"2019-07-08T22:35:56Z","M1",   """""
        val result = HighRiskVenueCsvParser().toJson(csv)

        expectThat(venuesFrom(result)).isEqualTo(
            listOf(
                HighRiskVenue(
                    VenueId.of("CD2"),
                    RiskyWindow.of("2019-07-04T13:33:03Z", "2019-07-04T15:56:00Z"),
                    MessageType.of("M1")
                ),
                HighRiskVenue(
                    VenueId.of("CD3"),
                    RiskyWindow.of("2019-07-06T19:33:03Z", "2019-07-06T21:01:07Z"),
                    MessageType.of("M2")
                ),
                HighRiskVenue(
                    VenueId.of("CD4"),
                    RiskyWindow.of("2019-07-08T20:05:52Z", "2019-07-08T22:35:56Z"),
                    MessageType.of("M1")
                )
            )
        )
    }

    @Test
    fun `throws if csv with hyphen in venue`() {
        val csv = """
            # venue_id, start_time, end_time, message_type, optional_parameter
            "8CHARS-Y", "2019-07-04T13:33:03Z", "2019-07-04T23:33:03Z", "M1", ""
            """.trimIndent()

        val result = HighRiskVenueCsvParser().toJson(csv)

        expectThat(result)
            .isA<Failure>()
            .get(Failure::message)
            .contains("validation error: VenueId must match [CDEFHJKMPRTVWXY2345689]{1,12}")
    }

    @Test
    fun `throws if csv message type is invalid`() {
        val csv = """
            # venue_id, start_time, end_time, message_type, optional_parameter
            "CD2", "2019-07-04T13:33:03Z", "2019-07-04T23:33:03Z", "d1", ""
            """.trimIndent()

        val result = HighRiskVenueCsvParser().toJson(csv)

        expectThat(result)
            .isA<Failure>()
            .get(Failure::message)
            .contains("validation error: Message Type must match M[1-2]")
    }

    @Test
    fun `throws if csv message type has invalid optional parameter`() {
        val csv = """
            # venue_id, start_time, end_time, message_type, optional_parameter
            "CD2", "2019-07-04T13:33:03Z", "2019-07-04T23:33:03Z", "M1", "0712 34567"
            """.trimIndent()

        val result = HighRiskVenueCsvParser().toJson(csv)

        expectThat(result)
            .isA<Failure>()
            .get(Failure::message)
            .contains("validation error: Message type M1 does not support optional parameter")
    }

    @Test
    fun `valid csv to json unix line ending`() {
        val csv = """
            # venue_id, start_time, end_time, message_type, optional_parameter
            "CD2", "2019-07-04T13:33:03Z", "2019-07-04T15:56:00Z", "M1", ""
            "CD3", "2019-07-06T19:33:03Z", "2019-07-06T21:01:07Z", "M1", ""
            "CD4", "2019-07-08T20:05:52Z", "2019-07-08T22:35:56Z", "M1", ""
            """.trimIndent()

        val expectedJson = """
            {
              "venues": [
                {
                  "id": "CD2",
                  "riskyWindow": {
                    "from": "2019-07-04T13:33:03Z",
                    "until": "2019-07-04T15:56:00Z"
                  },
                  "messageType": "M1"
                },
                {
                  "id": "CD3",
                  "riskyWindow": {
                    "from": "2019-07-06T19:33:03Z",
                    "until": "2019-07-06T21:01:07Z"
                  },
                  "messageType": "M1"
                },
                {
                  "id": "CD4",
                  "riskyWindow": {
                    "from": "2019-07-08T20:05:52Z",
                    "until": "2019-07-08T22:35:56Z"
                  },
                  "messageType": "M1"
                }
              ]
            }
            """.trimIndent()

        val result = HighRiskVenueCsvParser().toJson(csv)

        expectThat(result)
            .isA<Success>()
            .get(Success::json)
            .isEqualToJson(expectedJson)
    }

    @Test
    fun `valid csv to json windows line ending`() {
        val csv = """
            # venue_id, start_time, end_time, message_type, optional_parameter
            "CD2", "2019-07-04T13:33:03Z", "2019-07-04T15:56:00Z", "M1", ""
            "CD3", "2019-07-06T19:33:03Z", "2019-07-06T21:01:07Z", "M1", ""
            "CD4", "2019-07-08T20:05:52Z", "2019-07-08T22:35:56Z", "M1", ""
            """.trimIndent()

        val expectedJson = """
            {
              "venues": [
                {
                  "id": "CD2",
                  "riskyWindow": {
                    "from": "2019-07-04T13:33:03Z",
                    "until": "2019-07-04T15:56:00Z"
                  },
                  "messageType": "M1"
                },
                {
                  "id": "CD3",
                  "riskyWindow": {
                    "from": "2019-07-06T19:33:03Z",
                    "until": "2019-07-06T21:01:07Z"
                  },
                  "messageType": "M1"
                },
                {
                  "id": "CD4",
                  "riskyWindow": {
                    "from": "2019-07-08T20:05:52Z",
                    "until": "2019-07-08T22:35:56Z"
                  },
                  "messageType": "M1"
                }
              ]
            }
            """.trimIndent()

        val result = HighRiskVenueCsvParser().toJson(csv)

        expectThat(result)
            .isA<Success>()
            .get(Success::json)
            .isEqualToJson(expectedJson)
    }

    @Test
    fun `throws if empty csv`() {
        val result = HighRiskVenueCsvParser().toJson("")

        expectThat(result)
            .isA<Failure>()
            .get(Failure::message)
            .contains("validation error: No payload")
    }

    @Test
    fun `throws if whitespaces csv`() {
        val result = HighRiskVenueCsvParser().toJson("     ")

        expectThat(result)
            .isA<Failure>()
            .get(Failure::message).contains("validation error: No payload")
    }

    @Test
    fun `throws if csv too many cols`() {
        val csv = """
            # venue_id, start_time, end_time, vibe
            "ID1", "2019-07-04T13:33:03Z", "2019-07-04T15:56:00Z", L
            "ID2", "2019-07-06T19:33:03Z", "2019-07-04T21:01:07Z", M
            "ID3", "2019-07-08T20:05:52Z", "2019-07-04T22:35:56Z", H
            """.trimIndent()
        val result = HighRiskVenueCsvParser().toJson(csv)

        expectThat(result)
            .isA<Failure>()
            .get(Failure::message)
            .isEqualTo("validation error: Invalid header. Expected [venue_id, start_time, end_time, message_type, optional_parameter]")
    }

    @Test
    fun `throws if invalid header`() {
        val csv = """
            # venue_id, left, end_time
            "ID1", "2019-07-04T13:33:03Z", "2019-07-04T15:56:00Z"
            "ID2", "2019-07-06T19:33:03Z", "2019-07-04T21:01:07Z"
            "ID3", "2019-07-08T20:05:52Z", "2019-07-04T22:35:56Z"
            """.trimIndent()

        val result = HighRiskVenueCsvParser().toJson(csv)

        expectThat(result)
            .isA<Failure>()
            .get(Failure::message)
            .contains("validation error: Invalid header. Expected [venue_id, start_time, end_time, message_type, optional_parameter]")
    }

    @Test
    fun `throws if empty line`() {
        val csv = """
            # venue_id, start_time, end_time, message_type, optional_parameter
            "CD2", "2019-07-04T13:33:03Z", "2019-07-14T23:33:03Z", "M1", ""
            "CD3", "2019-07-05T13:33:03Z", "2019-07-15T23:33:03Z", "M2", ""
            
            "CD4", "2019-07-06T13:33:03Z", "2019-07-16T23:33:03Z", "M1", ""
            """.trimIndent()
        val result = HighRiskVenueCsvParser().toJson(csv)

        expectThat(result)
            .isA<Failure>()
            .get(Failure::message)
            .contains("validation error: Invalid data in row 4")
    }

    @Test
    fun `throws if different invalid row`() {
        val csv = """
            # venue_id, start_time, end_time, message_type, optional_parameter
            "CD2", "2019-07-04T13:33:03Z", "2019-07-04T15:56:00Z", "M1", ""
            "CD3", "2019-07-06T19:33:03Z", "2019-07-06T21:01:07Z", "M1", ""
            "CD4", "2019-07-08T20:05:52Z", "2019-07-08T22:35:56Z", "M1", "", H
            """.trimIndent()

        val result = HighRiskVenueCsvParser().toJson(csv)

        expectThat(result)
            .isA<Failure>()
            .get(Failure::message)
            .contains("validation error: Invalid data in row 3")
    }

    @Test
    fun `throws if invalid row`() {
        val csv = """
            # venue_id, start_time, end_time, message_type, optional_parameter
            "CD2", "2019-07-04T13:33:03Z", "2019-07-04T15:56:00Z", "M1", ""
            "CD3", "2019-07-06T19:33:03Z", "2019-07-06T21:01:07Z", "M1", ""
            "CD4", "2019-07-08T20:05:52Z"
            """.trimIndent()

        val result = HighRiskVenueCsvParser().toJson(csv)

        expectThat(result)
            .isA<Failure>()
            .get(Failure::message)
            .contains("validation error: Invalid data in row 3")
    }

    @Test
    fun `throws if invalid start date`() {
        val csv = """
            # venue_id, start_time, end_time, message_type, optional_parameter
            "CD2", "2019-07-04T13:33:03Z", "2019-07-04T15:56:00Z", "M1", ""
            "CD3", "2019-07-08T20:05F:52Z", "2019-07-04T22:35:56Z", "M1", ""
            """.trimIndent()

        val result = HighRiskVenueCsvParser().toJson(csv)

        expectThat(result)
            .isA<Failure>()
            .get(Failure::message)
            .isEqualTo("validation error: Date did not conform to expected format yyyy-MM-dd'T'HH:mm:ss'Z'")
    }

    @Test
    fun `throws if invalid end date`() {
        val csv = """
            # venue_id, start_time, end_time, message_type, optional_parameter
            "CD2", "2019-07-04T13:33:03Z", "2019-07-04T005:56:00Z", "M1", ""
            "CD3", "2019-07-08T20:05:52Z", "2019-07-04T22:35:56Z", "M1", ""
            """.trimIndent()

        val result = HighRiskVenueCsvParser().toJson(csv)

        expectThat(result)
            .isA<Failure>()
            .get(Failure::message)
            .isEqualTo("validation error: Date did not conform to expected format yyyy-MM-dd'T'HH:mm:ss'Z'")
    }

    @Test
    fun `throws if start date after end date`() {
        val csv = """
            # venue_id, start_time, end_time, message_type, optional_parameter
            "CD2", "2022-07-04T13:33:03Z", "2019-07-04T23:33:03Z", "M1", ""
            """.trimIndent()

        val result = HighRiskVenueCsvParser().toJson(csv)

        expectThat(result)
            .isA<Failure>()
            .get(Failure::message)
            .isEqualTo("validation error: Start date must be <= end date")
    }

    @Test
    fun `row without venue id throws`() {
        val csv = """
            # venue_id, start_time, end_time, message_type, optional_parameter
            , "2019-07-04T15:56:00Z", "2019-07-04T15:56:00Z", "M1", ""
            """.trimIndent()

        val result = HighRiskVenueCsvParser().toJson(csv)

        expectThat(result)
            .isA<Failure>()
            .get(Failure::message)
            .contains("validation error: VenueId must match [CDEFHJKMPRTVWXY2345689]{1,12}")
    }

    private fun venuesFrom(result: VenuesParsingResult): List<HighRiskVenue> = when (result) {
        is Failure -> fail("Result is a failure")
        is Success -> AppServicesJson.mapper.copy()
            .configure(FAIL_ON_MISSING_CREATOR_PROPERTIES, false)
            .configure(FAIL_ON_NULL_CREATOR_PROPERTIES, false)
            .readValue(result.json, HighRiskVenues::class.java).venues
    }

    @Test
    fun `throws if venue id has more characters`() {
        val csv = """
            # venue_id, start_time, end_time, message_type, optional_parameter
            "CDEFH23456789", "2019-07-04T13:33:03Z", "2019-07-04T15:56:00Z", "M1", ""
            """.trimIndent()

        val result = HighRiskVenueCsvParser().toJson(csv)

        expectThat(result)
            .isA<Failure>()
            .get(Failure::message)
            .contains("validation error: VenueId must match [CDEFHJKMPRTVWXY2345689]{1,12}")
    }

    @Test
    fun `throws if venue id has other characters`() {
        val csv = """
            # venue_id, start_time, end_time, message_type, optional_parameter
            "ID123456789", "2019-07-04T13:33:03Z", "2019-07-04T15:56:00Z", "M1", ""
            """.trimIndent()

        val result = HighRiskVenueCsvParser().toJson(csv)

        expectThat(result)
            .isA<Failure>()
            .get(Failure::message)
            .contains("validation error: VenueId must match [CDEFHJKMPRTVWXY2345689]{1,12}")
    }

    @Test
    fun `throws if message type is not m 1 or m2`() {
        val csv = """
            # venue_id, start_time, end_time, message_type, optional_parameter
            "CD2", "2019-07-04T13:33:03Z", "2019-07-14T23:33:03Z", "M1", ""
            "CD3", "2019-07-05T13:33:03Z", "2019-07-15T23:33:03Z", "M2", ""
            "CD4", "2019-07-06T13:33:03Z", "2019-07-16T23:33:03Z", "M3", "07911 123456"
            """.trimIndent()

        val result = HighRiskVenueCsvParser().toJson(csv)

        expectThat(result)
            .isA<Failure>()
            .get(Failure::message)
            .contains("validation error: Message Type must match M[1-2]")
    }
}
