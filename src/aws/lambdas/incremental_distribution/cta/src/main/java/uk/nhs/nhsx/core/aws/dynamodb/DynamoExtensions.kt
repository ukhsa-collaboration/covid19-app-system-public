package uk.nhs.nhsx.core.aws.dynamodb

import com.amazonaws.services.dynamodbv2.model.CreateTableRequest
import com.amazonaws.services.dynamodbv2.model.DeleteItemRequest
import com.amazonaws.services.dynamodbv2.model.GetItemRequest
import com.amazonaws.services.dynamodbv2.model.Put
import com.amazonaws.services.dynamodbv2.model.PutItemRequest
import com.amazonaws.services.dynamodbv2.model.QueryRequest
import com.amazonaws.services.dynamodbv2.model.Update

fun Put.withTableName(tableName: TableName): Put = withTableName(tableName.value)
fun Update.withTableName(tableName: TableName): Update = withTableName(tableName.value)

fun PutItemRequest.withTableName(tableName: TableName): PutItemRequest = withTableName(tableName.value)
fun GetItemRequest.withTableName(tableName: TableName): GetItemRequest = withTableName(tableName.value)
fun DeleteItemRequest.withTableName(tableName: TableName): DeleteItemRequest = withTableName(tableName.value)
fun QueryRequest.withTableName(tableName: TableName): QueryRequest = withTableName(tableName.value)
fun CreateTableRequest.withTableName(tableName: TableName): CreateTableRequest = withTableName(tableName.value)

fun QueryRequest.withIndexName(indexName: IndexName): QueryRequest = withIndexName(indexName.value)
