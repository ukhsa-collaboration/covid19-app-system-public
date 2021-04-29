package uk.nhs.nhsx.analyticsedge.persistence

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.analyticsedge.QueryId
import uk.nhs.nhsx.analyticsedge.QueryResult
import uk.nhs.nhsx.analyticsedge.fakes.FakeDbClient

class AnalyticsDaoTest {

    private val workspace = "some-workspace"
    private val schema = "analytics_db"
    private val tableAppStore = "analytics_app_store"
    private val tableQrPosters = "analytics_qr_posters"
    private val tableAnalyticsMobile = "analytics_mobile"
    private val tablePostcodeLookup = "analytics_postcode_demographic_geographic_lookup"
    private val queryId = QueryId("some-query-id")

    @Test
    fun `async adoption dataset sql contains correct workspace and returns queryId`() {
        val dbClient = FakeDbClient(listOf(queryId))
        val dao = AnalyticsDao(workspace, dbClient)

        assertThat(dao.startAdoptionDatasetQueryAsync()).isEqualTo(queryId)
        assertThat(dbClient.submittedSqlQueries()).hasSize(1)

        val sql = dbClient.lastSubmittedSqlQuery()
        assertThat(sql).contains(sqlFrom(tableAnalyticsMobile))
        assertThat(sql).contains(sqlFrom(tablePostcodeLookup))
    }

    @Test
    fun `async aggregate dataset sql contains correct workspace and returns queryId`() {
        val dbClient = FakeDbClient(listOf(queryId))
        val dao = AnalyticsDao(workspace, dbClient)

        assertThat(dao.startAggregateDatasetQueryAsync()).isEqualTo(queryId)
        assertThat(dbClient.submittedSqlQueries()).hasSize(1)

        val sql = dbClient.lastSubmittedSqlQuery()
        assertThat(sql).contains(sqlFrom(tableAppStore))
        assertThat(sql).contains(sqlFrom(tableQrPosters))
    }

    @Test
    fun `async enpic dataset sql contains correct workspace and returns queryId`() {
        val dbClient = FakeDbClient(listOf(queryId))
        val dao = AnalyticsDao(workspace, dbClient)

        assertThat(dao.startEnpicDatasetQueryAsync()).isEqualTo(queryId)
        assertThat(dbClient.submittedSqlQueries()).hasSize(1)

        val sql = dbClient.lastSubmittedSqlQuery()
        assertThat(sql).contains(sqlFrom(tableAnalyticsMobile))
        assertThat(sql).contains(sqlFrom(tablePostcodeLookup))
    }

    @Test
    fun `async isolation dataset sql contains correct workspace and returns queryId`() {
        val dbClient = FakeDbClient(listOf(queryId))
        val dao = AnalyticsDao(workspace, dbClient)

        assertThat(dao.startIsolationDatasetQueryAsync()).isEqualTo(queryId)
        assertThat(dbClient.submittedSqlQueries()).hasSize(1)

        val sql = dbClient.lastSubmittedSqlQuery()
        assertThat(sql).contains(sqlFrom(tableAnalyticsMobile))
        assertThat(sql).contains(sqlFrom(tablePostcodeLookup))
    }

    @Test
    fun `async poster dataset sql contains correct workspace and returns queryId`() {
        val dbClient = FakeDbClient(listOf(queryId))
        val dao = AnalyticsDao(workspace, dbClient)

        assertThat(dao.startPosterDatasetQueryAsync()).isEqualTo(queryId)
        assertThat(dbClient.submittedSqlQueries()).hasSize(1)

        val sql = dbClient.lastSubmittedSqlQuery()
        assertThat(sql).contains(sqlFrom(tableQrPosters))
    }

    @Test
    fun `maps adoption dataset query results when query finishes`() {
        val finished = QueryResult.Finished(Unit)
        val waiting = QueryResult.Waiting<Unit>()
        val dbClient = FakeDbClient(listOf(queryId), mapOf(queryId to listOf(waiting, finished)))
        val dao = AnalyticsDao(workspace, dbClient)

        assertThat(dao.checkQueryState(queryId)).isInstanceOf(QueryResult.Waiting::class.java)
        assertThat(dao.checkQueryState(queryId)).isEqualTo(QueryResult.Finished(Unit))
    }

    private fun sqlFrom(table: String): String = """"some-workspace_$schema"."some-workspace_$table""""
}

