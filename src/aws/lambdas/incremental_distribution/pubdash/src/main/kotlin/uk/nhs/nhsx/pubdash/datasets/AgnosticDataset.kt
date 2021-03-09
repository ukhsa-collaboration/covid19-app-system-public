package uk.nhs.nhsx.pubdash.datasets

import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.pubdash.CsvS3Object
import java.time.LocalDate

data class AgnosticDataset(val data: List<AgnosticDatasetRow>) : CsvS3Object {

    override fun objectKey(): ObjectKey = ObjectKey.of("data/covid19_app_country_agnostic_dataset.csv")

    override fun csv(): String = """
            |"Week ending (Wythnos yn gorffen)","Number of app downloads (Nifer o lawrlwythiadau ap)","Number of NHS QR posters created (Nifer o bosteri cod QR y GIG a grëwyd)","Number of 'at risk' venues triggering venue alerts (Nifer o leoliadau 'dan risg')"
            |${sortedData().joinToString("\n") { csvRowFrom(it) }}
        """.trimMargin()

    private fun sortedData() = data.sortedWith(compareBy { it.weekEnding })

    private fun csvRowFrom(it: AgnosticDatasetRow) =
        """"${it.weekEnding}",${it.downloads},${it.posters},${it.riskyVenues}"""
}


data class AgnosticDatasetRow(
    val weekEnding: LocalDate,
    val downloads: Long,
    val posters: Long,
    val riskyVenues: Long
)
