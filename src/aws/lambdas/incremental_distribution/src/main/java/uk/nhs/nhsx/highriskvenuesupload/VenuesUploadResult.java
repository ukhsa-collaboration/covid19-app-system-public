package uk.nhs.nhsx.highriskvenuesupload;

public class VenuesUploadResult {

    enum ResultType {Ok, ValidationError}

    public final ResultType type;
    public final String message;

    private VenuesUploadResult(ResultType type, String message) {
        this.type = type;
        this.message = message;
    }

    public static VenuesUploadResult ok() {
        return new VenuesUploadResult(ResultType.Ok, "successfully uploaded");
    }

    public static VenuesUploadResult validationError(String message) {
        return new VenuesUploadResult(ResultType.ValidationError, message);
    }

}
