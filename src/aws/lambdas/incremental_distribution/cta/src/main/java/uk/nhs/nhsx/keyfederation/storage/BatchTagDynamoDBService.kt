package uk.nhs.nhsx.keyfederation.storage

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.amazonaws.services.dynamodbv2.document.Item
import com.amazonaws.services.dynamodbv2.document.PrimaryKey
import com.amazonaws.services.dynamodbv2.document.Table
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap
import com.amazonaws.services.dynamodbv2.model.ReturnValue.UPDATED_NEW
import uk.nhs.nhsx.core.aws.dynamodb.TableName
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.domain.BatchTag
import uk.nhs.nhsx.keyfederation.FederationDownloadStateUpdated
import uk.nhs.nhsx.keyfederation.FederationUploadStateUpdated
import uk.nhs.nhsx.keyfederation.domain.FederationBatch
import java.time.Instant
import java.time.LocalDate

class BatchTagDynamoDBService(
    private val table: Table,
    private val events: Events
) : BatchTagService {

    constructor(
        tableName: TableName,
        client: AmazonDynamoDB,
        events: Events
    ) : this(
        DynamoDB(client).getTable(tableName.value),
        events
    )

    override fun lastUploadState() = table.getItem(lastUploadStatePK)?.let {
        UploadKeysResult(it.getLong(uploadTimeAttributeName))
    }

    override fun updateLastUploadState(lastUploadTimestamp: Instant) {
        table.updateItem(
            UpdateItemSpec()
                .withPrimaryKey(lastUploadStatePK)
                .withUpdateExpression("set $uploadTimeAttributeName = :uploadTimestamp")
                .withReturnValues(UPDATED_NEW)
                .withValueMap(ValueMap().withLong(":uploadTimestamp", lastUploadTimestamp.epochSecond))
        )

        events(FederationUploadStateUpdated(lastUploadTimestamp))
    }

    override fun latestFederationBatch() = table.getItem(lastDownloadStatePK)?.let {
        FederationBatch(
            BatchTag.of(it.getString("batchTag")),
            LocalDate.parse(it.getString("batchDate"))
        )
    }

    override fun updateLatestFederationBatch(batch: FederationBatch) {
        table.putItem(
            Item()
                .withPrimaryKey(lastDownloadStatePK)
                .with("batchTag", batch.batchTag.value)
                .with("batchDate", batch.batchDate.toString())
        )

        events(FederationDownloadStateUpdated(batch))
    }

    companion object {
        private val lastDownloadStatePK = PrimaryKey("id", "lastDownloadState")
        private val lastUploadStatePK = PrimaryKey("id", "lastUploadState")
        private const val uploadTimeAttributeName = "uploadTimestamp"
    }
}
