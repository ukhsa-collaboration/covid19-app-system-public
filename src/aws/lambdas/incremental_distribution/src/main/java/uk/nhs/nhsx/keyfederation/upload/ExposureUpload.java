package uk.nhs.nhsx.keyfederation.upload;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ExposureUpload {

    public final String keyData;
    public final int rollingStartNumber;
    public final int transmissionRiskLevel;
    public final int rollingPeriod;
    public final List<String> regions;

    @JsonCreator
    public ExposureUpload(@JsonProperty String keyData,
                          @JsonProperty int rollingStartNumber,
                          @JsonProperty int transmissionRiskLevel,
                          @JsonProperty int rollingPeriod,
                          @JsonProperty List<String> regions) {
        this.keyData = keyData;
        this.rollingStartNumber = rollingStartNumber;
        this.transmissionRiskLevel = transmissionRiskLevel;
        this.rollingPeriod = rollingPeriod;
        this.regions = regions;
    }
}
