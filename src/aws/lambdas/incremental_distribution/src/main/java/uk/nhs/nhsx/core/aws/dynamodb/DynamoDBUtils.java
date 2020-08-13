package uk.nhs.nhsx.core.aws.dynamodb;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.DeleteItemSpec;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;

public class DynamoDBUtils implements AwsDynamoClient {

	private final DynamoDB dynamoDB;

	public DynamoDBUtils() {
		dynamoDB = new DynamoDB(AmazonDynamoDBClientBuilder.defaultClient());
	}

	@Override
	public void putItem (String tableName, Item item) {
		Table table = dynamoDB.getTable(tableName);
    	table.putItem(item);
	}

	@Override
	public Item getItem(String tableName, String hashKeyName, String hashKeyValue) {
		Table table = dynamoDB.getTable(tableName);
		return table.getItem(hashKeyName, hashKeyValue);
	}

	@Override
	public DeleteItemOutcome deleteItem(String tableName, String hashKeyName, String hashKeyValue) {
		Table table = dynamoDB.getTable(tableName);

		DeleteItemSpec deleteItemSpec = new DeleteItemSpec()
			.withPrimaryKey(new PrimaryKey(hashKeyName, hashKeyValue))
			.withReturnValues(ReturnValue.ALL_OLD);

		return table.deleteItem(deleteItemSpec);
	}
}
