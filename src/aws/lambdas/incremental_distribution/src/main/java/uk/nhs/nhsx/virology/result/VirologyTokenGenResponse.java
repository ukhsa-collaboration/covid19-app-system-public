package uk.nhs.nhsx.virology.result;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Objects;

public class VirologyTokenGenResponse {

    public final String ctaToken;

    @JsonCreator
    private VirologyTokenGenResponse(String ctaToken) {
        this.ctaToken = ctaToken;
    }

    public static VirologyTokenGenResponse of(String ctaToken) {
        return new VirologyTokenGenResponse(ctaToken);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VirologyTokenGenResponse that = (VirologyTokenGenResponse) o;
        return Objects.equals(ctaToken, that.ctaToken);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ctaToken);
    }

    @Override
    public String toString() {
        return "VirologyTokenGenResponse{" +
            "ctaToken='" + ctaToken + '\'' +
            '}';
    }
}