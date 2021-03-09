package uk.nhs.nhsx.core.aws.dynamodb

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.document.DeleteItemOutcome
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.amazonaws.services.dynamodbv2.document.Item
import com.amazonaws.services.dynamodbv2.document.spec.DeleteItemSpec
import com.amazonaws.services.dynamodbv2.document.PrimaryKey
import com.amazonaws.services.dynamodbv2.model.ReturnValue

class DynamoDBUtils(client: AmazonDynamoDB) : AwsDynamoClient {

    private val dynamoDB: DynamoDB = DynamoDB(client)

    override fun getItem(
        tableName: String,
        hashKeyName: String,
        hashKeyValue: String
    ): Item? = dynamoDB.getTable(tableName).getItem(hashKeyName, hashKeyValue)

    override fun deleteItem(
        tableName: String,
        hashKeyName: String,
        hashKeyValue: String
    ) {
        dynamoDB.getTable(tableName).deleteItem(
            DeleteItemSpec()
                .withPrimaryKey(PrimaryKey(hashKeyName, hashKeyValue))
                .withReturnValues(ReturnValue.ALL_OLD)
        )
    }
}
