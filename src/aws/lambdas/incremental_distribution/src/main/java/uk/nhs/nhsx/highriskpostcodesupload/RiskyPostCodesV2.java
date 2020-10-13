package uk.nhs.nhsx.highriskpostcodesupload;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Map;

import static java.util.Collections.unmodifiableMap;

public class RiskyPostCodesV2 {

    public final Map<String, String> postDistricts;

    public final Map<String, RiskLevel> riskLevels;

    @JsonCreator
    public RiskyPostCodesV2(Map<String, String> postDistricts, Map<String, RiskLevel> riskLevels) {
        this.postDistricts = unmodifiableMap(postDistricts);
        this.riskLevels = riskLevels;
    }

}
