package uk.nhs.nhsx.highriskpostcodesupload;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Map;

public class RiskyPostDistrictsRequest {

    public final Map<String, PostDistrictIndicators> postDistricts;
    public final Map<String, LocalAuthorityIndicators> localAuthorities;

    @JsonCreator
    public RiskyPostDistrictsRequest(Map<String, PostDistrictIndicators> postDistricts,
                                     Map<String, LocalAuthorityIndicators> localAuthorities) {
        this.postDistricts = postDistricts;
        this.localAuthorities = localAuthorities;
    }
}
