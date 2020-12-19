package uk.nhs.nhsx.isolationpayment.model;

public enum TokenStateExternal {
    EXT_VALID("valid"),
    EXT_INVALID("invalid"),
    EXT_CONSUMED("consumed");

    TokenStateExternal(String value) {
        this.value = value;
    }

    public final String value;
}
