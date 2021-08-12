package uk.nhs.nhsx.pubdash.persistence

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import uk.nhs.nhsx.pubdash.QueryId
import uk.nhs.nhsx.pubdash.QueryResult
import uk.nhs.nhsx.pubdash.fakes.FakeDbClient

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
        val dao = AnalyticsDao(workspace, dbClient,tableAnalyticsMobile)

        expectThat(dao.startAgnosticDatasetQueryAsync()).isEqualTo(queryId)
        expectThat(dbClient.submittedSqlQueries()).hasSize(1)

        val sql = dbClient.lastSubmittedSqlQuery()
        expectThat(sql).contains(sqlFrom(tableAppStore))
        expectThat(sql).contains(sqlFrom(tableQrPosters))
    }

    @Test
    fun `async country dataset sql contains correct workspace and returns queryId`() {
        val dbClient = FakeDbClient(listOf(queryId))
        val dao = AnalyticsDao(workspace, dbClient,tableAnalyticsMobile)

        expectThat(dao.startLocalAuthorityDatasetQueryAsync()).isEqualTo(queryId)
        expectThat(dbClient.submittedSqlQueries()).hasSize(1)

        val sql = dbClient.lastSubmittedSqlQuery()
        expectThat(sql).contains(sqlFrom(tableAnalyticsMobile))
        expectThat(sql).contains(sqlFrom(tablePostcodeLookup))
    }

    @Test
    fun `async local authority dataset sql contains correct workspace and returns queryId`() {
        val dbClient = FakeDbClient(listOf(queryId))
        val dao = AnalyticsDao(workspace, dbClient,tableAnalyticsMobile)

        expectThat(dao.startLocalAuthorityDatasetQueryAsync()).isEqualTo(queryId)
        expectThat(dbClient.submittedSqlQueries()).hasSize(1)

        val sql = dbClient.lastSubmittedSqlQuery()
        expectThat(sql).contains(sqlFrom(tableAnalyticsMobile))
        expectThat(sql).contains(sqlFrom(tablePostcodeLookup))
    }

    private fun sqlFrom(table: String): String = """"some-workspace_$schema"."some-workspace_$table""""

    @Test
    fun `checks query state`() {
        val finished = QueryResult.Finished(Unit)
        val waiting = QueryResult.Waiting<Unit>()
        val dbClient = FakeDbClient(listOf(queryId), mapOf(queryId to listOf(waiting, finished)))
        val dao = AnalyticsDao(workspace, dbClient,tableAnalyticsMobile)

        expectThat(dao.checkQueryState(queryId)).isA<QueryResult.Waiting<Unit>>()
        expectThat(dao.checkQueryState(queryId)).isEqualTo(QueryResult.Finished(Unit))
    }

}

