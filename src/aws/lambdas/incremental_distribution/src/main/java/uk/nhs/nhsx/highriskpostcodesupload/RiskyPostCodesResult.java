package uk.nhs.nhsx.highriskpostcodesupload;

import java.util.Objects;

public class RiskyPostCodesResult {

    public final RiskyPostCodesV1 riskyPostCodesV1;
    public final RiskyPostCodesV2 riskyPostCodesV2;

    public RiskyPostCodesResult(RiskyPostCodesV1 riskyPostCodesV1,
                                RiskyPostCodesV2 riskyPostCodesV2) {
        this.riskyPostCodesV1 = riskyPostCodesV1;
        this.riskyPostCodesV2 = riskyPostCodesV2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RiskyPostCodesResult that = (RiskyPostCodesResult) o;
        return Objects.equals(riskyPostCodesV1, that.riskyPostCodesV1) &&
            Objects.equals(riskyPostCodesV2, that.riskyPostCodesV2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(riskyPostCodesV1, riskyPostCodesV2);
    }
}
