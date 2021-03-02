package uk.nhs.nhsx.pubdash.datasets

import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.pubdash.CsvS3Object
import java.time.LocalDate

data class CountryAgnosticDataset(val data: List<CountryAgnosticRow>) : CsvS3Object {

    override fun objectKey(): ObjectKey = ObjectKey.of("data/covid19_app_country_agnostic_dataset.csv")

    override fun csv(): String = """
            |"Week Ending (Wythnos Yn Gorffen)","Number of App Downloads (Nifer o Lawrlwythiadau Ap)","Number of NHS QR Posters Created (Nifer o Bosteri Cod QR y GIG a Grëwyd)","Number of 'At Risk' Venues Triggering Venue Alerts (Nifer o Leoliadau ‘Dan Risg)"
            |${data.joinToString("\n") { csvRowFrom(it) }}
        """.trimMargin()

    private fun csvRowFrom(it: CountryAgnosticRow) =
        """"${it.weekEnding}",${it.downloads},${it.posters},${it.riskyVenues}"""
}


data class CountryAgnosticRow(
    val weekEnding: LocalDate,
    val downloads: Int,
    val posters: Int,
    val riskyVenues: Int
)
