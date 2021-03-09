package uk.nhs.nhsx.pubdash.datasets

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.pubdash.CsvS3Object
import uk.nhs.nhsx.pubdash.QueryId
import uk.nhs.nhsx.pubdash.QueryResult
import java.time.LocalDate
import kotlin.random.Random

class FakeDataSetTest {

    private val source = object : AnalyticsSource {
        override fun agnosticDataset(queryId: QueryId): QueryResult<CsvS3Object> =
            QueryResult.Finished(
                AgnosticDataset(
                    listOf(AgnosticDatasetRow(LocalDate.parse("2020-11-01"), 1, 2, 3))
                )
            )

        override fun countryDataset(queryId: QueryId): QueryResult<CsvS3Object> =
            QueryResult.Finished(
                CountryDataset(
                    listOf(CountryDatasetRow(LocalDate.parse("2020-11-02"), "lang 1", "lang 2", 1, 2, 3, 4, 5))
                )
            )

        override fun localAuthorityDataset(queryId: QueryId): QueryResult<CsvS3Object> =
            QueryResult.Finished(
                LocalAuthorityDataset(
                    listOf(LocalAuthorityDatasetRow(LocalDate.parse("2020-11-03"), "local-auth", 1, 2, 3, 4, 5))
                )
            )

        override fun startAgnosticDatasetQueryAsync(): QueryId {
            TODO("Not yet implemented")
        }

        override fun startCountryDatasetQueryAsync(): QueryId {
            TODO("Not yet implemented")
        }

        override fun startLocalAuthorityDatasetQueryAsync(): QueryId {
            TODO("Not yet implemented")
        }
    }

    private val queryId = QueryId("some-id")

    @Test
    fun `randomizes country agnostic dataset`() {
        val dataset = FakeSource(source, random = Random(1))
        assertThat(dataset.agnosticDataset(queryId)).isEqualTo(
            QueryResult.Finished(
                AgnosticDataset(
                    listOf(AgnosticDatasetRow(LocalDate.parse("2020-11-01"), 645368, 12230, 33))
                )
            )
        )
    }

    @Test
    fun `randomizes country specific dataset`() {
        val dataset = FakeSource(source, random = Random(1))
        assertThat(dataset.countryDataset(queryId)).isEqualTo(
            QueryResult.Finished(
                CountryDataset(
                    listOf(
                        CountryDatasetRow(
                            LocalDate.parse("2020-11-02"), "lang 1", "lang 2", 99968, 12230, 14811, 67013, 65481
                        )
                    )
                )
            )
        )
    }

    @Test
    fun `randomizes local authority specific dataset`() {
        val dataset = FakeSource(source, random = Random(1))
        assertThat(dataset.localAuthorityDataset(queryId)).isEqualTo(
            QueryResult.Finished(
                LocalAuthorityDataset(
                    listOf(
                        LocalAuthorityDatasetRow(
                            LocalDate.parse("2020-11-03"), "local-auth", 99968, 12230, 14811, 67013, 65481
                        )
                    )
                )
            )
        )
    }
}
