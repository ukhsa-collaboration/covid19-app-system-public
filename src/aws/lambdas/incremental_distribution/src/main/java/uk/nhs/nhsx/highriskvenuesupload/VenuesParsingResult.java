package uk.nhs.nhsx.highriskvenuesupload;

import java.util.Optional;

public class VenuesParsingResult {

    private String json;
    private String failure;

    private VenuesParsingResult() {
    }

    public static VenuesParsingResult ok(String json) {
        VenuesParsingResult result = new VenuesParsingResult();
        result.json = json;
        return result;
    }

    public static VenuesParsingResult failure(String failure) {
        VenuesParsingResult result = new VenuesParsingResult();
        result.failure = failure;
        return result;
    }

    public Optional<String> failureMaybe() {
        return Optional.ofNullable(failure);
    }

    public String jsonOrThrow() {
        return Optional
            .ofNullable(json)
            .orElseThrow(() -> new IllegalStateException("Parsing result json not found"));
    }
}
