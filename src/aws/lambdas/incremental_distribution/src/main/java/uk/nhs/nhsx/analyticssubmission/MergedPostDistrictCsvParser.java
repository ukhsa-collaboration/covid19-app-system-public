package uk.nhs.nhsx.analyticssubmission;

import com.google.common.base.Strings;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MergedPostDistrictCsvParser {

    private static final String HEADER_FORMAT = "\\s*\"(?<postDistrictHeader>\\w+)\"\\s*,\\s*\"(?<mergedPostDistrictHeader>\\w+)\"\\s*,\\s*\"(?<estimatedPopulationHeader>\\w+)\"\\s*";
    private static final String ROW_FORMAT = "\\s*\"(?<postDistrict>[A-Z0-9]+)\"\\s*,\\s*\"(?<mergedPostDistrict>[A-Z_0-9]+)\"\\s*,\\s*(?<estimatedPopulation>\\d+)\\s*";
    private static final Pattern HEADER_PATTERN = Pattern.compile(HEADER_FORMAT);
    private static final Pattern ROW_PATTERN = Pattern.compile(ROW_FORMAT);
    private static final String POST_DISTRICT_HEADER = "Postcode_District";
    private static final String MERGED_POST_DISTRICT_HEADER = "Merged_Postcode_District";
    private static final String ESTIMATED_POPULATION_HEADER = "Estimated_Population";

    public static Map<String, String> parse(String csv){
        if (Strings.isNullOrEmpty(csv) || csv.trim().isEmpty()) {
            throw new RuntimeException("Empty csv");
        }
        Map<String, String> map = new HashMap<>();
        String[] rows = csv.split("\\r?\\n");
        validateHeaderRow(rows[0]);

        for (int i = 1; i < rows.length; i++) {
            Matcher matcher = ROW_PATTERN.matcher(rows[i]);
            if (!matcher.matches()){
                throw new RuntimeException("Invalid csv row: \n " + rows[i] + "\n at line number: " + i);
            }
            String postDistrict = matcher.group("postDistrict");
            String mergedPostDistrict = matcher.group("mergedPostDistrict");
            map.put(postDistrict, mergedPostDistrict);
        }
        return map;
    }

    private static void validateHeaderRow(String header){
        Matcher matcher = HEADER_PATTERN.matcher(header);
        if (!matcher.matches()){
            throw new RuntimeException("Invalid csv header");
        }
        String postDistrictHeader = matcher.group("postDistrictHeader");
        String mergedPostDistrictHeader = matcher.group("mergedPostDistrictHeader");
        String estimatedPopulationHeader = matcher.group("estimatedPopulationHeader");
        if (!POST_DISTRICT_HEADER.equals(postDistrictHeader) ||
            !MERGED_POST_DISTRICT_HEADER.equals(mergedPostDistrictHeader) ||
            !ESTIMATED_POPULATION_HEADER.equals(estimatedPopulationHeader)){
            throw new RuntimeException("Invalid csv header: unexpected column headers");
        }
    }
}