package uk.nhs.nhsx.analyticsexporter

interface AnalyticsFileExporterConfig {
    val s3DisallowedPrefixList: String
}
