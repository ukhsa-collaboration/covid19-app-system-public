package uk.nhs.nhsx.keyfederation.upload;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DiagnosisKeysUploadRequest {

    public final String batchTag;
    public final String payload;

    @JsonCreator
    public DiagnosisKeysUploadRequest(
        @JsonProperty String batchTag,
        @JsonProperty String payload) {
        this.batchTag = batchTag;
        this.payload = payload;
    }
}

