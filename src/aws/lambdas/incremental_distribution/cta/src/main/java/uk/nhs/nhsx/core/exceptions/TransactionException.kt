package uk.nhs.nhsx.core.exceptions

import com.amazonaws.services.dynamodbv2.model.TransactionCanceledException
import uk.nhs.nhsx.core.aws.dynamodb.DynamoTransactions.reasons

class TransactionException(nestedTransactionCanceledException: TransactionCanceledException) :
    RuntimeException(
        "Transaction cancelled by remote DB service due to: " + reasons(
            nestedTransactionCanceledException
        )
    )
