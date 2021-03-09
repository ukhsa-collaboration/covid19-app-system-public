package uk.nhs.nhsx.keyfederation;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.KeyAttribute;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import org.jetbrains.annotations.NotNull;
import uk.nhs.nhsx.keyfederation.upload.lookup.UploadKeysResult;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

public class BatchTagDynamoDBService implements BatchTagService {

    private static final String uploadTimeAttributeName = "uploadTimestamp";
    private static final String primaryKeyAttributeName = "id";
    private static final String primaryKeyAttributeValueDownload = "lastDownloadState";
    private static final String primaryKeyAttributeValueUpload = "lastUploadState";

    private final Table table;

    public BatchTagDynamoDBService(String stateTableName, AmazonDynamoDB client) {
        this.table = new DynamoDB(client).getTable(stateTableName);
    }

    @NotNull
    @Override
    public Optional<UploadKeysResult> lastUploadState() {
        return getLastUploadState(table);
    }

    public Optional<UploadKeysResult> getLastUploadState(Table table) {
        Item item = table.getItem(primaryKeyAttributeName, primaryKeyAttributeValueUpload);
        return Optional.ofNullable(item).map(i -> new UploadKeysResult(i.getLong(uploadTimeAttributeName)));
    }

    @Override
    public void updateLastUploadState(@NotNull Instant uploadTimestamp) {
        updateLastUploadState(uploadTimestamp, table);
    }

    public void updateLastUploadState(Instant uploadTimestamp, Table table) {
        UpdateItemSpec updateItemSpec = new UpdateItemSpec().withPrimaryKey(primaryKeyAttributeName, primaryKeyAttributeValueUpload)
            .withUpdateExpression("set " + uploadTimeAttributeName + " = :uploadTimestamp")
            .withReturnValues(ReturnValue.UPDATED_NEW)
            .withValueMap(new ValueMap()
                .withLong(":uploadTimestamp", uploadTimestamp.getEpochSecond()));
        table.updateItem(updateItemSpec);
    }

    @NotNull
    @Override
    public Optional<FederationBatch> latestFederationBatch() {
        Item item = table.getItem(new KeyAttribute("id", primaryKeyAttributeValueDownload));
        return Optional.ofNullable(item).map(it -> new FederationBatch(
            BatchTag.Companion.of(it.getString("batchTag")),
            LocalDate.parse(it.getString("batchDate"))
        ));
    }

    @Override
    public void updateLatestFederationBatch(FederationBatch batch) {
        table.putItem(
            new Item()
                .withPrimaryKey("id", primaryKeyAttributeValueDownload)
                .with("batchTag", batch.batchTag.getValue())
                .with("batchDate", batch.batchDate.toString())
        );
    }

}
