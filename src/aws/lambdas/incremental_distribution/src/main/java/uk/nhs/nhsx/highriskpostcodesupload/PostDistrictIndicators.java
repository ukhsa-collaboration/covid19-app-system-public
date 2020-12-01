package uk.nhs.nhsx.highriskpostcodesupload;

import com.fasterxml.jackson.annotation.JsonCreator;

public class PostDistrictIndicators {

    public final String riskIndicator;
    public final String tierIndicator;

    @JsonCreator
    public PostDistrictIndicators(String riskIndicator, String tierIndicator) {
        this.riskIndicator = riskIndicator;
        this.tierIndicator = tierIndicator;
    }
}
