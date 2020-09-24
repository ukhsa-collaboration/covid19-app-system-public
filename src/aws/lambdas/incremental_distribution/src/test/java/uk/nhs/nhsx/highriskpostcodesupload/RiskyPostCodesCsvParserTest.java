package uk.nhs.nhsx.highriskpostcodesupload;

import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import uk.nhs.nhsx.core.exceptions.ApiResponseException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;

public class RiskyPostCodesCsvParserTest {

    @SuppressWarnings("serial")
    @Test
    public void parseCsv() {
        String csv = "" +
            "# postal_district_code, risk_indicator\n" +
            "\"CODE1\", \"H\"\n" +
            "\"CODE2\", \"M\"\n" +
            "\"CODE3\", \"L\"";

        RiskyPostCodes riskyPostcodes = RiskyPostCodesCsvParser.parse(csv);
        Map<String, String> postDistrictsMap = new HashMap<>() {
            {
                put("CODE1", "H");
                put("CODE2", "M");
                put("CODE3", "L");
            }
        };
        assertEquals(postDistrictsMap, riskyPostcodes.postDistricts);
    }

    @SuppressWarnings("serial")
    @Test
    public void parseCsvValidFile() throws Exception {
        String csv = readAsString("highriskpostcodes/postcodes.csv");

        RiskyPostCodes riskyPostcodes = RiskyPostCodesCsvParser.parse(csv);
        Map<String, String> postDistrictsMap = new HashMap<>() {
            {
                put("CODE1", "H");
                put("CODE2", "M");
                put("CODE3", "L");
            }
        };
        assertEquals(postDistrictsMap, riskyPostcodes.postDistricts);
    }

    @SuppressWarnings("serial")
    @Test
    public void parseCsvValidFileWithExtraWhitespace() throws Exception {
        String csv = readAsString("highriskpostcodes/postcodes_extra_whitespace.csv");

        RiskyPostCodes riskyPostcodes = RiskyPostCodesCsvParser.parse(csv);
        Map<String, String> postDistrictsMap = new HashMap<>() {
            {
                put("CODE1", "H");
                put("CODE2", "M");
                put("CODE3", "L");
            }
        };
        assertEquals(postDistrictsMap, riskyPostcodes.postDistricts);
    }

    @Test
    public void parseCsvWithTooManyColumnsShouldThrowException() throws Exception {
        String csv = readAsString("highriskpostcodes/postcodes_extra_column.csv");

        assertThatThrownBy(() -> RiskyPostCodesCsvParser.parse(csv))
            .isInstanceOf(ApiResponseException.class)
            .hasMessage("validation error: Invalid header");
    }

    @Test
    public void parseCsvWithInvalidHeaderShouldThrowException() throws Exception {
        String csv = readAsString("highriskpostcodes/postcodes_invalid_header.csv");

        assertThatThrownBy(() -> RiskyPostCodesCsvParser.parse(csv))
            .isInstanceOf(ApiResponseException.class)
            .hasMessage("validation error: Invalid header");
    }

    @Test
    public void parseCsvWithInvalidRiskHeaderShouldThrowException() throws Exception {
        String csv = readAsString("highriskpostcodes/postcodes_invalid_risk_header.csv");

        assertThatThrownBy(() -> RiskyPostCodesCsvParser.parse(csv))
            .isInstanceOf(ApiResponseException.class)
            .hasMessage("validation error: Invalid header");
    }

    @Test
    public void parseCsvWithInvalidRiskIndicatorShouldThrowException() throws Exception {
        String csv = readAsString("highriskpostcodes/postcodes_invalid_risk_indicator.csv");
        assertThatThrownBy(() -> RiskyPostCodesCsvParser.parse(csv))
            .isInstanceOf(ApiResponseException.class)
            .hasMessage("validation error: Invalid risk indicator on line number: 5");
    }

    @Test
    public void parseCsvWithInvalidRowShouldThrowException() throws Exception {
        String csv = readAsString("highriskpostcodes/postcodes_invalid_row.csv");

        assertThatThrownBy(() -> RiskyPostCodesCsvParser.parse(csv))
            .isInstanceOf(ApiResponseException.class)
            .hasMessage("validation error: Invalid data row on line number: 5");
    }

    @Test
    public void parseCsvWithInvalidAdditionalDataShouldThrowException() throws Exception {
        String csv = readAsString("highriskpostcodes/postcodes_invalid_row_2.csv");

        assertThatThrownBy(() -> RiskyPostCodesCsvParser.parse(csv))
            .isInstanceOf(ApiResponseException.class)
            .hasMessage("validation error: Invalid data row on line number: 5");
    }

    @Test
    public void validateHeader() {
        RiskyPostCodesCsvParser.parse("# postal_district_code, risk_indicator");
    }

    @Test
    public void postDistrictLargerThan20CharactersThrowsException(){
        String csv = "" +
            "# postal_district_code, risk_indicator\n" +
            "\"123456789012345678901\", \"H\"\n" +
            "\"CODE2\", \"M\"\n" +
            "\"CODE3\", \"L\"";
        assertThatThrownBy(() -> RiskyPostCodesCsvParser.parse(csv))
            .isInstanceOf(ApiResponseException.class)
            .hasMessage("validation error: Invalid post district longer than 20 characters: 123456789012345678901");
    }

    @Test
    public void validCsvToJson() throws Exception {
        String csv = "" +
            "# postal_district_code, risk_indicator\n" +
            "\"CODE1\", \"H\"\n" +
            "\"CODE2\", \"M\"\n" +
            "\"CODE3\", \"L\"";

        String json = new RiskyPostCodesCsvParser().toJson(csv);
        JSONAssert.assertEquals(
            "{\"postDistricts\":{\"CODE2\":\"M\",\"CODE1\":\"H\",\"CODE3\":\"L\"}}",
            json,
            JSONCompareMode.STRICT
        );
    }

    @Test
    public void csvToJson() throws Exception {
        String csv = readAsString("inputcsv/postcodes.csv");
        String expectedJson = readAsString("outputjson/postcodes.json");

        String json = new RiskyPostCodesCsvParser().toJson(csv);
        JSONAssert.assertEquals(expectedJson, json, JSONCompareMode.STRICT);
    }

    @Test
    public void validCsvToJsonWindowsLineEnding() throws Exception {
        String csv = "" +
            "# postal_district_code, risk_indicator\r\n" +
            "\"CODE1\", \"H\"\r\n" +
            "\"CODE2\", \"M\"\r\n" +
            "\"CODE3\", \"L\"";

        String expectedJson = "{\n" +
            "  \"postDistricts\": {\n" +
            "    \"CODE1\": \"H\",\n" +
            "    \"CODE2\": \"M\",\n" +
            "    \"CODE3\": \"L\"\n" +
            "  }\n" +
            "}";
        String json = new RiskyPostCodesCsvParser().toJson(csv);
        JSONAssert.assertEquals(expectedJson, json, JSONCompareMode.STRICT);
    }

    private static String readAsString(String resourceName) throws IOException, URISyntaxException {
        URL resource = RiskyPostCodesCsvParserTest.class.getClassLoader().getResource(resourceName);
        Objects.requireNonNull(resource);
        return new String(Files.readAllBytes(Paths.get(resource.toURI())));
    }
}
