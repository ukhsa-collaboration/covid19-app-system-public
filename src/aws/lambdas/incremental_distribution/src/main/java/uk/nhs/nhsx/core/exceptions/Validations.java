package uk.nhs.nhsx.core.exceptions;

import static uk.nhs.nhsx.core.exceptions.HttpStatusCode.UNPROCESSABLE_ENTITY_422;

public class Validations {
    
    public static void throwValidationErrorWith(String reason) {
        throw validationErrorWith(reason);
    }

    public static ApiResponseException validationErrorWith(String reason) {
        return new ApiResponseException(UNPROCESSABLE_ENTITY_422, validationMessageWith(reason));
    }

    private static String validationMessageWith(String reason) {
        return "validation error: " + reason;
    }
}