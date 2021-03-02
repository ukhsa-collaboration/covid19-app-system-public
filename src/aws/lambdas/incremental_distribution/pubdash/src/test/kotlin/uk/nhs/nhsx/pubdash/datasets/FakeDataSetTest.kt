package uk.nhs.nhsx.pubdash.datasets

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.random.Random

class FakeDataSetTest {

    private val source = object : AnalyticsDataSet {
        override fun countryAgnosticDataset() =
            CountryAgnosticDataset(
                listOf(CountryAgnosticRow(LocalDate.parse("2020-11-01"), 1, 2, 3))
            )

        override fun countrySpecificDataset() =
            CountrySpecificDataset(
                listOf(CountrySpecificRow(LocalDate.parse("2020-11-01"), "lang 1", "lang 2", 1, 2, 3, 4, 5))
            )
    }

    @Test
    fun `randomizes country agnostic dataset`() {
        val dataset = FakeDataSet(source, random = Random(1))
        assertThat(dataset.countryAgnosticDataset()).isEqualTo(
            CountryAgnosticDataset(listOf(CountryAgnosticRow(LocalDate.parse("2020-11-01"), 461965, 15376, 86)))
        )
    }

    @Test
    fun `randomizes country specific dataset`() {
        val dataset = FakeDataSet(source, random = Random(1))
        assertThat(dataset.countrySpecificDataset()).isEqualTo(
            CountrySpecificDataset(listOf(CountrySpecificRow(LocalDate.parse("2020-11-01"), "lang 1", "lang 2", 62365, 15376, 60152, 97138, 42649)))
        )
    }
}
