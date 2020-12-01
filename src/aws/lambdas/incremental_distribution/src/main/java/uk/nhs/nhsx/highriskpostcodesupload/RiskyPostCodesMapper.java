package uk.nhs.nhsx.highriskpostcodesupload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static org.apache.logging.log4j.util.Strings.isBlank;
import static uk.nhs.nhsx.core.exceptions.Validations.throwValidationErrorWith;

public class RiskyPostCodesMapper {

    private static final List<String> VALID_RISK_INDICATORS = asList("H", "M", "L");

    private final Map<String, Map<String, Object>> tierMetadata;
    private final List<String> validTierIndicators;

    public RiskyPostCodesMapper(Map<String, Map<String, Object>> tierMetadata) {
        this.tierMetadata = tierMetadata;
        this.validTierIndicators = new ArrayList<>(tierMetadata.keySet());
    }

    public RiskyPostCodesResult mapOrThrow(RiskyPostDistrictsRequest request) {
        var postCodesToRiskIndicator = new HashMap<String, String>();
        var postCodesToTierIndicator = new HashMap<String, String>();

        request.postDistricts.forEach(
            (postDistrict, indicators) -> {
                validatePostDistrict(postDistrict, indicators);
                postCodesToRiskIndicator.put(postDistrict, indicators.riskIndicator);
                postCodesToTierIndicator.put(postDistrict, indicators.tierIndicator);
            }
        );

        var localAuthoritiesToRiskIndicator = new HashMap<String, String>();
        request.localAuthorities.forEach(
            (localAuthority, indicators) -> {
                validateLocalAuthority(localAuthority, indicators);
                localAuthoritiesToRiskIndicator.put(localAuthority, indicators.tierIndicator);
            }
        );

        var v1 = new RiskyPostCodesV1(postCodesToRiskIndicator);
        var v2 = new RiskyPostCodesV2(postCodesToTierIndicator, localAuthoritiesToRiskIndicator, tierMetadata);

        return new RiskyPostCodesResult(v1, v2);
    }

    private void validatePostDistrict(String postDistrict, PostDistrictIndicators indicators) {
        if (isBlank(postDistrict)) {
            throwValidationErrorWith("Invalid post district - no value");
        }

        if (postDistrict.length() > 20) {
            throwValidationErrorWith("Invalid post district longer than 20 characters: " + postDistrict);
        }

        if (!VALID_RISK_INDICATORS.contains(indicators.riskIndicator)) {
            throwValidationErrorWith(
                "Invalid risk indicator: " + indicators.riskIndicator + " for post district: " + postDistrict
            );
        }

        if (!validTierIndicators.contains(indicators.tierIndicator)) {
            throwValidationErrorWith(
                "Invalid tier indicator: " + indicators.tierIndicator + " for post district: " + postDistrict
            );
        }
    }

    private void validateLocalAuthority(String localAuthority, LocalAuthorityIndicators indicators) {
        if (isBlank(localAuthority)) {
            throwValidationErrorWith("Invalid local authority - no value");
        }

        if (!validTierIndicators.contains(indicators.tierIndicator)) {
            throwValidationErrorWith(
                "Invalid tier indicator: " + indicators.tierIndicator + " for local authority: " + localAuthority
            );
        }
    }

    public String convertToAnalyticsCsv(RiskyPostDistrictsRequest request) {
        return request.postDistricts
            .entrySet().stream()
            .map(toCsvRow())
            .reduce("# postal_district_code, risk_indicator, tier_indicator", String::concat);
    }

    private Function<Map.Entry<String, PostDistrictIndicators>, String> toCsvRow() {
        return postDistrictEntry -> "\n\""
            + postDistrictEntry.getKey() + "\", \""
            + postDistrictEntry.getValue().riskIndicator + "\", \""
            + postDistrictEntry.getValue().tierIndicator + "\"";
    }
}
