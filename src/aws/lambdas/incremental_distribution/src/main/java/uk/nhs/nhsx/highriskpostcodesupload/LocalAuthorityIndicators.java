package uk.nhs.nhsx.highriskpostcodesupload;

import com.fasterxml.jackson.annotation.JsonCreator;

public class LocalAuthorityIndicators {

    public final String tierIndicator;

    @JsonCreator
    public LocalAuthorityIndicators(String tierIndicator) {
        this.tierIndicator = tierIndicator;
    }
}
