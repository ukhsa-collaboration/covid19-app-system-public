package uk.nhs.nhsx.keyfederation.download;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ExposureDownload {
    
    public final String keyData;
    public final int rollingStartNumber;
    public final int transmissionRiskLevel;
    public final int rollingPeriod;
    public final String origin;
    public final List<String> regions;

    @JsonCreator
    public ExposureDownload(@JsonProperty String keyData,
                            @JsonProperty int rollingStartNumber,
                            @JsonProperty int transmissionRiskLevel,
                            @JsonProperty int rollingPeriod,
                            @JsonProperty String origin,
                            @JsonProperty List<String> regions) {
        this.keyData = keyData;
        this.rollingStartNumber = rollingStartNumber;
        this.transmissionRiskLevel = transmissionRiskLevel;
        this.rollingPeriod = rollingPeriod;
        this.origin = origin;
        this.regions = regions;
    }
}
