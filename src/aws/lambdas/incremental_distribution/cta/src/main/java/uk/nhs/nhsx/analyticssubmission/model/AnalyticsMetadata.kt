package uk.nhs.nhsx.analyticssubmission.model

class AnalyticsMetadata(
    @JvmField val postalDistrict: String,
    @JvmField val deviceModel: String,
    @JvmField val operatingSystemVersion: String,
    @JvmField val latestApplicationVersion: String,
    @JvmField val localAuthority: String?
)
