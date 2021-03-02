package uk.nhs.nhsx.pubdash.datasets

import kotlin.random.Random

class FakeDataSet(
    private val source: AnalyticsDataSet,
    private val random: Random = Random.Default
) : AnalyticsDataSet {

    override fun countryAgnosticDataset(): CountryAgnosticDataset =
        CountryAgnosticDataset(
            data = source.countryAgnosticDataset().data.map { it.fake() }
        )

    override fun countrySpecificDataset(): CountrySpecificDataset =
        CountrySpecificDataset(
            data = source.countrySpecificDataset().data.map { it.fake() }
        )

    private fun CountryAgnosticRow.fake() =
        CountryAgnosticRow(
            weekEnding = weekEnding,
            downloads = random.nextInt(100000, 1000000),
            posters = random.nextInt(100, 100000),
            riskyVenues = random.nextInt(1, 100)
        )

    private fun CountrySpecificRow.fake() =
        CountrySpecificRow(
            weekEnding = weekEnding,
            countryEnglish = countryEnglish,
            countryWelsh = countryWelsh,
            contactTracingAlerts = random.nextInt(100, 100000),
            checkIns = random.nextInt(100, 100000),
            positiveTestResults = random.nextInt(100, 100000),
            negativeTestResults = random.nextInt(100, 100000),
            symptomsReported = random.nextInt(100, 100000)
        )
}
