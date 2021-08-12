package uk.nhs.nhsx.core.aws.dynamodb

import com.amazonaws.services.dynamodbv2.document.Item

interface AwsDynamoClient {
    fun getItem(
        tableName: TableName,
        hashKeyName: String,
        hashKeyValue: String
    ): Item?

    fun deleteItem(
        tableName: TableName,
        hashKeyName: String,
        hashKeyValue: String
    )
}
