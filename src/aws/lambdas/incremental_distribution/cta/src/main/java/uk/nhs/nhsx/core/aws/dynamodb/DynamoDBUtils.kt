package uk.nhs.nhsx.core.aws.dynamodb

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.amazonaws.services.dynamodbv2.document.Item
import com.amazonaws.services.dynamodbv2.document.PrimaryKey
import com.amazonaws.services.dynamodbv2.document.spec.DeleteItemSpec
import com.amazonaws.services.dynamodbv2.model.ReturnValue.ALL_OLD

class DynamoDBUtils(client: AmazonDynamoDB) : AwsDynamoClient {

    private val dynamoDB: DynamoDB = DynamoDB(client)

    override fun getItem(
        tableName: TableName,
        hashKeyName: String,
        hashKeyValue: String
    ): Item? = getTable(tableName).getItem(hashKeyName, hashKeyValue)

    override fun deleteItem(
        tableName: TableName,
        hashKeyName: String,
        hashKeyValue: String
    ) {
        val spec = DeleteItemSpec()
            .withPrimaryKey(PrimaryKey(hashKeyName, hashKeyValue))
            .withReturnValues(ALL_OLD)

        getTable(tableName).deleteItem(spec)
    }

    private fun getTable(tableName: TableName) = dynamoDB.getTable(tableName.value)
}
