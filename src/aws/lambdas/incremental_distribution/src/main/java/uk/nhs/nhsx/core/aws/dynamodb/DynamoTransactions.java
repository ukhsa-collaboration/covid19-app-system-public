package uk.nhs.nhsx.core.aws.dynamodb;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.*;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class DynamoTransactions {

    public static String reasons(TransactionCanceledException e) {
        return e.getCancellationReasons()
            .stream()
            .map(CancellationReason::getMessage)
            .filter(Objects::nonNull)
            .collect(Collectors.joining(","));
    }

    public static TransactWriteItemsResult executeTransaction(AmazonDynamoDB dynamoDbClient, 
                                                              List<TransactWriteItem> items) {
        try {
            var transactWriteItemsRequest = new TransactWriteItemsRequest().withTransactItems(items);
            return dynamoDbClient.transactWriteItems(transactWriteItemsRequest);
        } catch (TransactionCanceledException e) {
            throw new RuntimeException("Transaction cancelled by remote DB service due to: " + reasons(e));
        }
    }

}
