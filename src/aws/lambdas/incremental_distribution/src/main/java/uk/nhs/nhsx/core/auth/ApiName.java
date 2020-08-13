package uk.nhs.nhsx.core.auth;

public enum ApiName {
    Mobile("mobile"), //do not rename -> dependency -> tools/build/lib/nhsx/tasks/secret.rb
    HighRiskPostCodeUpload("highRiskPostCodeUpload"), //do not rename -> dependency -> tools/build/lib/nhsx/tasks/secret.rb
    TestResultUpload("testResultUpload"), //do not rename -> dependency -> tools/build/lib/nhsx/tasks/secret.rb
    HighRiskVenuesUpload("highRiskVenuesCodeUpload"); //do not rename -> dependency -> tools/build/lib/nhsx/tasks/secret.rb

    public final String name;

    ApiName(String name) {
        this.name = name;
    }
}
