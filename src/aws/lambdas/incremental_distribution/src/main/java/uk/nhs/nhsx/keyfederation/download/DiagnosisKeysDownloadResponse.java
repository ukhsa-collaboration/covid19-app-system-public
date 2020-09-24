package uk.nhs.nhsx.keyfederation.download;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.nhs.nhsx.keyfederation.Exposure;

import java.util.List;

public class DiagnosisKeysDownloadResponse {
    public final String batchTag;
    public final List<Exposure> exposures;

    @JsonCreator
    public DiagnosisKeysDownloadResponse(
        @JsonProperty String batchTag,
        @JsonProperty List<Exposure> exposures
    ) {
        this.batchTag = batchTag;
        this.exposures = exposures;
    }
}
