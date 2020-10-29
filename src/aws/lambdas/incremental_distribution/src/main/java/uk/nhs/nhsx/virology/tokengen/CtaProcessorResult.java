package uk.nhs.nhsx.virology.tokengen;

import java.util.Map;

public abstract class CtaProcessorResult {

    public abstract Map<String, String> toResponse();

    public static class Success extends CtaProcessorResult {
        public final String zipFilename;
        private final String message;

        public Success(String zipFilename, String message) {
            this.zipFilename = zipFilename;
            this.message = message;
        }

        @Override
        public Map<String, String> toResponse() {
            return Map.of(
                "result", "success",
                "message", message,
                "filename", zipFilename
            );
        }
    }

    public static class Error extends CtaProcessorResult {
        public final String message;

        public Error(String message) {
            this.message = message;
        }

        @Override
        public Map<String, String> toResponse() {
            return Map.of(
                "result", "error",
                "message", message
            );
        }
    }
}
