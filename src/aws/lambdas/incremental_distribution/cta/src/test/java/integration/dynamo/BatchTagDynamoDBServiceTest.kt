package integration.dynamo

import com.amazonaws.services.dynamodbv2.model.DeleteItemRequest
import com.amazonaws.services.dynamodbv2.model.ScanRequest
import integration.dynamo.BatchTagDynamoDBServiceTest.State.Download
import integration.dynamo.BatchTagDynamoDBServiceTest.State.Upload
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import uk.nhs.nhsx.core.aws.dynamodb.DynamoAttributes.attributeMap
import uk.nhs.nhsx.core.aws.dynamodb.TableName
import uk.nhs.nhsx.core.aws.dynamodb.withTableName
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.domain.BatchTag
import uk.nhs.nhsx.keyfederation.BatchTagDynamoDBService
import uk.nhs.nhsx.keyfederation.FederationBatch
import uk.nhs.nhsx.keyfederation.upload.lookup.UploadKeysResult
import java.time.Clock
import java.time.Duration.ofMinutes
import java.time.Instant.EPOCH
import java.time.LocalDate
import java.util.*

class BatchTagDynamoDBServiceTest : DynamoIntegrationTest() {
    private val tableName = TableName.of("${tgtEnv}-federation-key-proc-history")
    private val persistence = BatchTagDynamoDBService(tableName, dbClient, RecordingEvents())

    @Test
    fun `upload state lifecycle`() {
        assumeTrue("te-ci" == tgtEnv.lowercase())

        deleteState(Upload)
        wait { numberOfItems(Upload) == 0 }

        // is NOT set
        expectThat(persistence.lastUploadState()).isNull()

        // set last upload time
        persistence.updateLastUploadState(EPOCH)
        wait { persistence.lastUploadState() != null }

        // is now set
        expectThat(persistence.lastUploadState())
            .isNotNull()
            .isEqualTo(UploadKeysResult(EPOCH.epochSecond))
    }

    @Test
    fun `download state lifecycle`() {
        assumeTrue("te-ci" == tgtEnv.lowercase())

        val batchTag = BatchTag.of(UUID.randomUUID().toString())
        val batchDate = LocalDate.now(Clock.systemUTC())
        val federationBatch = FederationBatch(batchTag, batchDate)

        deleteState(Download)
        wait { numberOfItems(Download) == 0 }

        // is NOT set
        expectThat(persistence.latestFederationBatch()).isNull()

        // set last upload time
        persistence.updateLatestFederationBatch(federationBatch)
        wait { persistence.latestFederationBatch() != null }

        // is now set
        expectThat(persistence.latestFederationBatch())
            .isNotNull()
            .isEqualTo(federationBatch)
    }

    private enum class State {
        Upload, Download
    }

    private fun deleteState(state: State) {
        when (state) {
            Upload -> dbClient.deleteItem(
                DeleteItemRequest()
                    .withTableName(tableName)
                    .withKey(attributeMap("id", "lastUploadState"))
            )
            Download -> dbClient.deleteItem(
                DeleteItemRequest()
                    .withTableName(tableName)
                    .withKey(attributeMap("id", "lastDownloadState"))
            )
        }
    }

    private fun numberOfItems(state: State): Int? {
        val key = when (state) {
            Upload -> "lastDownloadState"
            Download -> "lastDownloadState"
        }

        return dbClient.scan(
            ScanRequest()
                .withTableName(tableName.value)
                .withFilterExpression("id = $key")
        ).count
    }

    private fun wait(predicate: () -> Boolean) {
        await().atMost(ofMinutes(1)).until(predicate)
    }
}
