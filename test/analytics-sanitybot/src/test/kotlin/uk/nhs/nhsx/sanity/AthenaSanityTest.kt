package uk.nhs.nhsx.sanity

import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.services.athena.AmazonAthenaClient
import com.amazonaws.services.s3.AmazonS3Client
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import uk.nhs.nhsx.sanity.BaseSanityCheck.Companion.account
import uk.nhs.nhsx.sanity.BaseSanityCheck.Companion.targetWorkspace
import uk.nhs.nhsx.sanity.athena.AthenaAsyncDbClient
import uk.nhs.nhsx.sanity.athena.QueryId
import uk.nhs.nhsx.sanity.athena.QueryResult


class AthenaSanityTest {
    private val analyticsDevProfile = "analytics-$account-ApplicationDeploymentUser"
    private val profileCredentialsProvider = ProfileCredentialsProvider(analyticsDevProfile)

    private val athenaDb = "${targetWorkspace}_analytics_db"
    private val outputBucketName = "${targetWorkspace}-analytics-output-quicksight"
    private val outputBucketSuffix = "analytics-sanity-checks"

    private val athenaDbClient = AthenaAsyncDbClient(
        athena = AmazonAthenaClient.builder().withCredentials(profileCredentialsProvider).build(),
        athenaDb = athenaDb,
        outputBucket = "s3://$outputBucketName/$outputBucketSuffix"
        )

    private val s3Client = AmazonS3Client
        .builder()
        .withCredentials(profileCredentialsProvider)
        .build()

    @Test
    fun `athena sanity checks`() {
        val showTablesQueryId = athenaDbClient.submitQuery("SHOW TABLES")

        waitForQueryToFinish(showTablesQueryId)

        val showTablesQueryResult = s3Client.getObject("$outputBucketName/$outputBucketSuffix", "${showTablesQueryId.id}.txt").objectContent.bufferedReader().readText()

        val athenaTables = showTablesQueryResult.lines();

        athenaTables.forEach { table ->
            val selectTableQueryId = athenaDbClient.submitQuery("SELECT * FROM \"$athenaDb\".\"$table\" limit 1;")

            try {
                waitForQueryToFinish(selectTableQueryId)
            }
            catch (ex: Exception) {
                throw RuntimeException("Failed to query $table: ${ex.message}")
            }
        }

        expectThat(athenaTables.count()).isEqualTo(39)
    }

    private fun waitForQueryToFinish(queryId: QueryId) {
        while (true) {
            when (val state = athenaDbClient.queryResults(queryId)) {
                is QueryResult.Waiting -> Thread.sleep(1000)
                is QueryResult.Error -> throw RuntimeException("Error:${state.message}, executing query:$queryId")
                is QueryResult.Finished -> return
            }
        }
    }
}
