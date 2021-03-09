package uk.nhs.nhsx.pubdash.datasets

import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.pubdash.CsvS3Object
import java.time.LocalDate

data class LocalAuthorityDataset(val data: List<LocalAuthorityDatasetRow>) : CsvS3Object {

    override fun objectKey(): ObjectKey = ObjectKey.of("data/covid19_app_data_by_local_authority.csv")

    override fun csv(): String = """
            |"Week ending (Wythnos yn gorffen)","Local authority (Awdurdod lleol)","Contact tracing alert (Hysbysiadau olrhain cyswllt)","Check-ins (Cofrestriadau)","Positive test results linked to app (Canlyniadau prawf positif)","Negative test results linked to app (Canlyniadau prawf negatif)","Symptoms reported (Symptomau a adroddwyd)"
            |${sortedData().joinToString("\n") { csvRowFrom(it) }}
        """.trimMargin()

    private fun sortedData() = data.sortedWith(compareBy { it.weekEnding })

    private fun csvRowFrom(it: LocalAuthorityDatasetRow) =
        """"${it.weekEnding}","${it.localAuthority}",${it.contactTracingAlerts},${it.checkIns},${it.positiveTestResults},${it.negativeTestResults},${it.symptomsReported}"""
}


data class LocalAuthorityDatasetRow(
    val weekEnding: LocalDate,
    val localAuthority: String,
    val contactTracingAlerts: Long,
    val checkIns: Long,
    val positiveTestResults: Long,
    val negativeTestResults: Long,
    val symptomsReported: Long
)
