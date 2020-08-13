package uk.nhs.nhsx.highriskvenuesupload;

import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import uk.nhs.nhsx.core.Jackson;
import uk.nhs.nhsx.highriskvenuesupload.model.HighRiskVenue;
import uk.nhs.nhsx.highriskvenuesupload.model.HighRiskVenues;
import uk.nhs.nhsx.highriskvenuesupload.model.RiskyWindow;

import java.util.*;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

public class RiskyVenuesCsvParserTest {

    @Test
    public void validHeaderWithNoRows() {
        VenuesParsingResult result = new HighRiskVenueCsvParser()
            .toJson("# venue_id, start_time, end_time");

        assertEquals(emptyList(), venuesFrom(result));
    }

    @Test
    public void validCsv() {
        String csv = "" +
            "# venue_id, start_time, end_time\n" +
            "\"ID1\", \"2019-07-04T13:33:03Z\", \"2019-07-04T15:56:00Z\"\n" +
            "\"ID2\", \"2019-07-06T19:33:03Z\", \"2019-07-06T21:01:07Z\"\n" +
            "\"ID3\", \"2019-07-08T20:05:52Z\", \"2019-07-08T22:35:56Z\"";

        VenuesParsingResult result = new HighRiskVenueCsvParser()
            .toJson(csv);

        HighRiskVenue venue1 = new HighRiskVenue("ID1",
            new RiskyWindow("2019-07-04T13:33:03Z", "2019-07-04T15:56:00Z"));
        HighRiskVenue venue2 = new HighRiskVenue("ID2",
            new RiskyWindow("2019-07-06T19:33:03Z", "2019-07-06T21:01:07Z"));
        HighRiskVenue venue3 = new HighRiskVenue("ID3",
            new RiskyWindow("2019-07-08T20:05:52Z", "2019-07-08T22:35:56Z"));
        List<HighRiskVenue> venues = Arrays.asList(venue1, venue2, venue3);

        assertEquals(venues, venuesFrom(result));
    }

    @Test
    public void validCsvWithAdditionalWhitespace() {
        String csv = "" +
            "       # venue_id ,     start_time    ,    end_time \n" +
            " \"ID1\" , \"2019-07-04T13:33:03Z\" ,  \"2019-07-04T15:56:00Z\"\n" +
            "\"ID2\" ,\"2019-07-06T19:33:03Z\" , \"2019-07-06T21:01:07Z\"\n" +
            "   \"ID3\" , \"2019-07-08T20:05:52Z\" ,\"2019-07-08T22:35:56Z\"";


        VenuesParsingResult result = new HighRiskVenueCsvParser()
            .toJson(csv);

        HighRiskVenue venue1 = new HighRiskVenue("ID1",
            new RiskyWindow("2019-07-04T13:33:03Z", "2019-07-04T15:56:00Z"));
        HighRiskVenue venue2 = new HighRiskVenue("ID2",
            new RiskyWindow("2019-07-06T19:33:03Z", "2019-07-06T21:01:07Z"));
        HighRiskVenue venue3 = new HighRiskVenue("ID3",
            new RiskyWindow("2019-07-08T20:05:52Z", "2019-07-08T22:35:56Z"));
        List<HighRiskVenue> venues = Arrays.asList(venue1, venue2, venue3);

        assertEquals(venues, venuesFrom(result));
    }

    @Test
    public void validCsvWithHyphenInVenue() {
        String csv = "# venue_id, start_time, end_time \n" +
            "\"8CHARS-Y\", \"2019-07-04T13:33:03Z\", \"2019-07-04T23:33:03Z\"";

        VenuesParsingResult result = new HighRiskVenueCsvParser()
            .toJson(csv);

        HighRiskVenue venue1 = new HighRiskVenue("8CHARS-Y",
            new RiskyWindow("2019-07-04T13:33:03Z", "2019-07-04T23:33:03Z"));
        List<HighRiskVenue> venues = Collections.singletonList(venue1);

        assertEquals(venues, venuesFrom(result));
    }

