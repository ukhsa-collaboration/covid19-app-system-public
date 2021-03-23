package uk.nhs.nhsx.keyfederation

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.amazonaws.services.dynamodbv2.document.Item
import com.amazonaws.services.dynamodbv2.document.KeyAttribute
import com.amazonaws.services.dynamodbv2.document.Table
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap
import com.amazonaws.services.dynamodbv2.model.ReturnValue.UPDATED_NEW
import uk.nhs.nhsx.keyfederation.upload.lookup.UploadKeysResult
import java.time.Instant
import java.time.LocalDate
import java.util.*

class BatchTagDynamoDBService(stateTableName: String, client: AmazonDynamoDB) : BatchTagService {
    private val table = DynamoDB(client).getTable(stateTableName)

    override fun lastUploadState() = getLastUploadState(table)

    private fun getLastUploadState(table: Table) =
        Optional.ofNullable(table.getItem(primaryKeyAttributeName, primaryKeyAttributeValueUpload))
            .map { UploadKeysResult(it.getLong(uploadTimeAttributeName)) }

    override fun updateLastUploadState(lastUploadTimestamp: Instant) =
        updateLastUploadState(lastUploadTimestamp, table)

    private fun updateLastUploadState(uploadTimestamp: Instant, table: Table) {
        table.updateItem(
            UpdateItemSpec()
                .withPrimaryKey(primaryKeyAttributeName, primaryKeyAttributeValueUpload)
                .withUpdateExpression("set $uploadTimeAttributeName = :uploadTimestamp")
                .withReturnValues(UPDATED_NEW)
                .withValueMap(ValueMap().withLong(":uploadTimestamp", uploadTimestamp.epochSecond))
        )
    }

    override fun latestFederationBatch(): Optional<FederationBatch> =
        Optional.ofNullable(table.getItem(KeyAttribute("id", primaryKeyAttributeValueDownload))).map {
            FederationBatch(
                BatchTag.of(it.getString("batchTag")),
                LocalDate.parse(it.getString("batchDate"))
            )
        }

    override fun updateLatestFederationBatch(batch: FederationBatch) {
        table.putItem(
            Item()
                .withPrimaryKey("id", primaryKeyAttributeValueDownload)
                .with("batchTag", batch.batchTag.value)
                .with("batchDate", batch.batchDate.toString())
        )
    }

    companion object {
        private const val uploadTimeAttributeName = "uploadTimestamp"
        private const val primaryKeyAttributeName = "id"
        private const val primaryKeyAttributeValueDownload = "lastDownloadState"
        private const val primaryKeyAttributeValueUpload = "lastUploadState"
    }

}
