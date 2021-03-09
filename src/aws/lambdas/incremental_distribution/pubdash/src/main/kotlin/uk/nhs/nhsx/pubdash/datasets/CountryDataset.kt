package uk.nhs.nhsx.pubdash.datasets

import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.pubdash.CsvS3Object
import java.time.LocalDate

data class CountryDataset(val data: List<CountryDatasetRow>) : CsvS3Object {

    override fun objectKey(): ObjectKey = ObjectKey.of("data/covid19_app_country_specific_dataset.csv")

    override fun csv(): String = """
            |"Week ending (Wythnos yn gorffen)","Country","Wlad","Contact tracing alert (Hysbysiadau olrhain cyswllt)","Check-ins (Cofrestriadau)","Positive test results linked to app (Canlyniadau prawf positif)","Negative test results linked to app (Canlyniadau prawf negatif)","Symptoms reported (Symptomau a adroddwyd)"
            |${sortedData().joinToString("\n") { csvRowFrom(it) }}
        """.trimMargin()

    private fun sortedData() = data.sortedWith(compareBy({ it.weekEnding }, { it.countryEnglish }))

    private fun csvRowFrom(it: CountryDatasetRow) =
        """"${it.weekEnding}","${it.countryEnglish}","${it.countryWelsh}",${it.contactTracingAlerts},${it.checkIns},${it.positiveTestResults},${it.negativeTestResults},${it.symptomsReported}"""
}


data class CountryDatasetRow(
    val weekEnding: LocalDate,
    val countryEnglish: String,
    val countryWelsh: String,
    val contactTracingAlerts: Long,
    val checkIns: Long,
    val positiveTestResults: Long,
    val negativeTestResults: Long,
    val symptomsReported: Long
)
