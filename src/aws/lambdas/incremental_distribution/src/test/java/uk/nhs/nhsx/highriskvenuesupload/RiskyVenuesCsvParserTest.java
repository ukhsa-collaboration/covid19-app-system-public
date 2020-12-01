package uk.nhs.nhsx.highriskvenuesupload;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import uk.nhs.nhsx.highriskvenuesupload.model.HighRiskVenue;
import uk.nhs.nhsx.highriskvenuesupload.model.HighRiskVenues;
import uk.nhs.nhsx.highriskvenuesupload.model.RiskyWindow;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class RiskyVenuesCsvParserTest {

    @Test
    public void validHeaderWithNoRows() throws JsonProcessingException {
        VenuesParsingResult result = new HighRiskVenueCsvParser()
            .toJson("# venue_id, start_time, end_time");

        assertEquals(emptyList(), venuesFrom(result));
    }

    @Test
    public void validCsv() throws JsonProcessingException {
        String csv = "" +
            "# venue_id, start_time, end_time\n" +
            "\"CD2\", \"2019-07-04T13:33:03Z\", \"2019-07-04T15:56:00Z\"\n" +
            "\"CD3\", \"2019-07-06T19:33:03Z\", \"2019-07-06T21:01:07Z\"\n" +
            "\"CD4\", \"2019-07-08T20:05:52Z\", \"2019-07-08T22:35:56Z\"";

        VenuesParsingResult result = new HighRiskVenueCsvParser()
            .toJson(csv);

        HighRiskVenue venue1 = new HighRiskVenue("CD2",
            new RiskyWindow("2019-07-04T13:33:03Z", "2019-07-04T15:56:00Z"));
        HighRiskVenue venue2 = new HighRiskVenue("CD3",
            new RiskyWindow("2019-07-06T19:33:03Z", "2019-07-06T21:01:07Z"));
        HighRiskVenue venue3 = new HighRiskVenue("CD4",
            new RiskyWindow("2019-07-08T20:05:52Z", "2019-07-08T22:35:56Z"));
        List<HighRiskVenue> venues = Arrays.asList(venue1, venue2, venue3);

        assertEquals(venues, venuesFrom(result));
    }

    @Test
    public void validCsvWithMessageTypeAndOptionalParameter() throws JsonProcessingException {
        String csv = "" +
            "# venue_id, start_time, end_time, message_type, optional_parameter\n" +
            "\"CD2\", \"2019-07-04T13:33:03Z\", \"2019-07-14T23:33:03Z\", \"M1\", \"\"\n" +
            "\"CD3\", \"2019-07-05T13:33:03Z\", \"2019-07-15T23:33:03Z\", \"M2\", \"\"\n" +
            "\"CD4\", \"2019-07-06T13:33:03Z\", \"2019-07-16T23:33:03Z\", \"M3\", \"07911 123456\"";


        VenuesParsingResult result = new HighRiskVenueCsvParser(true)
            .toJson(csv);

        HighRiskVenue venue1 = new HighRiskVenue("CD2",
            new RiskyWindow("2019-07-04T13:33:03Z", "2019-07-14T23:33:03Z"), "M1", null);
        HighRiskVenue venue2 = new HighRiskVenue("CD3",
            new RiskyWindow("2019-07-05T13:33:03Z", "2019-07-15T23:33:03Z"), "M2", null);
        HighRiskVenue venue3 = new HighRiskVenue("CD4",
            new RiskyWindow("2019-07-06T13:33:03Z", "2019-07-16T23:33:03Z"), "M3", "07911 123456");
        List<HighRiskVenue> venues = Arrays.asList(venue1, venue2, venue3);

        assertEquals(venues, venuesFrom(result));
    }

    @Test
    public void validCsvWithAdditionalWhitespace() throws JsonProcessingException {
        String csv = "" +
            "       # venue_id ,     start_time    ,    end_time \n" +
            " \"CD2\" , \"2019-07-04T13:33:03Z\" ,  \"2019-07-04T15:56:00Z\"\n" +
            "\"CD3\" ,\"2019-07-06T19:33:03Z\" , \"2019-07-06T21:01:07Z\"\n" +
            "   \"CD4\" , \"2019-07-08T20:05:52Z\" ,\"2019-07-08T22:35:56Z\"";


        VenuesParsingResult result = new HighRiskVenueCsvParser()
            .toJson(csv);

        HighRiskVenue venue1 = new HighRiskVenue("CD2",
            new RiskyWindow("2019-07-04T13:33:03Z", "2019-07-04T15:56:00Z"));
        HighRiskVenue venue2 = new HighRiskVenue("CD3",
            new RiskyWindow("2019-07-06T19:33:03Z", "2019-07-06T21:01:07Z"));
        HighRiskVenue venue3 = new HighRiskVenue("CD4",
            new RiskyWindow("2019-07-08T20:05:52Z", "2019-07-08T22:35:56Z"));
        List<HighRiskVenue> venues = Arrays.asList(venue1, venue2, venue3);

        assertEquals(venues, venuesFrom(result));
    }

    @Test
    public void throwsIfCsvWithHyphenInVenue() {
        String csv = "# venue_id, start_time, end_time \n" +
            "\"8CHARS-Y\", \"2019-07-04T13:33:03Z\", \"2019-07-04T23:33:03Z\"";

        VenuesParsingResult result = new HighRiskVenueCsvParser()
            .toJson(csv);

        assertThat(result.failureMaybe())
            .isEqualTo(Optional.of("validation error: Invalid characters on the venueId: 8CHARS-Y"));
    }

    @Test
    public void throwsIfCsvMessageTypeIsInvalid() {
        String csv = "# venue_id, start_time, end_time, message_type, optional_parameter\n" +
            "\"CD2\", \"2019-07-04T13:33:03Z\", \"2019-07-04T23:33:03Z\", \"d1\", \"\"";

        VenuesParsingResult result = new HighRiskVenueCsvParser(true)
            .toJson(csv);

        assertThat(result.failureMaybe())
            .isEqualTo(Optional.of("validation error: Invalid characters on the messageType: d1"));
    }

    @Test
    public void throwsIfCsvMessageTypeHasInvalidOptionalParameter() {
        String csv = "# venue_id, start_time, end_time, message_type, optional_parameter\n" +
            "\"CD2\", \"2019-07-04T13:33:03Z\", \"2019-07-04T23:33:03Z\", \"M1\", \"0712 34567\"";

        VenuesParsingResult result = new HighRiskVenueCsvParser(true)
            .toJson(csv);

        assertThat(result.failureMaybe())
            .isEqualTo(Optional.of("validation error: Message type M1 does not support optional parameter"));
    }

    @Test
    public void throwsIfCsvMessageTypeHasNoOptionalParameter() {
        String csv = "# venue_id, start_time, end_time, message_type, optional_parameter\n" +
            "\"CD2\", \"2019-07-04T13:33:03Z\", \"2019-07-04T23:33:03Z\", \"M3\", \"\"";

        VenuesParsingResult result = new HighRiskVenueCsvParser(true)
            .toJson(csv);

        assertThat(result.failureMaybe())
            .isEqualTo(Optional.of("validation error: Message type M3 must include an optional parameter"));
    }

    @Test
    public void validCsvToJsonUnixLineEnding() throws Exception {
        String csv = "" +
            "# venue_id, start_time, end_time\n" +
            "\"CD2\", \"2019-07-04T13:33:03Z\", \"2019-07-04T15:56:00Z\"\n" +
            "\"CD3\", \"2019-07-06T19:33:03Z\", \"2019-07-06T21:01:07Z\"\n" +
            "\"CD4\", \"2019-07-08T20:05:52Z\", \"2019-07-08T22:35:56Z\"";

        String expectedJson = "{\n" +
            "    \"venues\" : [\n" +
            "        {\n" +
            "            \"id\": \"CD2\",\n" +
            "            \"riskyWindow\": {\n" +
            "              \"from\": \"2019-07-04T13:33:03Z\",\n" +
            "              \"until\": \"2019-07-04T15:56:00Z\"\n" +
            "            },\n" +
            "           \"messageType\": \"M1\"\n" +
            "        },{\n" +
            "            \"id\": \"CD3\",\n" +
            "            \"riskyWindow\": {\n" +
            "              \"from\": \"2019-07-06T19:33:03Z\",\n" +
            "              \"until\": \"2019-07-06T21:01:07Z\"\n" +
            "            },\n" +
            "           \"messageType\": \"M1\"\n" +
            "        },{\n" +
            "            \"id\": \"CD4\",\n" +
            "            \"riskyWindow\": {\n" +
            "              \"from\": \"2019-07-08T20:05:52Z\",\n" +
            "              \"until\": \"2019-07-08T22:35:56Z\"\n" +
            "            },\n" +
            "           \"messageType\": \"M1\"\n" +
            "        }\n" +
            "\n" +
            "    ]\n" +
            "}";
        VenuesParsingResult result = new HighRiskVenueCsvParser().toJson(csv);
        JSONAssert.assertEquals(expectedJson, result.jsonOrThrow(), JSONCompareMode.STRICT);
    }

    @Test
    public void validCsvToJsonWindowsLineEnding() throws Exception {
        String csv = "" +
            "# venue_id, start_time, end_time\r\n" +
            "\"CD2\", \"2019-07-04T13:33:03Z\", \"2019-07-04T15:56:00Z\"\r\n" +
            "\"CD3\", \"2019-07-06T19:33:03Z\", \"2019-07-06T21:01:07Z\"\r\n" +
            "\"CD4\", \"2019-07-08T20:05:52Z\", \"2019-07-08T22:35:56Z\"";

        String expectedJson = "{\n" +
            "    \"venues\" : [\n" +
            "        {\n" +
            "            \"id\": \"CD2\",\n" +
            "            \"riskyWindow\": {\n" +
            "              \"from\": \"2019-07-04T13:33:03Z\",\n" +
            "              \"until\": \"2019-07-04T15:56:00Z\"\n" +
            "            },\n" +
            "           \"messageType\": \"M1\"\n" +
            "        },{\n" +
            "            \"id\": \"CD3\",\n" +
            "            \"riskyWindow\": {\n" +
            "              \"from\": \"2019-07-06T19:33:03Z\",\n" +
            "              \"until\": \"2019-07-06T21:01:07Z\"\n" +
            "            },\n" +
            "           \"messageType\": \"M1\"\n" +
            "        },{\n" +
            "            \"id\": \"CD4\",\n" +
            "            \"riskyWindow\": {\n" +
            "              \"from\": \"2019-07-08T20:05:52Z\",\n" +
            "              \"until\": \"2019-07-08T22:35:56Z\"\n" +
            "            },\n" +
            "           \"messageType\": \"M1\"\n" +
            "        }\n" +
            "\n" +
            "    ]\n" +
            "}";
        VenuesParsingResult result = new HighRiskVenueCsvParser().toJson(csv);
        JSONAssert.assertEquals(expectedJson, result.jsonOrThrow(), JSONCompareMode.STRICT);
    }

    @Test
    public void throwsIfNullCsv() {
        VenuesParsingResult result = new HighRiskVenueCsvParser().toJson(null);

        assertThat(result.failureMaybe())
            .isEqualTo(Optional.of("validation error: No payload"));
    }

    @Test
    public void throwsIfEmptyCsv() {
        VenuesParsingResult result = new HighRiskVenueCsvParser().toJson("");

        assertThat(result.failureMaybe())
            .isEqualTo(Optional.of("validation error: No payload"));
    }

    @Test
    public void throwsIfWhitespacesCsv() {
        VenuesParsingResult result = new HighRiskVenueCsvParser().toJson("     ");

        assertThat(result.failureMaybe())
            .isEqualTo(Optional.of("validation error: No payload"));
    }

    @Test
    public void throwsIfCsvTooManyCols() {
        String csv = "" +
            "# venue_id, start_time, end_time, vibe\n" +
            "\"ID1\", \"2019-07-04T13:33:03Z\", \"2019-07-04T15:56:00Z\", L\n" +
            "\"ID2\", \"2019-07-06T19:33:03Z\", \"2019-07-04T21:01:07Z\", M\n" +
            "\"ID3\", \"2019-07-08T20:05:52Z\", \"2019-07-04T22:35:56Z\", H";

        VenuesParsingResult result = new HighRiskVenueCsvParser().toJson(csv);

        assertThat(result.failureMaybe())
            .isEqualTo(Optional.of("validation error: Invalid header"));
    }

    @Test
    public void throwsIfInvalidHeader() {
        String csv = "" +
            "# venue_id, left, end_time\n" +
            "\"ID1\", \"2019-07-04T13:33:03Z\", \"2019-07-04T15:56:00Z\"\n" +
            "\"ID2\", \"2019-07-06T19:33:03Z\", \"2019-07-04T21:01:07Z\"\n" +
            "\"ID3\", \"2019-07-08T20:05:52Z\", \"2019-07-04T22:35:56Z\"";

        VenuesParsingResult result = new HighRiskVenueCsvParser().toJson(csv);

        assertThat(result.failureMaybe())
            .isEqualTo(Optional.of("validation error: Invalid header"));
    }

    @Test
    public void throwsIfDifferentInvalidRow() {
        String csv = "" +
            "# venue_id, start_time, end_time\n" +
            "\"CD2\", \"2019-07-04T13:33:03Z\", \"2019-07-04T15:56:00Z\"\n" +
            "\"CD3\", \"2019-07-06T19:33:03Z\", \"2019-07-06T21:01:07Z\"\n" +
            "\"CD4\", \"2019-07-08T20:05:52Z\", \"2019-07-08T22:35:56Z\", H";

        VenuesParsingResult result = new HighRiskVenueCsvParser().toJson(csv);

        assertThat(result.failureMaybe())
            .isEqualTo(Optional.of("validation error: Invalid data row on line number: 4"));
    }

    @Test
    public void throwsIfInvalidRow() {
        String csv = "" +
            "# venue_id, start_time, end_time\n" +
            "\"CD2\", \"2019-07-04T13:33:03Z\", \"2019-07-04T15:56:00Z\"\n" +
            "\"CD3\", \"2019-07-06T19:33:03Z\", \"2019-07-06T21:01:07Z\"\n" +
            "\"CD4\", \"2019-07-08T20:05:52Z\"";

        VenuesParsingResult result = new HighRiskVenueCsvParser().toJson(csv);

        assertThat(result.failureMaybe())
            .isEqualTo(Optional.of("validation error: Invalid data row on line number: 4"));
    }

    @Test
    public void throwsIfInvalidStartDate() {
        String csv = "" +
            "# venue_id, start_time, end_time\n" +
            "\"CD2\", \"2019-07-04T13:33:03Z\", \"2019-07-04T15:56:00Z\"\n" +
            "\"CD3\", \"2019-07-08T20:05F:52Z\", \"2019-07-04T22:35:56Z\"";

        VenuesParsingResult result = new HighRiskVenueCsvParser().toJson(csv);

        assertThat(result.failureMaybe())
            .isEqualTo(Optional.of(
                "validation error: Date 2019-07-08T20:05F:52Z did " +
                    "not conform to expected format yyyy-MM-dd'T'HH:mm:ss'Z'"
            ));
    }

    @Test
    public void throwsIfInvalidEndDate() {
        String csv = "" +
            "# venue_id, start_time, end_time\n" +
            "\"ID1\", \"2019-07-04T13:33:03Z\", \"2019-07-04T005:56:00Z\"\n" +
            "\"ID3\", \"2019-07-08T20:05:52Z\", \"2019-07-04T22:35:56Z\"";

        VenuesParsingResult result = new HighRiskVenueCsvParser().toJson(csv);

        assertThat(result.failureMaybe())
            .isEqualTo(Optional.of(
                "validation error: Date 2019-07-04T005:56:00Z " +
                    "did not conform to expected format yyyy-MM-dd'T'HH:mm:ss'Z'"
            ));
    }

    @Test
    public void throwsIfStartDateAfterEndDate() {
        String csv =
            "# venue_id, start_time, end_time\n" +
                "\"ID1\", \"2022-07-04T13:33:03Z\", \"2019-07-04T23:33:03Z\"";

        VenuesParsingResult result = new HighRiskVenueCsvParser().toJson(csv);

        assertThat(result.failureMaybe())
            .isEqualTo(Optional.of("validation error: Start date must be <= end date"));
    }

    @Test
    public void rowWithoutVenueIdThrows() {
        String csv =
            "# venue_id, start_time, end_time\n" +
                ", \"2019-07-04T15:56:00Z\", \"2019-07-04T15:56:00Z\"";

        VenuesParsingResult result = new HighRiskVenueCsvParser().toJson(csv);

        assertThat(result.failureMaybe())
            .isEqualTo(Optional.of("validation error: Invalid data row on line number: 2"));
    }

    private List<HighRiskVenue> venuesFrom(VenuesParsingResult result) throws JsonProcessingException {
            ObjectMapper riskyVenueMapper = new ObjectMapper()
                .deactivateDefaultTyping()
                .registerModule(new ParameterNamesModule(JsonCreator.Mode.PROPERTIES))
                .registerModule(new Jdk8Module());
            return riskyVenueMapper.readValue(result.jsonOrThrow(), HighRiskVenues.class).venues;
    }

    @Test
    public void throwsIfVenueIdHasMoreCharacters() {
        String csv =
            "# venue_id, start_time, end_time\n" +
                "\"CDEFH23456789\", \"2019-07-04T13:33:03Z\", \"2019-07-04T15:56:00Z\"";

        VenuesParsingResult result = new HighRiskVenueCsvParser().toJson(csv);

        assertThat(result.failureMaybe())
            .isEqualTo(Optional.of("validation error: Length of VenueId is greater than 12 characters"));
    }


    @Test
    public void throwsIfVenueIdHasOtherCharacters() {
        String csv =
            "# venue_id, start_time, end_time\n" +
                "\"ID123456789\", \"2019-07-04T13:33:03Z\", \"2019-07-04T15:56:00Z\"";

        VenuesParsingResult result = new HighRiskVenueCsvParser().toJson(csv);

        assertThat(result.failureMaybe())
            .isEqualTo(Optional.of("validation error: Invalid characters on the venueId: ID123456789"));
    }
}
