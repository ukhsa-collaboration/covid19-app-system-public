package uk.nhs.nhsx.pubdash.datasets

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.pubdash.TestData.localAuthorityDatasetCsvHeader
import java.time.LocalDate

class LocalAuthorityDatasetTest {

    @Test
    fun `has correct object key`() {
        val dataSet = LocalAuthorityDataset(emptyList())
        assertThat(dataSet.objectKey()).isEqualTo(ObjectKey.of("data/covid19_app_data_by_local_authority.csv"))
    }

    @Test
    fun `handles no csv rows`() {
        val dataSet = LocalAuthorityDataset(emptyList())
        assertThat(dataSet.csv()).isEqualTo(
            """
            |$localAuthorityDatasetCsvHeader
            |
        """.trimMargin()
        )
    }

    @Test
    fun `matches csv content`() {
        val dataSet = LocalAuthorityDataset(
            listOf(
                LocalAuthorityDatasetRow(LocalDate.parse("2020-11-01"), "la-1", 1, 2, 3, 4, 5),
                LocalAuthorityDatasetRow(LocalDate.parse("2020-11-02"), "la-2", 6, 7, 8, 9, 10)
            )
        )
        assertThat(dataSet.csv()).isEqualTo(
            """
            |$localAuthorityDatasetCsvHeader
            |"2020-11-01","la-1",1,2,3,4,5
            |"2020-11-02","la-2",6,7,8,9,10
        """.trimMargin()
        )
    }

    @Test
    fun `csv is correctly sorted`() {
        val dataSet = LocalAuthorityDataset(
            listOf(
                LocalAuthorityDatasetRow(LocalDate.parse("2020-11-04"), "la-3", 3, 3, 3, 3, 3),
                LocalAuthorityDatasetRow(LocalDate.parse("2020-10-01"), "la-1", 1, 1, 1, 1, 1),
                LocalAuthorityDatasetRow(LocalDate.parse("2020-10-02"), "la-2", 2, 2, 2, 2, 2),
            )
        )
        assertThat(dataSet.csv()).isEqualTo(
            """
            |$localAuthorityDatasetCsvHeader
            |"2020-10-01","la-1",1,1,1,1,1
            |"2020-10-02","la-2",2,2,2,2,2
            |"2020-11-04","la-3",3,3,3,3,3
        """.trimMargin()
        )
    }
}
