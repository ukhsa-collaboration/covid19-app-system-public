package uk.nhs.nhsx.core.exceptions;

import com.amazonaws.services.dynamodbv2.model.TransactionCanceledException;

import static uk.nhs.nhsx.core.aws.dynamodb.DynamoTransactions.reasons;

public class TransactionException extends RuntimeException {

    public TransactionException(TransactionCanceledException transactionCanceledException) {
        super(messageFrom(transactionCanceledException));
    }

    private static String messageFrom(TransactionCanceledException transactionCanceledException) {
        return "Transaction cancelled by remote DB service due to: " + reasons(transactionCanceledException);
    }

}
