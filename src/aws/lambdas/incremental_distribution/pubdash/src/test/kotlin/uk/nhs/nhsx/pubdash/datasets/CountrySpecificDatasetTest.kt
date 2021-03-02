package uk.nhs.nhsx.pubdash.datasets

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import java.time.LocalDate

class CountrySpecificDatasetTest {

    @Test
    fun `has correct object key`() {
        val dataSet = CountrySpecificDataset(emptyList())
        assertThat(dataSet.objectKey()).isEqualTo(ObjectKey.of("data/covid19_app_country_specific_dataset.csv"))
    }

    @Test
    fun `handles no csv rows`() {
        val dataSet = CountrySpecificDataset(emptyList())
        assertThat(dataSet.csv()).isEqualTo(
            """
            |"Week Ending (Wythnos Yn Gorffen)","Country","Wlad","Contact Tracing Alert (Hysbysiadau Olrhain Cyswllt)","Check-Ins (Cofrestriadau)","Positive Test Results Linked to App (Canlyniadau prawf positif)","Negative Test Results Linked to App (Canlyniadau prawf negatif)","Symptoms Reported (Symptomau a adroddwyd)"
            |
        """.trimMargin()
        )
    }

    @Test
    fun `matches csv content`() {
        val dataSet = CountrySpecificDataset(
            listOf(
                CountrySpecificRow(LocalDate.parse("2020-11-01"), "lang 1", "lang 2", 1, 2, 3, 4, 5),
                CountrySpecificRow(LocalDate.parse("2020-11-02"), "lang 3", "lang 4", 6, 7, 8, 9, 10)
            )
        )
        assertThat(dataSet.csv()).isEqualTo(
            """
            |"Week Ending (Wythnos Yn Gorffen)","Country","Wlad","Contact Tracing Alert (Hysbysiadau Olrhain Cyswllt)","Check-Ins (Cofrestriadau)","Positive Test Results Linked to App (Canlyniadau prawf positif)","Negative Test Results Linked to App (Canlyniadau prawf negatif)","Symptoms Reported (Symptomau a adroddwyd)"
            |"2020-11-01","lang 1","lang 2",1,2,3,4,5
            |"2020-11-02","lang 3","lang 4",6,7,8,9,10
        """.trimMargin()
        )
    }
}
