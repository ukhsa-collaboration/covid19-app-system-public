package uk.nhs.nhsx.keyfederation;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.*;
import uk.nhs.nhsx.keyfederation.upload.lookup.UploadKeysResult;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Arrays.asList;
import static uk.nhs.nhsx.core.aws.dynamodb.DynamoAttributes.attributeMap;
import static uk.nhs.nhsx.core.aws.dynamodb.DynamoAttributes.itemValueOrThrow;
import static uk.nhs.nhsx.core.aws.dynamodb.DynamoTransactions.executeTransaction;

public class BatchTagDynamoDBService implements BatchTagService {

    private final String batchTagAttributeName = "lastReceivedBatchTag";
    private final String primaryKeyAttributeName = "id";
    private final int primaryKeyAttributeValue = 0;

    private final AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
    private final DynamoDB dynamoDB = new DynamoDB(client);
    private final String stateTableName;
    private final Table table;

    public BatchTagDynamoDBService(String stateTableName) {
        this.stateTableName = stateTableName;
        this.table = dynamoDB.getTable(stateTableName);
    }

    @Override
    public BatchTag getLatestBatchTag() {
        return getLatestBatchTag(table);
    }

    public BatchTag getLatestBatchTag(Table table) {
        try {
            Item item = table.getItem(primaryKeyAttributeName, primaryKeyAttributeValue);
            if (item != null) {
                BatchTag batchTag = BatchTag.of(String.valueOf(item.getString(batchTagAttributeName)));
                return batchTag;
            }
            return null;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void updateLatestBatchTag(BatchTag batchTag) {
        updateLatestBatchTag(batchTag, table);
    }

    public void updateLatestBatchTag(BatchTag batchTag, Table table) {
        UpdateItemSpec updateItemSpec = new UpdateItemSpec().withPrimaryKey(primaryKeyAttributeName, primaryKeyAttributeValue)
                .withUpdateExpression("set " + batchTagAttributeName + " = :tag")
                .withReturnValues(ReturnValue.UPDATED_NEW)
                .withValueMap(new ValueMap()
                        .withString(":tag", batchTag.toString()));
        table.updateItem(updateItemSpec);
    }

    public Optional<UploadKeysResult> getLastUploadState() {
        ScanRequest scanRequest = new ScanRequest()
            .withTableName(stateTableName)
            .withFilterExpression("id = :id")
            .withExpressionAttributeValues(attributeMap(":id", 0))
            .withLimit(1);

        ScanResult scanResult = client.scan(scanRequest);
        List<Map<String, AttributeValue>> resultItems = scanResult.getItems();

        return resultItems.stream().findFirst()
            .map(it -> new UploadKeysResult(
                itemValueOrThrow(it, "lastUploadState")
            ));
    }

    public void updateLastUploadState(String lastUploadState) {
        List<TransactWriteItem> transactionItems = asList(
            new TransactWriteItem().withUpdate(
                new Update()
                    .withTableName(stateTableName)
                    .withKey(attributeMap("id",0))
                    .withUpdateExpression("set lastUploadState = :lastUploadState")
                    .withExpressionAttributeValues(attributeMap(":lastUploadState", lastUploadState))
            )
        );
        executeTransaction(client, transactionItems);
    }

}
