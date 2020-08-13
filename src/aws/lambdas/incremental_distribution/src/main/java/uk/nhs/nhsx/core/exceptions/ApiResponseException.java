package uk.nhs.nhsx.core.exceptions;

public class ApiResponseException extends RuntimeException {
  	private static final long serialVersionUID = 1L;
	
    public final HttpStatusCode statusCode;
    public ApiResponseException(HttpStatusCode statusCode) {
        super();
        this.statusCode = statusCode;
    }

    public ApiResponseException(HttpStatusCode statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public ApiResponseException(HttpStatusCode statusCode, String message, Exception cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public ApiResponseException(HttpStatusCode statusCode, Exception cause) {
        super(cause);
        this.statusCode = statusCode;
    }

}
