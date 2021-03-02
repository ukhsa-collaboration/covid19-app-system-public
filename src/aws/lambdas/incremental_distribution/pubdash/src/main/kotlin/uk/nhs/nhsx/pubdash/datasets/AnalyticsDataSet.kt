package uk.nhs.nhsx.pubdash.datasets

interface AnalyticsDataSet {
    fun countryAgnosticDataset(): CountryAgnosticDataset
    fun countrySpecificDataset(): CountrySpecificDataset
}
