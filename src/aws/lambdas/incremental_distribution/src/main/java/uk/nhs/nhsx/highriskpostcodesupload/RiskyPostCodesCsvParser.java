package uk.nhs.nhsx.highriskpostcodesupload;

import com.google.common.base.Strings;
import uk.nhs.nhsx.core.Jackson;
import uk.nhs.nhsx.core.csvupload.CsvToJsonParser;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static uk.nhs.nhsx.core.exceptions.Validations.throwValidationErrorWith;

public class RiskyPostCodesCsvParser implements CsvToJsonParser {

    private static final String POST_CODE_CSV_ROW_FORMAT = "\\s*\"(?<postCode>\\w+)\"\\s*,\\s*\"(?<riskIndicator>\\w+)\"\\s*";
    private static final String POST_CODE_CSV_HEADER_FORMAT = "\\s*#\\s*(?<postCodeColumnHeader>\\w+)\\s*,\\s*(?<riskIndicatorColumnHeader>\\w+)\\s*";
    private static final Pattern POST_CODE_CSV_ROW_PATTERN = Pattern.compile(POST_CODE_CSV_ROW_FORMAT);
    private static final Pattern POST_CODE_CSV_HEADER_PATTERN = Pattern.compile(POST_CODE_CSV_HEADER_FORMAT);
    private static final List<String> VALID_RISK_INDICATORS = Arrays.asList("H", "M", "L");
    private static final String CSV_HEADER_POSTAL_DISTRICT_CODE = "postal_district_code";
    private static final String CSV_HEADER_RISK_INDICATOR = "risk_indicator";

    public String toJson(String csv) {
        RiskyPostCodes riskyPostcodes = RiskyPostCodesCsvParser.parse(csv);
        return Jackson.toJson(riskyPostcodes);
    }
    
    public static RiskyPostCodes parse(String csv) {
        if (Strings.isNullOrEmpty(csv))
            throwValidationErrorWith("No payload");

        String[] rows = csv.split("\\r?\\n");

        validateHeaderRow(rows[0]);

        Map<String, String> postCodeRiskIndicators = new HashMap<>();
        for (int i = 1; i < rows.length; i++) {
            PostCodeRisk postCodeRisk = parseRow(rows[i], i+1);
            postCodeRiskIndicators.put(postCodeRisk.postCode, postCodeRisk.riskIndicator);
        }
        return new RiskyPostCodes(postCodeRiskIndicators);
    }

    private static class PostCodeRisk {
        final String postCode;
        final String riskIndicator;

        PostCodeRisk(String postCode, String riskIndicator) {
            this.postCode = postCode;
            this.riskIndicator = riskIndicator;
        }
    }

    private static PostCodeRisk parseRow(String dataRow, Integer row) {
        Matcher matcher = POST_CODE_CSV_ROW_PATTERN.matcher(dataRow);
        if (!matcher.matches()) {
            throwValidationErrorWith("Invalid data row on line number: " + row);
        }

        String postCode = matcher.group("postCode");
        String riskIndicator = matcher.group("riskIndicator");

        if (!VALID_RISK_INDICATORS.contains(riskIndicator)) {
            throwValidationErrorWith("Invalid risk indicator on line number: " + row);
        }

        if (postCode.length() > 20){
            throwValidationErrorWith("Invalid post district longer than 20 characters: " + postCode);
        }

        return new PostCodeRisk(postCode, riskIndicator);
    }

    private static void validateHeaderRow(String headerRow) {
        Matcher matcher = POST_CODE_CSV_HEADER_PATTERN.matcher(headerRow);
        if (!matcher.matches()) {
            throwValidationErrorWith("Invalid header");
        }

        String postCodeColumnHeader = matcher.group("postCodeColumnHeader");
        String riskIndicatorColumnHeader = matcher.group("riskIndicatorColumnHeader");

        if (!CSV_HEADER_POSTAL_DISTRICT_CODE.equals(postCodeColumnHeader) ||
            !CSV_HEADER_RISK_INDICATOR.equals(riskIndicatorColumnHeader)) {
            throwValidationErrorWith("Invalid header");
        }
    }

}
