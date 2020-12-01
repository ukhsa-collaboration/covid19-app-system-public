package uk.nhs.nhsx.circuitbreakers;

public enum ApprovalStatus {

    YES("yes"),
    NO("no"),
    PENDING("pending");

    private final String name;

    ApprovalStatus(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
