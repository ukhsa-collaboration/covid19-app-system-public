package uk.nhs.nhsx.highriskpostcodesupload;

import com.google.common.base.Strings;
import uk.nhs.nhsx.core.Jackson;
import uk.nhs.nhsx.core.csvupload.CsvToJsonParser;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static uk.nhs.nhsx.core.exceptions.Validations.throwValidationErrorWith;

public class RiskyPostCodesCsvParser implements CsvToJsonParser {

    private static final String POST_CODE_CSV_ROW_FORMAT = "\\s*\"(?<postCode>\\w+)\"\\s*,\\s*\"(?<riskIndicator>\\w+)\"\\s*";
    private static final String POST_CODE_CSV_HEADER_FORMAT = "\\s*#\\s*(?<postCodeColumnHeader>\\w+)\\s*,\\s*(?<riskIndicatorColumnHeader>\\w+)\\s*";
    private static final Pattern POST_CODE_CSV_ROW_PATTERN = Pattern.compile(POST_CODE_CSV_ROW_FORMAT);
    private static final Pattern POST_CODE_CSV_HEADER_PATTERN = Pattern.compile(POST_CODE_CSV_HEADER_FORMAT);
    private static final String POST_CODE_CSV_TIER_ROW_FORMAT = "\\s*\"(?<postCode>\\w+)\"\\s*,\\s*\"(?<riskIndicator>\\w+)\"\\s*,\\s*\"(?<tierIndicator>.*)\"\\s*";
    private static final String POST_CODE_CSV_TIER_HEADER_FORMAT = "\\s*#\\s*(?<postCodeColumnHeader>\\w+)\\s*,\\s*(?<riskIndicatorColumnHeader>\\w+)\\s*,\\s*(?<tierIndicatorColumnHeader>\\w+)\\s*";
    private static final Pattern POST_CODE_CSV_TIER_ROW_PATTERN = Pattern.compile(POST_CODE_CSV_TIER_ROW_FORMAT);
    private static final Pattern POST_CODE_CSV_TIER_HEADER_PATTERN = Pattern.compile(POST_CODE_CSV_TIER_HEADER_FORMAT);
    private static final List<String> VALID_RISK_INDICATORS = Arrays.asList("H", "M", "L");

    private static final String CSV_HEADER_POSTAL_DISTRICT_CODE = "postal_district_code";
    private static final String CSV_HEADER_RISK_INDICATOR = "risk_indicator";
    private static final String CSV_HEADER_TIER_INDICATOR = "tier_indicator";

    public static Map<String, String> parseV2(String csv, Map<String, RiskLevel> riskLevels) {
        if (Strings.isNullOrEmpty(csv))
            return new HashMap<>();

        var validTierIndicators = getValidTierIndicators(riskLevels);

        String[] rows = csv.split("\\r?\\n");
        Map<String, String> postCodeRiskIndicators = new HashMap<>();
        if (validateHeadersWithTiers(rows[0])) {
            for (int i = 1; i < rows.length; i++) {
                PostCodeRisk postCodeRisk = parseTierRow(rows[i], i + 1, validTierIndicators);
                if (!postCodeRisk.tierIndicator.isEmpty()) {
                    postCodeRiskIndicators.put(postCodeRisk.postCode, postCodeRisk.tierIndicator);
                }
            }
        }
        return postCodeRiskIndicators;
    }

    public static RiskyPostCodes parse(String csv, Map<String, RiskLevel> riskLevels) {
        if (Strings.isNullOrEmpty(csv))
            throwValidationErrorWith("No payload");

        var validTierIndicators = getValidTierIndicators(riskLevels);

        String[] rows = csv.split("\\r?\\n");

        if (!validateHeadersWithTiers(rows[0])) {
            validateHeaderRow(rows[0]);
        }

        Map<String, String> postCodeRiskIndicators = new HashMap<>();
        if (!validateHeadersWithTiers(rows[0])) {
            for (int i = 1; i < rows.length; i++) {
                PostCodeRisk postCodeRisk = parseRow(rows[i], i + 1);
                postCodeRiskIndicators.put(postCodeRisk.postCode, postCodeRisk.riskIndicator);

            }
        } else {
            for (int i = 1; i < rows.length; i++) {
                PostCodeRisk postCodeRisk = parseTierRow(rows[i], i + 1, validTierIndicators);
                postCodeRiskIndicators.put(postCodeRisk.postCode, postCodeRisk.riskIndicator);
            }
        }

        return new RiskyPostCodes(postCodeRiskIndicators);
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

        if (postCode.length() > 20) {
            throwValidationErrorWith("Invalid post district longer than 20 characters: " + postCode);
        }

        return new PostCodeRisk(postCode, riskIndicator);
    }

    private static PostCodeRisk parseTierRow(String dataRow, Integer row, List<String> validTierIndicators) {
        Matcher matcher = POST_CODE_CSV_TIER_ROW_PATTERN.matcher(dataRow);

        if (!matcher.matches()) {
            throwValidationErrorWith("Invalid data row on line number: " + row);
        }

        String postCode = matcher.group("postCode");
        String riskIndicator = matcher.group("riskIndicator");
        String tierIndicator = matcher.group("tierIndicator");

        if (!VALID_RISK_INDICATORS.contains(riskIndicator)) {
            throwValidationErrorWith("Invalid risk indicator on line number: " + row);
        }

        if (postCode.length() > 20) {
            throwValidationErrorWith("Invalid post district longer than 20 characters: " + postCode);
        }

        if (!validTierIndicators.contains(tierIndicator)) {
            throwValidationErrorWith("Invalid tier indicator on line number: " + row);
        }

        return new PostCodeRisk(postCode, riskIndicator, tierIndicator);
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

    private static boolean validateHeadersWithTiers(String headerRow) {
        Matcher matcher = POST_CODE_CSV_TIER_HEADER_PATTERN.matcher(headerRow);
        if (!matcher.matches()) {
            return false;
        }

        String postCodeColumnHeader = matcher.group("postCodeColumnHeader");
        String riskIndicatorColumnHeader = matcher.group("riskIndicatorColumnHeader");
        String tierIndicatorColumnHeader = matcher.group("tierIndicatorColumnHeader");

        return CSV_HEADER_POSTAL_DISTRICT_CODE.equals(postCodeColumnHeader) &&
            CSV_HEADER_RISK_INDICATOR.equals(riskIndicatorColumnHeader) &&
            CSV_HEADER_TIER_INDICATOR.equals(tierIndicatorColumnHeader);
    }

    public String toJson(String csv, Map<String, RiskLevel> riskLevels) {
        RiskyPostCodes riskyPostcodes = RiskyPostCodesCsvParser.parse(csv, riskLevels);
        return Jackson.toJson(riskyPostcodes);
    }

    private static List<String> getValidTierIndicators(Map<String, RiskLevel> riskLevels) {
        var tierIndicatorsList = new ArrayList<>(riskLevels.keySet());
        tierIndicatorsList.add("");
        return tierIndicatorsList;
    }

    private static class PostCodeRisk {
        final String postCode;
        final String riskIndicator;
        final String tierIndicator;

        PostCodeRisk(String postCode, String riskIndicator) {
            this(postCode, riskIndicator, "");
        }

        PostCodeRisk(String postCode, String riskIndicator, String tierIndicator) {
            this.postCode = postCode;
            this.riskIndicator = riskIndicator;
            this.tierIndicator = tierIndicator;
        }
    }

}
