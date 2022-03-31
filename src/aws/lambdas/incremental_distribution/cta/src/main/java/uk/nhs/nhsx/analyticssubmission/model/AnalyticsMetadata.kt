package uk.nhs.nhsx.analyticssubmission.model

data class AnalyticsMetadata(
    val postalDistrict: String,
    val deviceModel: String,
    val operatingSystemVersion: String,
    val latestApplicationVersion: String,
    val localAuthority: String?
)
