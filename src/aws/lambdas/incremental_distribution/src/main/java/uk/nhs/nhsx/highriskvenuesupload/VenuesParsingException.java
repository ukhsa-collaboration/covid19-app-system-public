package uk.nhs.nhsx.highriskvenuesupload;

public class VenuesParsingException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public VenuesParsingException(String message) {
        super(message);
    }
}
