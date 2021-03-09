package uk.nhs.nhsx.core.auth

// maps to api name defined in -> tools/build/lib/nhsx/helpers/target.rb
enum class ApiName(val keyName: String) {
    Mobile("mobile"),
    Health("health"),
    HighRiskPostCodeUpload("highRiskPostCodeUpload"),
    TestResultUpload("testResultUpload"),
    HighRiskVenuesUpload("highRiskVenuesCodeUpload"),
    IsolationPayment("isolationPayment");
}
