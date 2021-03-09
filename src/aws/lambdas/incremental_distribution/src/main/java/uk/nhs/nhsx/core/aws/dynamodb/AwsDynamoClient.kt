package uk.nhs.nhsx.core.aws.dynamodb

import com.amazonaws.services.dynamodbv2.document.Item

interface AwsDynamoClient {
    fun getItem(
        tableName: String,
        hashKeyName: String,
        hashKeyValue: String
    ): Item?

    fun deleteItem(
        tableName: String,
        hashKeyName: String,
        hashKeyValue: String
    )
}
