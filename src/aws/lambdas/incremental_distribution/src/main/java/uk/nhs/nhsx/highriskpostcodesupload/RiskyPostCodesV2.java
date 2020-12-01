package uk.nhs.nhsx.highriskpostcodesupload;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Map;
import java.util.Objects;

import static java.util.Collections.unmodifiableMap;

public class RiskyPostCodesV2 {

    public final Map<String, String> postDistricts;
    public final Map<String, String> localAuthorities;
    public final Map<String, Map<String, Object>> riskLevels;

    @JsonCreator
    public RiskyPostCodesV2(Map<String, String> postDistricts,
                            Map<String, String> localAuthorities,
                            Map<String, Map<String, Object>> riskLevels) {
        this.postDistricts = unmodifiableMap(postDistricts);
        this.localAuthorities = unmodifiableMap(localAuthorities);
        this.riskLevels = riskLevels;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RiskyPostCodesV2 that = (RiskyPostCodesV2) o;
        return Objects.equals(postDistricts, that.postDistricts) &&
            Objects.equals(localAuthorities, that.localAuthorities) &&
            Objects.equals(riskLevels, that.riskLevels);
    }

    @Override
    public int hashCode() {
        return Objects.hash(postDistricts, localAuthorities, riskLevels);
    }
}
