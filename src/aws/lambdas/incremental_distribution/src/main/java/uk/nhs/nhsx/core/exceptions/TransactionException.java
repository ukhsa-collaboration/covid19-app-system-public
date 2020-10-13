package uk.nhs.nhsx.core.exceptions;

import com.amazonaws.services.dynamodbv2.model.TransactionCanceledException;

import static uk.nhs.nhsx.core.aws.dynamodb.DynamoTransactions.reasons;

public class TransactionException extends RuntimeException {

    private final TransactionCanceledException nestedTransactionCanceledException;
    public TransactionException(TransactionCanceledException transactionCanceledException) {
        super(messageFrom(transactionCanceledException));
        nestedTransactionCanceledException = transactionCanceledException;
    }

    private static String messageFrom(TransactionCanceledException transactionCanceledException) {
        return "Transaction cancelled by remote DB service due to: " + reasons(transactionCanceledException);
    }

    public boolean isConditionFailure(){
        return nestedTransactionCanceledException.getCancellationReasons().stream().anyMatch(reason -> reason.getCode().equals("ConditionalCheckFailed"));
    }
}
