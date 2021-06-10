package uk.nhs.nhsx.core.aws.dynamodb

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsResult
import com.amazonaws.services.dynamodbv2.model.TransactionCanceledException
import uk.nhs.nhsx.core.exceptions.TransactionException

object DynamoTransactions {
    fun reasons(e: TransactionCanceledException) = e.cancellationReasons.mapNotNull { it.message }.joinToString(",")

    fun executeTransaction(
        dynamoDbClient: AmazonDynamoDB,
        items: List<TransactWriteItem?>?
    ): TransactWriteItemsResult = try {
        dynamoDbClient.transactWriteItems(TransactWriteItemsRequest().withTransactItems(items))
    } catch (e: TransactionCanceledException) {
        throw TransactionException(e)
    }
}
