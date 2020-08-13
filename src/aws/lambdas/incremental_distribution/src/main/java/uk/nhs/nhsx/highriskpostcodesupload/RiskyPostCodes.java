package uk.nhs.nhsx.highriskpostcodesupload;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Map;

import static java.util.Collections.unmodifiableMap;

public class RiskyPostCodes {

    public final Map<String, String> postDistricts;

    @JsonCreator
    RiskyPostCodes(Map<String, String> postDistricts) {
        this.postDistricts = unmodifiableMap(postDistricts);
    }
}
