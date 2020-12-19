package uk.nhs.nhsx.analyticssubmission.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public class AnalyticsMetadata {

    public final String postalDistrict;
    public final String deviceModel;
    public final String operatingSystemVersion;
    public final String latestApplicationVersion;
    public String localAuthority;

    @JsonCreator
    public AnalyticsMetadata(String postalDistrict,
                             String deviceModel,
                             String operatingSystemVersion,
                             String latestApplicationVersion) {
        this.postalDistrict = postalDistrict;
        this.deviceModel = deviceModel;
        this.operatingSystemVersion = operatingSystemVersion;
        this.latestApplicationVersion = latestApplicationVersion;
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
