package uk.nhs.nhsx.core.aws.dynamodb;

import com.amazonaws.services.dynamodbv2.document.DeleteItemOutcome;
import com.amazonaws.services.dynamodbv2.document.Item;

public interface AwsDynamoClient {
    void putItem (String tableName, Item item);
    Item getItem(String tableName, String hashKeyName, String hashKeyValue);
    DeleteItemOutcome deleteItem(String tableName, String hashKeyName, String hashKeyValue);
}
