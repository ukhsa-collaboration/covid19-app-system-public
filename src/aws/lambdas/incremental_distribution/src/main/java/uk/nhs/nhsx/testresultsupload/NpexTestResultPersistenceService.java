package uk.nhs.nhsx.testresultsupload;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.nhs.nhsx.core.aws.dynamodb.DynamoTransactions;
import uk.nhs.nhsx.core.exceptions.ApiResponseException;
import uk.nhs.nhsx.core.exceptions.HttpStatusCode;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static uk.nhs.nhsx.core.aws.dynamodb.DynamoAttributes.attributeMap;
import static uk.nhs.nhsx.core.aws.dynamodb.DynamoAttributes.itemValueOrThrow;
import static uk.nhs.nhsx.core.aws.dynamodb.DynamoTransactions.executeTransaction;

public class NpexTestResultPersistenceService {

    private static final Logger logger = LoggerFactory.getLogger(NpexTestResultPersistenceService.class);

    private static class Order {
        final String testResultPollingToken;
        final String diagnosisKeySubmissionToken;

        Order(String testResultPollingToken, String diagnosisKeySubmissionToken) {
            this.testResultPollingToken = testResultPollingToken;
            this.diagnosisKeySubmissionToken = diagnosisKeySubmissionToken;
        }
    }

    private final AmazonDynamoDB dynamoDbClient;
    private final NpexTestResultConfig config;

    NpexTestResultPersistenceService(AmazonDynamoDB dynamoDbClient, NpexTestResultConfig config) {
        this.dynamoDbClient = dynamoDbClient;
        this.config = config;
    }

    public void persistPositiveTestResult(NpexTestResult.Positive testResult, long expireAt) {
        persistTestResult(
            testResult,
            order -> List.of(resultPollingTokenUpdateOp(order, testResult), submissionTokenCreateOp(order, expireAt))
        );
    }

    public void persistNonPositiveTestResult(NpexTestResult.NonPositive testResult) {
        persistTestResult(
            testResult,
            order -> List.of(resultPollingTokenUpdateOp(order, testResult))
        );
    }

    private void persistTestResult(NpexTestResult testResult,
                                   Function<Order, List<TransactWriteItem>> transactWriteItems) {
        getOrder(testResult.ctaToken)
            .map(order -> executeTransaction(dynamoDbClient, transactWriteItems.apply(order)))
            .orElseThrow(() -> {
                logger.warn("Failed to persist test result to database. Call to get test order table did not return complete result.");
                return new ApiResponseException(HttpStatusCode.BAD_REQUEST_400, "Bad request: test order token not found");
            });
    }

    private Optional<Order> getOrder(String ctaToken) {
        var itemResult = dynamoDbClient.getItem(config.testOrdersTable, attributeMap("ctaToken", ctaToken));
        return Optional.ofNullable(itemResult.getItem())
            .map(it ->
                new Order(
                    itemValueOrThrow(it, "testResultPollingToken"),
                    itemValueOrThrow(it, "diagnosisKeySubmissionToken")
                )
            );
    }

    private TransactWriteItem resultPollingTokenUpdateOp(Order order, NpexTestResult testResult) {
        return new TransactWriteItem().withUpdate(
            new Update()
                .withTableName(config.testResultsTable)
                .withKey(attributeMap("testResultPollingToken", order.testResultPollingToken))
                .withUpdateExpression("set #s = :status, testEndDate = :testEndDate, testResult = :testResult")
                .withExpressionAttributeValues(
                    Map.of(
                        ":status", new AttributeValue("available"),
                        ":testEndDate", new AttributeValue(testResult.testEndDate),
                        ":testResult", new AttributeValue(testResult.testResult)
                    )
                )
                .withExpressionAttributeNames(
                    Map.of("#s", "status")
                )
        );
    }

    private TransactWriteItem submissionTokenCreateOp(Order order, long expireAt) {
        return new TransactWriteItem().withPut(
            new Put()
                .withTableName(config.submissionTokensTable)
                .withItem(
                    Map.of(
                        "diagnosisKeySubmissionToken", new AttributeValue(order.diagnosisKeySubmissionToken),
                        "expireAt", new AttributeValue().withN(String.valueOf(expireAt))
                    )
                )
        );
    }

}
