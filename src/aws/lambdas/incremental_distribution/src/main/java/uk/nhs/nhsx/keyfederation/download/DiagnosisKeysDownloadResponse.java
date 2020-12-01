package uk.nhs.nhsx.keyfederation.download;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class DiagnosisKeysDownloadResponse {

    public final String batchTag;
    public final List<ExposureDownload> exposures;

    @JsonCreator
    public DiagnosisKeysDownloadResponse(@JsonProperty String batchTag,
                                         @JsonProperty List<ExposureDownload> exposures) {
        this.batchTag = batchTag;
        this.exposures = exposures;
    }
}
