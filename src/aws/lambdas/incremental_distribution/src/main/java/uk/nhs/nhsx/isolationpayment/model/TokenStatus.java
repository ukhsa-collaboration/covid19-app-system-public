package uk.nhs.nhsx.isolationpayment.model;

public enum TokenStatus {
    CREATED("created"),
    VALID("valid"),
    CONSUMED("consumed"),
    INVALID("invalid");

    TokenStatus(String value) {
        this.value = value;
    }

    public final String value;
}
