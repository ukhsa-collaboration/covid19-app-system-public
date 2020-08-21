package uk.nhs.nhsx.testkitorder;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.nhs.nhsx.core.aws.dynamodb.DynamoTransactions;
import uk.nhs.nhsx.testkitorder.lookup.TestResult;
import uk.nhs.nhsx.testkitorder.order.TokensGenerator;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static java.util.Arrays.asList;
import static uk.nhs.nhsx.core.aws.dynamodb.DynamoAttributes.*;
import static uk.nhs.nhsx.core.aws.dynamodb.DynamoTransactions.executeTransaction;

class TestKitOrderDynamoService implements TestKitOrderPersistenceService {

    private static final Logger logger = LoggerFactory.getLogger(TestKitOrderDynamoService.class);

    private final AmazonDynamoDB dynamoDbClient;
    private final TestKitOrderConfig config;

    public TestKitOrderDynamoService(AmazonDynamoDB dynamoDbClient,
                                     TestKitOrderConfig config) {
        this.config = config;
        this.dynamoDbClient = dynamoDbClient;
    }

    public Optional<TestResult> getTestResult(TestResultPollingToken pollingToken) {
        var itemResult = dynamoDbClient.getItem(
            new GetItemRequest(
                config.testResultsTable,
                attributeMap("testResultPollingToken", pollingToken.value)
            )
        );

        return Optional.ofNullable(itemResult.getItem())
            .map(it -> new TestResult(
                itemValueOrThrow(it, "testResultPollingToken"),
                itemValueMaybe(it, "testEndDate").orElse(""),
                itemValueMaybe(it, "testResult").orElse(""),
                itemValueOrThrow(it, "status")
            ));
    }

    public TokensGenerator.TestOrderTokens persistTestOrder(Supplier<TokensGenerator.TestOrderTokens> tokens,
                                                            long expireAt) {
        var numberOfTries = 0;
        do {
            try {
                TokensGenerator.TestOrderTokens newTokens = tokens.get();
                dynamoDbClient.transactWriteItems(
                    new TransactWriteItemsRequest()
                        .withTransactItems(
                            testOrderCreateOp(newTokens, expireAt),
                            testResultCreateOp(newTokens, expireAt)
                        )
                );
                return newTokens;
            } catch (TransactionCanceledException e) {
                logger.warn("Persistence of test order was cancelled by remote DB service due to " +
                    DynamoTransactions.reasons(e)
                );
            }
            numberOfTries++;
        } while (numberOfTries < config.maxTokenPersistenceRetryCount);


        throw new RuntimeException(
            "Persistence of test order exceeded maximum of " + config.maxTokenPersistenceRetryCount + " retries"
        );
    }

    public void markForDeletion(VirologyDataTimeToLive virologyDataTimeToLive) {
        var testOrderItem = scanTestOrderFor(virologyDataTimeToLive.pollingToken)
            .orElseThrow(() -> new IllegalStateException(
                "Could not find ctaToken for testResultPollingToken:" + virologyDataTimeToLive.pollingToken.value
            ));

        List<TransactWriteItem> transactionItems =
            updateVirologyDataExpireAt(
                virologyDataTimeToLive,
                itemValueOrThrow(testOrderItem, "ctaToken"),
                itemValueOrThrow(testOrderItem, "diagnosisKeySubmissionToken")
            );

        executeTransaction(dynamoDbClient, transactionItems);
    }

    private Optional<Map<String, AttributeValue>> scanTestOrderFor(TestResultPollingToken pollingToken) {
        Map<String, AttributeValue> lastKeyEvaluated = null;
        List<Map<String, AttributeValue>> resultItems;

        do {
            ScanRequest scanRequest = new ScanRequest()
                .withTableName(config.testOrdersTable)
                .withFilterExpression("testResultPollingToken = :pollingToken")
                .withExpressionAttributeValues(attributeMap(":pollingToken", pollingToken.value))
                .withExclusiveStartKey(lastKeyEvaluated)
                .withLimit(1000);

            ScanResult scanResult = dynamoDbClient.scan(scanRequest);

            lastKeyEvaluated = scanResult.getLastEvaluatedKey();

            resultItems = scanResult.getItems();

        } while (lastKeyEvaluated != null && resultItems.isEmpty());

        return resultItems.stream().findFirst();
    }

    private List<TransactWriteItem> updateVirologyDataExpireAt(VirologyDataTimeToLive timeToLive,
                                                               String ctaToken,
                                                               String diagnosisKeySubmissionToken) {
        return asList(
            new TransactWriteItem().withUpdate(
                new Update()
                    .withTableName(config.testOrdersTable)
                    .withKey(attributeMap("ctaToken", ctaToken))
                    .withUpdateExpression("set expireAt = :expireAt")
                    .withExpressionAttributeValues(attributeMap(":expireAt", timeToLive.testDataExpireAt))
            ),
            new TransactWriteItem().withUpdate(
                new Update()
                    .withTableName(config.testResultsTable)
                    .withKey(attributeMap("testResultPollingToken", timeToLive.pollingToken.value))
                    .withUpdateExpression("set expireAt = :expireAt")
                    .withExpressionAttributeValues(attributeMap(":expireAt", timeToLive.testDataExpireAt))
            ),
            new TransactWriteItem().withUpdate(
                new Update()
                    .withTableName(config.virologySubmissionTokensTable)
                    .withKey(attributeMap("diagnosisKeySubmissionToken", diagnosisKeySubmissionToken))
                    .withUpdateExpression("set expireAt = :expireAt")
                    .withExpressionAttributeValues(attributeMap(":expireAt", timeToLive.submissionDataExpireAt))
            )
        );
    }

    private TransactWriteItem testOrderCreateOp(TokensGenerator.TestOrderTokens tokens,
                                                long expireAt) {
        return new TransactWriteItem().withPut(
            new Put()
                .withTableName(config.testOrdersTable)
                .withItem(
                    Map.of(
                        "ctaToken", new AttributeValue(tokens.ctaToken),
                        "testResultPollingToken", new AttributeValue(tokens.testResultPollingToken),
                        "diagnosisKeySubmissionToken", new AttributeValue(tokens.diagnosisKeySubmissionToken),
                        "expireAt", new AttributeValue().withN(String.valueOf(expireAt))
                    )
                )
                .withConditionExpression("attribute_not_exists(ctaToken)")
        );
    }

    private TransactWriteItem testResultCreateOp(TokensGenerator.TestOrderTokens tokens,
                                                 long expireAt) {

        return new TransactWriteItem().withPut(
            new Put()
                .withTableName(config.testResultsTable)
                .withItem(
                    Map.of(
                        "testResultPollingToken", new AttributeValue(tokens.testResultPollingToken),
                        "status", new AttributeValue("pending"),
                        "expireAt", new AttributeValue().withN(String.valueOf(expireAt))
                    )
                )
        );
    }
}
