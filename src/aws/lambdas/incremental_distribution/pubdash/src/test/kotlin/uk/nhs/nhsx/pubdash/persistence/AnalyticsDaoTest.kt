package uk.nhs.nhsx.pubdash.persistence

import com.amazonaws.services.athena.model.Datum
import com.amazonaws.services.athena.model.GetQueryResultsResult
import com.amazonaws.services.athena.model.ResultSet
import com.amazonaws.services.athena.model.Row
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.pubdash.QueryId
import uk.nhs.nhsx.pubdash.QueryResult
import uk.nhs.nhsx.pubdash.datasets.AgnosticDataset
import uk.nhs.nhsx.pubdash.datasets.AgnosticDatasetRow
import uk.nhs.nhsx.pubdash.datasets.CountryDataset
import uk.nhs.nhsx.pubdash.datasets.CountryDatasetRow
import uk.nhs.nhsx.pubdash.datasets.LocalAuthorityDataset
import uk.nhs.nhsx.pubdash.datasets.LocalAuthorityDatasetRow
import uk.nhs.nhsx.pubdash.fakes.FakeDbClient
import java.time.LocalDate

class AnalyticsDaoTest {

    private val workspace = "some-workspace"
    private val schema = "analytics_db"
    private val tableAppStore = "analytics_app_store"
    private val tableQrPosters = "analytics_qr_posters"
    private val tableAnalyticsMobile = "analytics_mobile"
    private val tablePostcodeLookup = "analytics_postcode_demographic_geographic_lookup"
    private val queryId = QueryId("some-query-id")

    @Test
    fun `async agnostic dataset sql contains correct workspace and returns queryId`() {
        val dbClient = FakeDbClient(listOf(queryId))
        val dao = AnalyticsDao(workspace, dbClient)

        assertThat(dao.startAgnosticDatasetQueryAsync()).isEqualTo(queryId)
        assertThat(dbClient.submittedSqlQueries()).hasSize(1)

        val sql = dbClient.lastSubmittedSqlQuery()
        assertThat(sql).contains(sqlFrom(tableAppStore))
        assertThat(sql).contains(sqlFrom(tableQrPosters))
    }

    @Test
    fun `async country dataset sql contains correct workspace and returns queryId`() {
        val dbClient = FakeDbClient(listOf(queryId))
        val dao = AnalyticsDao(workspace, dbClient)

        assertThat(dao.startLocalAuthorityDatasetQueryAsync()).isEqualTo(queryId)
        assertThat(dbClient.submittedSqlQueries()).hasSize(1)

        val sql = dbClient.lastSubmittedSqlQuery()
        assertThat(sql).contains(sqlFrom(tableAnalyticsMobile))
        assertThat(sql).contains(sqlFrom(tablePostcodeLookup))
    }

    @Test
    fun `async local authority dataset sql contains correct workspace and returns queryId`() {
        val dbClient = FakeDbClient(listOf(queryId))
        val dao = AnalyticsDao(workspace, dbClient)

        assertThat(dao.startLocalAuthorityDatasetQueryAsync()).isEqualTo(queryId)
        assertThat(dbClient.submittedSqlQueries()).hasSize(1)

        val sql = dbClient.lastSubmittedSqlQuery()
        assertThat(sql).contains(sqlFrom(tableAnalyticsMobile))
        assertThat(sql).contains(sqlFrom(tablePostcodeLookup))
    }

    private fun sqlFrom(table: String): String = """"some-workspace_$schema"."some-workspace_$table""""

    @Test
    fun `maps agnostic dataset query results when query finishes`() {
        val finished = QueryResult.Finished(agnosticSampleResultSet())
        val waiting = QueryResult.Waiting<GetQueryResultsResult>()
        val dbClient = FakeDbClient(listOf(queryId), mapOf(queryId to listOf(waiting, finished)))
        val dao = AnalyticsDao(workspace, dbClient)

        assertThat(dao.agnosticDataset(queryId)).isInstanceOf(QueryResult.Waiting::class.java)
        assertThat(dao.agnosticDataset(queryId)).isEqualTo(
            QueryResult.Finished(
                AgnosticDataset(
                    listOf(
                        AgnosticDatasetRow(
                            weekEnding = LocalDate.parse("2021-01-01"),
                            downloads = 1,
                            riskyVenues = 2,
                            posters = 3,
                        ),
                        AgnosticDatasetRow(
                            weekEnding = LocalDate.parse("2021-01-02"),
                            downloads = 4,
                            riskyVenues = 5,
                            posters = 6,
                        )
                    )
                )
            )
        )
    }