    @Test
    public void validCsvToJsonUnixLineEnding() throws Exception {
        String csv = "" +
            "# venue_id, start_time, end_time\n" +
            "\"ID1\", \"2019-07-04T13:33:03Z\", \"2019-07-04T15:56:00Z\"\n" +
            "\"ID2\", \"2019-07-06T19:33:03Z\", \"2019-07-06T21:01:07Z\"\n" +
            "\"ID3\", \"2019-07-08T20:05:52Z\", \"2019-07-08T22:35:56Z\"";

        String expectedJson = "{\n" +
            "    \"venues\" : [\n" +
            "        {\n" +
            "            \"id\": \"ID1\",\n" +
            "            \"riskyWindow\": {\n" +
            "              \"from\": \"2019-07-04T13:33:03Z\",\n" +
            "              \"until\": \"2019-07-04T15:56:00Z\"\n" +
            "            }\n" +
            "        },{\n" +
            "            \"id\": \"ID2\",\n" +
            "            \"riskyWindow\": {\n" +
            "              \"from\": \"2019-07-06T19:33:03Z\",\n" +
            "              \"until\": \"2019-07-06T21:01:07Z\"\n" +
            "            }\n" +
            "        },{\n" +
            "            \"id\": \"ID3\",\n" +
            "            \"riskyWindow\": {\n" +
            "              \"from\": \"2019-07-08T20:05:52Z\",\n" +
            "              \"until\": \"2019-07-08T22:35:56Z\"\n" +
            "            }\n" +
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
            "\"ID1\", \"2019-07-04T13:33:03Z\", \"2019-07-04T15:56:00Z\"\r\n" +
            "\"ID2\", \"2019-07-06T19:33:03Z\", \"2019-07-06T21:01:07Z\"\r\n" +
            "\"ID3\", \"2019-07-08T20:05:52Z\", \"2019-07-08T22:35:56Z\"";

        String expectedJson = "{\n" +
            "    \"venues\" : [\n" +
            "        {\n" +
            "            \"id\": \"ID1\",\n" +
            "            \"riskyWindow\": {\n" +
            "              \"from\": \"2019-07-04T13:33:03Z\",\n" +
            "              \"until\": \"2019-07-04T15:56:00Z\"\n" +
            "            }\n" +
            "        },{\n" +
            "            \"id\": \"ID2\",\n" +
            "            \"riskyWindow\": {\n" +
            "              \"from\": \"2019-07-06T19:33:03Z\",\n" +
            "              \"until\": \"2019-07-06T21:01:07Z\"\n" +
            "            }\n" +
            "        },{\n" +
            "            \"id\": \"ID3\",\n" +
            "            \"riskyWindow\": {\n" +
            "              \"from\": \"2019-07-08T20:05:52Z\",\n" +
            "              \"until\": \"2019-07-08T22:35:56Z\"\n" +
            "            }\n" +
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
            "\"ID1\", \"2019-07-04T13:33:03Z\", \"2019-07-04T15:56:00Z\"\n" +
            "\"ID2\", \"2019-07-06T19:33:03Z\", \"2019-07-06T21:01:07Z\"\n" +
            "\"ID3\", \"2019-07-08T20:05:52Z\", \"2019-07-08T22:35:56Z\", H";

        VenuesParsingResult result = new HighRiskVenueCsvParser().toJson(csv);

        assertThat(result.failureMaybe())
            .isEqualTo(Optional.of("validation error: Invalid data row on line number: 4"));
    }

    @Test
    public void throwsIfInvalidRow() {
        String csv = "" +
            "# venue_id, start_time, end_time\n" +
            "\"ID1\", \"2019-07-04T13:33:03Z\", \"2019-07-04T15:56:00Z\"\n" +
            "\"ID2\", \"2019-07-06T19:33:03Z\", \"2019-07-06T21:01:07Z\"\n" +
            "\"ID3\", \"2019-07-08T20:05:52Z\"";

        VenuesParsingResult result = new HighRiskVenueCsvParser().toJson(csv);

        assertThat(result.failureMaybe())
            .isEqualTo(Optional.of("validation error: Invalid data row on line number: 4"));
    }

    @Test
    public void throwsIfInvalidStartDate() {
        String csv = "" +
            "# venue_id, start_time, end_time\n" +
            "\"ID1\", \"2019-07-04T13:33:03Z\", \"2019-07-04T15:56:00Z\"\n" +
            "\"ID3\", \"2019-07-08T20:05F:52Z\", \"2019-07-04T22:35:56Z\"";

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

    private List<HighRiskVenue> venuesFrom(VenuesParsingResult result) {
        return Jackson.deserializeMaybe(result.jsonOrThrow(), HighRiskVenues.class)
            .map(it -> it.venues)
            .orElseThrow(() -> new IllegalStateException("Could not deserialize venues"));
    }
}
