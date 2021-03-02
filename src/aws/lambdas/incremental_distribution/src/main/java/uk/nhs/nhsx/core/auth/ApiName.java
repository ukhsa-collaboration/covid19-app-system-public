package uk.nhs.nhsx.core.auth;

// maps to api name defined in -> tools/build/lib/nhsx/helpers/target.rb
public enum ApiName {
    Mobile("mobile"),
    Health("health"),
    HighRiskPostCodeUpload("highRiskPostCodeUpload"),
    TestResultUpload("testResultUpload"),
    HighRiskVenuesUpload("highRiskVenuesCodeUpload"),
    IsolationPayment("isolationPayment");

    public final String name;

    ApiName(String name) {
        this.name = name;
    }
}
