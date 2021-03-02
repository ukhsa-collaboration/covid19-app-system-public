package uk.nhs.nhsx.pubdash.datasets

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import java.time.LocalDate

class CountryAgnosticDatasetTest {

    @Test
    fun `has correct object key`() {
        val dataSet = CountryAgnosticDataset(emptyList())
        assertThat(dataSet.objectKey()).isEqualTo(ObjectKey.of("data/covid19_app_country_agnostic_dataset.csv"))
    }

    @Test
    fun `handles no csv rows`() {
        val dataSet = CountryAgnosticDataset(emptyList())
        assertThat(dataSet.csv()).isEqualTo(
            """
            |"Week Ending (Wythnos Yn Gorffen)","Number of App Downloads (Nifer o Lawrlwythiadau Ap)","Number of NHS QR Posters Created (Nifer o Bosteri Cod QR y GIG a Grëwyd)","Number of 'At Risk' Venues Triggering Venue Alerts (Nifer o Leoliadau ‘Dan Risg)"
            |
        """.trimMargin()
        )
    }

    @Test
    fun `matches csv content`() {
        val dataSet = CountryAgnosticDataset(
            listOf(
                CountryAgnosticRow(LocalDate.parse("2020-11-01"), 1, 2, 3),
                CountryAgnosticRow(LocalDate.parse("2020-11-02"), 4, 5, 6)
            )
        )
        assertThat(dataSet.csv()).isEqualTo(
            """
            |"Week Ending (Wythnos Yn Gorffen)","Number of App Downloads (Nifer o Lawrlwythiadau Ap)","Number of NHS QR Posters Created (Nifer o Bosteri Cod QR y GIG a Grëwyd)","Number of 'At Risk' Venues Triggering Venue Alerts (Nifer o Leoliadau ‘Dan Risg)"
            |"2020-11-01",1,2,3
            |"2020-11-02",4,5,6
        """.trimMargin()
        )
    }
}
