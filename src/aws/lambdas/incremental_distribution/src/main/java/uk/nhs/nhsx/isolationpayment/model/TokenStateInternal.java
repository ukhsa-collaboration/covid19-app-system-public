package uk.nhs.nhsx.isolationpayment.model;

public enum TokenStateInternal {
    INT_CREATED("created"),
    INT_UPDATED("valid");

    TokenStateInternal(String value) {
        this.value = value;
    }

    public final String value;
}
