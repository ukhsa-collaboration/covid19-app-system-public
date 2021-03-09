package uk.nhs.nhsx.analyticssubmission.model;


public class AnalyticsMetadata {

    public String postalDistrict;
    public String deviceModel;
    public String operatingSystemVersion;
    public String latestApplicationVersion;
    public String localAuthority;

    public AnalyticsMetadata() {
    }

    public AnalyticsMetadata(String postalDistrict,
                             String deviceModel,
                             String operatingSystemVersion,
                             String latestApplicationVersion,
                             String localAuthority) {
        this.postalDistrict = postalDistrict;
        this.deviceModel = deviceModel;
        this.operatingSystemVersion = operatingSystemVersion;
        this.latestApplicationVersion = latestApplicationVersion;
        this.localAuthority = localAuthority;
    }
}
