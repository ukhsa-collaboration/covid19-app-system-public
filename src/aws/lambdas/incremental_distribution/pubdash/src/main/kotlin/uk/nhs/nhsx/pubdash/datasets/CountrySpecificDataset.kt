package uk.nhs.nhsx.pubdash.datasets

import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.pubdash.CsvS3Object
import java.time.LocalDate

data class CountrySpecificDataset(val data: List<CountrySpecificRow>) : CsvS3Object {

    override fun objectKey(): ObjectKey = ObjectKey.of("data/covid19_app_country_specific_dataset.csv")

    override fun csv(): String = """
            |"Week Ending (Wythnos Yn Gorffen)","Country","Wlad","Contact Tracing Alert (Hysbysiadau Olrhain Cyswllt)","Check-Ins (Cofrestriadau)","Positive Test Results Linked to App (Canlyniadau prawf positif)","Negative Test Results Linked to App (Canlyniadau prawf negatif)","Symptoms Reported (Symptomau a adroddwyd)"
            |${data.joinToString("\n") { csvRowFrom(it) }}
        """.trimMargin()

    private fun csvRowFrom(it: CountrySpecificRow) =
        """"${it.weekEnding}","${it.countryEnglish}","${it.countryWelsh}",${it.contactTracingAlerts},${it.checkIns},${it.positiveTestResults},${it.negativeTestResults},${it.symptomsReported}"""
}


data class CountrySpecificRow(
    val weekEnding: LocalDate,
    val countryEnglish: String,
    val countryWelsh: String,
    val contactTracingAlerts: Int,
    val checkIns: Int,
    val positiveTestResults: Int,
    val negativeTestResults: Int,
    val symptomsReported: Int
)
