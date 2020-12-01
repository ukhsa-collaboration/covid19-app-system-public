package uk.nhs.nhsx.highriskpostcodesupload;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Map;
import java.util.Objects;

import static java.util.Collections.unmodifiableMap;

public class RiskyPostCodesV1 {

    public final Map<String, String> postDistricts;

    @JsonCreator
    RiskyPostCodesV1(Map<String, String> postDistricts) {
        this.postDistricts = unmodifiableMap(postDistricts);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RiskyPostCodesV1 that = (RiskyPostCodesV1) o;
        return Objects.equals(postDistricts, that.postDistricts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(postDistricts);
    }
}
