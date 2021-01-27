package uk.nhs.nhsx.analyticssubmission;

import com.google.common.base.Strings;
import uk.nhs.nhsx.analyticssubmission.model.PostDistrictLADTuple;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PostDistrictLaReplacerCsvParser {

    private static final String HEADER_FORMAT = "\\s*\"(?<postDistrictLADIdHeader>\\w+)\"\\s*,\\s*\"(?<lad20cdHeader>\\w+)\"\\s*,\\s*\"(?<mergedPostcodeDistrictHeader>\\w+)\"\\s*";
    private static final String ROW_FORMAT = "\\s*\"(?<postDistrictLADId>[A-Z_0-9]*_[A-Z_0-9]*)\"\\s*,\\s*\"(?<lad20cd>[-A-Z0-9]*)\"\\s*,\\s*\"(?<mergedPostcodeDistrict>[-A-Z_0-9\\s]+)\"\\s*";
    private static final Pattern HEADER_PATTERN = Pattern.compile(HEADER_FORMAT);
    private static final Pattern ROW_PATTERN = Pattern.compile(ROW_FORMAT);
    private static final String POSTCODE_DISTRICT_LAD_ID = "Postcode_District_LAD_ID";
    private static final String LAD20CD = "LAD20CD";
    private static final String MERGED_POSTCODE_DISTRICT = "Merged_Postcode_District";

    public static Map<PostDistrictLADTuple, PostDistrictLADTuple> parse(String csv) {
        if (Strings.isNullOrEmpty(csv) || csv.trim().isEmpty()) {
            throw new RuntimeException("Empty csv");
        }
        Map<PostDistrictLADTuple, PostDistrictLADTuple> replacer = new HashMap<>();
        String[] rows = csv.split("\\r?\\n");
        validateHeaderRow(rows[0]);

        for (int i = 1; i < rows.length; i++) {
            Matcher matcher = ROW_PATTERN.matcher(rows[i]);
            if (!matcher.matches()){
                throw new RuntimeException("Invalid csv row: \n " + rows[i] + "\n at line number: " + i);
            }
            String postDistrictLADIdTuple = matcher.group("postDistrictLADId");
            String lad20cd = matcher.group("lad20cd");
            String mergedPostDistrict = matcher.group("mergedPostcodeDistrict");
            PostDistrictLADTuple input;
            PostDistrictLADTuple output;
            String[] splitTuple = postDistrictLADIdTuple.split("_");
            if (splitTuple.length > 1) {
                input = new PostDistrictLADTuple(splitTuple[0], splitTuple[1]);
                output = new PostDistrictLADTuple(mergedPostDistrict, lad20cd);
            } else {
                if (postDistrictLADIdTuple.equals("_")){
                    input = new PostDistrictLADTuple("", "");
                }
                else{
                    input = new PostDistrictLADTuple(splitTuple[0], "UNKNOWN");
                }
                output = new PostDistrictLADTuple(mergedPostDistrict, null);
            }

            replacer.put(input, output);
        }
        return replacer;
    }

    private static void validateHeaderRow(String header) {
        Matcher matcher = HEADER_PATTERN.matcher(header);
        if (!matcher.matches()) {
            throw new RuntimeException("Invalid csv header");
        }
        String postDistrictLADIDHeader = matcher.group("postDistrictLADIdHeader");
        String lad20cdHeader = matcher.group("lad20cdHeader");
        String mergedPostcodeDistrictHeader = matcher.group("mergedPostcodeDistrictHeader");
        if (!POSTCODE_DISTRICT_LAD_ID.equals(postDistrictLADIDHeader) ||
            !LAD20CD.equals(lad20cdHeader) ||
            !MERGED_POSTCODE_DISTRICT.equals(mergedPostcodeDistrictHeader)) {
            throw new RuntimeException("Invalid csv header: unexpected column headers");
        }
    }

    private static int indexOfLastMatch(Pattern pattern, String input) {
        Matcher matcher = pattern.matcher(input);
        for (int i = input.length(); i > 0; --i) {
            Matcher region = matcher.region(0, i);
            if (region.matches() || region.hitEnd()) {
                return i;
            }
        }

        return 0;
    }
}
