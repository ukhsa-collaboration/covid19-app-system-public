package uk.nhs.nhsx.keyfederation.upload;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class InteropUploadResponse {
    public final String batchTag;
    public final int insertedExposures;

    @JsonCreator
    public InteropUploadResponse(
        @JsonProperty String batchTag,
        @JsonProperty int insertedExposures
    ) {
        this.batchTag = batchTag;
        this.insertedExposures = insertedExposures;
    }
}