    @Test
    fun `maps  country dataset query results when query finishes`() {
        val finished = QueryResult.Finished(countrySampleResultSet())
        val waiting = QueryResult.Waiting<GetQueryResultsResult>()
        val dbClient = FakeDbClient(listOf(queryId), mapOf(queryId to listOf(waiting, finished)))
        val dao = AnalyticsDao(workspace, dbClient)

        assertThat(dao.countryDataset(queryId)).isInstanceOf(QueryResult.Waiting::class.java)
        assertThat(dao.countryDataset(queryId)).isEqualTo(
            QueryResult.Finished(
                CountryDataset(
                    listOf(
                        CountryDatasetRow(
                            weekEnding = LocalDate.parse("2021-01-01"),
                            countryEnglish = "England",
                            countryWelsh = "Lloegr",
                            checkIns = 1,
                            contactTracingAlerts = 2,
                            negativeTestResults = 3,
                            positiveTestResults = 4,
                            symptomsReported = 5
                        ),
                        CountryDatasetRow(
                            weekEnding = LocalDate.parse("2021-01-01"),
                            countryEnglish = "Wales",
                            countryWelsh = "Cymru",
                            checkIns = 1,
                            contactTracingAlerts = 2,
                            negativeTestResults = 3,
                            positiveTestResults = 4,
                            symptomsReported = 5
                        )
                    )
                )
            )
        )
    }

    @Test
    fun `maps local authority query results when query finishes`() {
        val finished = QueryResult.Finished(localAuthoritySampleResultSet())
        val waiting = QueryResult.Waiting<GetQueryResultsResult>()
        val dbClient = FakeDbClient(listOf(queryId), mapOf(queryId to listOf(waiting, finished)))
        val dao = AnalyticsDao(workspace, dbClient)

        assertThat(dao.localAuthorityDataset(queryId)).isInstanceOf(QueryResult.Waiting::class.java)
        assertThat(dao.localAuthorityDataset(queryId)).isEqualTo(
            QueryResult.Finished(
                LocalAuthorityDataset(
                    listOf(
                        LocalAuthorityDatasetRow(
                            weekEnding = LocalDate.parse("2021-01-01"),
                            localAuthority = "local authority 1",
                            checkIns = 1,
                            contactTracingAlerts = 2,
                            negativeTestResults = 3,
                            positiveTestResults = 4,
                            symptomsReported = 5
                        ),
                        LocalAuthorityDatasetRow(
                            weekEnding = LocalDate.parse("2021-01-02"),
                            localAuthority = "local authority 2",
                            checkIns = 1,
                            contactTracingAlerts = 2,
                            negativeTestResults = 3,
                            positiveTestResults = 4,
                            symptomsReported = 5
                        )
                    )
                )
            )
        )
    }

    private fun agnosticSampleResultSet(): GetQueryResultsResult =
        GetQueryResultsResult()
            .withResultSet(
                ResultSet().withRows(
                    listOf(
                        rowWithHeader(),
                        rowWith("2021-01-01", "1", "2", "3"),
                        rowWith("2021-01-02", "4", "5", "6")
                    )
                )
            )

    private fun countrySampleResultSet(): GetQueryResultsResult =
        GetQueryResultsResult()
            .withResultSet(
                ResultSet().withRows(
                    listOf(
                        rowWithHeader(),
                        rowWith("2021-01-01", "England", "Lloegr", "1", "2", "3", "4", "5"),
                        rowWith("2021-01-01", "Wales", "Cymru", "1", "2", "3", "4", "5"),
                    )
                )
            )

    private fun localAuthoritySampleResultSet(): GetQueryResultsResult =
        GetQueryResultsResult()
            .withResultSet(
                ResultSet().withRows(
                    listOf(
                        rowWithHeader(),
                        rowWith("2021-01-01", "local authority 1", "1", "2", "3", "4", "5"),
                        rowWith("2021-01-02", "local authority 2", "1", "2", "3", "4", "5"),
                    )
                )
            )

    private fun rowWithHeader() =
        Row().withData(Datum().withVarCharValue("header row - ignore"))

    private fun rowWith(vararg values: String) =
        Row().withData(values.map { Datum().withVarCharValue(it) })
}

