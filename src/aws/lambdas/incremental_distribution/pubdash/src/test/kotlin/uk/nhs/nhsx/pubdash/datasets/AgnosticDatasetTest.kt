package uk.nhs.nhsx.pubdash.datasets

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.pubdash.TestData.countryAgnosticDatasetCsvHeader
import java.time.LocalDate

class AgnosticDatasetTest {

    @Test
    fun `has correct object key`() {
        val dataSet = AgnosticDataset(emptyList())
        assertThat(dataSet.objectKey()).isEqualTo(ObjectKey.of("data/covid19_app_country_agnostic_dataset.csv"))
    }

    @Test
    fun `handles no csv rows`() {
        val dataSet = AgnosticDataset(emptyList())
        assertThat(dataSet.csv()).isEqualTo(
            """
            |$countryAgnosticDatasetCsvHeader
            |
        """.trimMargin()
        )
    }

    @Test
    fun `matches csv content`() {
        val dataSet = AgnosticDataset(
            listOf(
                AgnosticDatasetRow(LocalDate.parse("2020-11-01"), 1, 2, 3),
                AgnosticDatasetRow(LocalDate.parse("2020-11-02"), 4, 5, 6)
            )
        )
        assertThat(dataSet.csv()).isEqualTo(
            """
            |$countryAgnosticDatasetCsvHeader
            |"2020-11-01",1,2,3
            |"2020-11-02",4,5,6
        """.trimMargin()
        )
    }

    @Test
    fun `csv is correctly sorted`() {
        val dataSet = AgnosticDataset(
            listOf(
                AgnosticDatasetRow(LocalDate.parse("2020-11-03"), 2, 2, 2),
                AgnosticDatasetRow(LocalDate.parse("2020-10-01"), 0, 0, 0),
                AgnosticDatasetRow(LocalDate.parse("2020-11-02"), 1, 1, 1)
            )
        )
        assertThat(dataSet.csv()).isEqualTo(
            """
            |$countryAgnosticDatasetCsvHeader
            |"2020-10-01",0,0,0
            |"2020-11-02",1,1,1
            |"2020-11-03",2,2,2
        """.trimMargin()
        )
    }
}
