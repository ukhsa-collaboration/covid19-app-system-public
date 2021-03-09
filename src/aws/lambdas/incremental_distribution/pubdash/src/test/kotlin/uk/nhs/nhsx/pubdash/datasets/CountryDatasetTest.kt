package uk.nhs.nhsx.pubdash.datasets

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.pubdash.TestData.countrySpecificDatasetCsvHeader
import java.time.LocalDate

class CountryDatasetTest {

    @Test
    fun `has correct object key`() {
        val dataSet = CountryDataset(emptyList())
        assertThat(dataSet.objectKey()).isEqualTo(ObjectKey.of("data/covid19_app_country_specific_dataset.csv"))
    }

    @Test
    fun `handles no csv rows`() {
        val dataSet = CountryDataset(emptyList())
        assertThat(dataSet.csv()).isEqualTo(
            """
            |$countrySpecificDatasetCsvHeader
            |
        """.trimMargin()
        )
    }

    @Test
    fun `matches csv content`() {
        val dataSet = CountryDataset(
            listOf(
                CountryDatasetRow(LocalDate.parse("2020-11-01"), "lang 1", "lang 2", 1, 2, 3, 4, 5),
                CountryDatasetRow(LocalDate.parse("2020-11-02"), "lang 3", "lang 4", 6, 7, 8, 9, 10)
            )
        )
        assertThat(dataSet.csv()).isEqualTo(
            """
            |$countrySpecificDatasetCsvHeader
            |"2020-11-01","lang 1","lang 2",1,2,3,4,5
            |"2020-11-02","lang 3","lang 4",6,7,8,9,10
        """.trimMargin()
        )
    }

    @Test
    fun `csv is correctly sorted`() {
        val dataSet = CountryDataset(
            listOf(
                CountryDatasetRow(LocalDate.parse("2020-11-04"), "England", "Lloegr", 4, 4, 4, 4, 4),
                CountryDatasetRow(LocalDate.parse("2020-10-01"), "Wales", "Cymru", 1, 1, 1, 1, 1),
                CountryDatasetRow(LocalDate.parse("2020-11-01"), "Wales", "Cymru", 3, 3, 3, 3, 3),
                CountryDatasetRow(LocalDate.parse("2020-11-01"), "England", "Lloegr", 2, 2, 2, 2, 2),
                CountryDatasetRow(LocalDate.parse("2020-10-01"), "England", "Lloegr", 0, 0, 0, 0, 0),
                CountryDatasetRow(LocalDate.parse("2020-11-04"), "Wales", "Cymru", 5, 5, 5, 5, 5),
            )
        )
        assertThat(dataSet.csv()).isEqualTo(
            """
            |$countrySpecificDatasetCsvHeader
            |"2020-10-01","England","Lloegr",0,0,0,0,0
            |"2020-10-01","Wales","Cymru",1,1,1,1,1
            |"2020-11-01","England","Lloegr",2,2,2,2,2
            |"2020-11-01","Wales","Cymru",3,3,3,3,3
            |"2020-11-04","England","Lloegr",4,4,4,4,4
            |"2020-11-04","Wales","Cymru",5,5,5,5,5
        """.trimMargin()
        )
    }
}
