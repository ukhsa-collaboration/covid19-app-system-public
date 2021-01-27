package uk.nhs.nhsx.virology.persistence;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.nhs.nhsx.core.aws.dynamodb.DynamoTransactions;
import uk.nhs.nhsx.core.exceptions.TransactionException;
import uk.nhs.nhsx.virology.*;
import uk.nhs.nhsx.virology.result.VirologyResultRequest;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Arrays.asList;
import static uk.nhs.nhsx.core.aws.dynamodb.DynamoAttributes.*;
import static uk.nhs.nhsx.core.aws.dynamodb.DynamoTransactions.executeTransaction;
import static uk.nhs.nhsx.virology.TestKit.LAB_RESULT;

public class VirologyPersistenceService {

    private static final Logger logger = LogManager.getLogger(VirologyPersistenceService.class);
    private final AmazonDynamoDB dynamoDbClient;
    private final VirologyConfig config;

    public VirologyPersistenceService(AmazonDynamoDB dynamoDbClient,
                                      VirologyConfig config) {
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
                itemValueOrThrow(it, "status"),
                itemValueMaybe(it, "testKit").map(TestKit::valueOf).orElse(LAB_RESULT)));
    }

    public Optional<TestOrder> getTestOrder(CtaToken ctaToken) {
        var itemResult = dynamoDbClient.getItem(
            new GetItemRequest(
                config.testOrdersTable,
                attributeMap("ctaToken", ctaToken.value)
            )
        );

        return Optional.ofNullable(itemResult.getItem())
            .map(it -> new TestOrder(
                itemValueOrThrow(it, "ctaToken"),
                itemValueOrThrow(it, "testResultPollingToken"),
                itemValueOrThrow(it, "diagnosisKeySubmissionToken"),
                itemIntegerValueMaybe(it, "downloadCount").orElse(0)
            ));
    }

    public TestOrder persistTestOrder(Supplier<TestOrder> testOrderSupplier,
                                      long expireAt) {
        return persistTestOrderTransactItems(
            testOrderSupplier,
            testOrder -> asList(
                testOrderCreateOp(testOrder, expireAt),
                testResultPendingCreateOp(testOrder, expireAt)
            )
        );
    }

    public TestOrder persistTestOrderAndResult(Supplier<TestOrder> testOrderSupplier,
                                               long expireAt,
                                               String testResult,
                                               String testEndDate,
                                               TestKit testKit) {
        return persistTestOrderTransactItems(
            testOrderSupplier,
            testOrder -> {
                if (VirologyResultRequest.NPEX_POSITIVE.equals(testResult)) {
                    return asList(
                        testOrderCreateOp(testOrder, expireAt),
                        testResultAvailableCreateOp(testOrder, expireAt, testResult, testEndDate, testKit),
                        submissionTokenCreateOp(testOrder, expireAt, testKit)
                    );
                }

                return asList(
                    testOrderCreateOp(testOrder, expireAt),
                    testResultAvailableCreateOp(testOrder, expireAt, testResult, testEndDate, testKit)
                );
            }
        );
    }

    private TestOrder persistTestOrderTransactItems(Supplier<TestOrder> testOrderSupplier,
                                                    Function<TestOrder, List<TransactWriteItem>> transactWriteItems) {
        var numberOfTries = 0;
        do {
            try {
                var testOrder = testOrderSupplier.get();
                var transactItems = transactWriteItems.apply(testOrder);
                dynamoDbClient.transactWriteItems(new TransactWriteItemsRequest().withTransactItems(transactItems));
                return testOrder;
            } catch (TransactionCanceledException e) {
                logger.info("Persistence of test order was cancelled by remote DB service due to " +
                    DynamoTransactions.reasons(e)
                );
            }
            numberOfTries++;
        } while (numberOfTries < config.maxTokenPersistenceRetryCount);


        throw new RuntimeException(
            "Persistence of test order exceeded maximum of " + config.maxTokenPersistenceRetryCount + " retries"
        );
    }

    public void markForDeletion(TestResult testResult, VirologyDataTimeToLive virologyTimeToLive) {
        queryTestOrderFor(testResult)
            .ifPresentOrElse(
                testOrder -> markTestResultForDeletion(testResult, virologyTimeToLive, testOrder),
                () -> logger.warn("Could not mark for deletion testResultPollingToken:" + testResult.testResultPollingToken) // FIXME
            );
    }

    private void markTestResultForDeletion(TestResult testResult,
                                           VirologyDataTimeToLive virologyTimeToLive,
                                           TestOrder testOrder) {
        long testDataExpireAt = virologyTimeToLive.testDataExpireAt;

        if (testResult.isPositive()) {
            executeTransaction(dynamoDbClient,
                asList(
                    testOrderTtlUpdateOp(testOrder.ctaToken, testDataExpireAt),
                    testResultTtlUpdateOp(testOrder.testResultPollingToken, testDataExpireAt),
                    submissionTokenTimeToLiveUpdateOp(
                        testOrder.diagnosisKeySubmissionToken,
                        virologyTimeToLive.submissionDataExpireAt
                    )
                )
            );
        } else {
            executeTransaction(dynamoDbClient,
                asList(
                    testOrderTtlUpdateOp(testOrder.ctaToken, testDataExpireAt),
                    testResultTtlUpdateOp(testOrder.testResultPollingToken, testDataExpireAt)
                )
            );
        }
    }

    public void updateOnCtaExchange(TestOrder testOrder,
                                    TestResult testResult,
                                    VirologyDataTimeToLive virologyTimeToLive) {
        try {
            updateCtaExchangeTimeToLiveAndCounter(testOrder, testResult, virologyTimeToLive);
        } catch (TransactionException e) {
            logger.warn(
                "Cta exchange ttl and counter not updated for " +
                    "ctaToken:" + testOrder.ctaToken +
                    ", pollingToken:" + testOrder.testResultPollingToken.value +
                    ", submissionToken:" + testOrder.diagnosisKeySubmissionToken.value, e
            );
        }
    }

    private void updateCtaExchangeTimeToLiveAndCounter(TestOrder testOrder,
                                                       TestResult testResult,
                                                       VirologyDataTimeToLive virologyTimeToLive) {
        if (testResult.isPositive() && submissionTokenPresent(testOrder)) {
            executeTransaction(dynamoDbClient,
                asList(
                    testOrderTtlAndCounterUpdateOp(testOrder.ctaToken, virologyTimeToLive.testDataExpireAt),
                    testResultTtlUpdateOp(testOrder.testResultPollingToken, virologyTimeToLive.testDataExpireAt),
                    submissionTokenTimeToLiveUpdateOp(
                        testOrder.diagnosisKeySubmissionToken,
                        virologyTimeToLive.submissionDataExpireAt
                    )
                )
            );
        } else {
            executeTransaction(dynamoDbClient,
                asList(
                    testOrderTtlAndCounterUpdateOp(testOrder.ctaToken, virologyTimeToLive.testDataExpireAt),
                    testResultTtlUpdateOp(testOrder.testResultPollingToken, virologyTimeToLive.testDataExpireAt)
                )
            );
        }
    }

    private boolean submissionTokenPresent(TestOrder testOrder){
        var itemResult = dynamoDbClient.getItem(config.submissionTokensTable, attributeMap("diagnosisKeySubmissionToken", testOrder.diagnosisKeySubmissionToken.value));
         return itemResult.getItem() != null;
    }

    private Optional<TestOrder> queryTestOrderFor(TestResult testResult) {
        var request = new QueryRequest()
            .withTableName(config.testOrdersTable)
            .withIndexName(config.testOrdersIndex)
            .withKeyConditionExpression("testResultPollingToken = :pollingToken")
            .withExpressionAttributeValues(attributeMap(":pollingToken", testResult.testResultPollingToken));

        var result = dynamoDbClient.query(request);
        return result.getItems().stream().findFirst()
            .map(it -> new TestOrder(
                    itemValueOrThrow(it, "ctaToken"),
                    itemValueOrThrow(it, "testResultPollingToken"),
                    itemValueOrThrow(it, "diagnosisKeySubmissionToken")
                )
            );
    }

    private TransactWriteItem testOrderCreateOp(TestOrder testOrder, long expireAt) {
        return new TransactWriteItem().withPut(
            new Put()
                .withTableName(config.testOrdersTable)
                .withItem(
                    Map.of(
                        "ctaToken", stringAttribute(testOrder.ctaToken.value),
                        "testResultPollingToken", stringAttribute(testOrder.testResultPollingToken.value),
                        "diagnosisKeySubmissionToken", stringAttribute(testOrder.diagnosisKeySubmissionToken.value),
                        "expireAt", numericAttribute(expireAt)
                    )
                )
                .withConditionExpression("attribute_not_exists(ctaToken)")
        );
    }

    private TransactWriteItem testResultPendingCreateOp(TestOrder testOrder, long expireAt) {

        return new TransactWriteItem().withPut(
            new Put()
                .withTableName(config.testResultsTable)
                .withItem(
                    Map.of(
                        "testResultPollingToken", stringAttribute(testOrder.testResultPollingToken.value),
                        "status", stringAttribute("pending"),
                        "expireAt", numericAttribute(expireAt)
                    )
                )
        );
    }

    private TransactWriteItem testResultAvailableCreateOp(TestOrder testOrder,
                                                          long expireAt,
                                                          String testResult,
                                                          String testEndDate,
                                                          TestKit testKit) {

        return new TransactWriteItem().withPut(
            new Put()
                .withTableName(config.testResultsTable)
                .withItem(
                    Map.of(
                        "testResultPollingToken", stringAttribute(testOrder.testResultPollingToken.value),
                        "status", stringAttribute("available"),
                        "testResult", stringAttribute(testResult),
                        "testEndDate", stringAttribute(testEndDate),
                        "expireAt", numericAttribute(expireAt),
                        "testKit", stringAttribute(testKit.name())
                    )
                )
        );
    }

    private TransactWriteItem testOrderTtlUpdateOp(CtaToken ctaToken, long testDataExpireAt) {
        return new TransactWriteItem().withUpdate(
            new Update()
                .withTableName(config.testOrdersTable)
                .withKey(attributeMap("ctaToken", ctaToken.value))
                .withUpdateExpression("set expireAt = :expireAt")
                .withExpressionAttributeValues(attributeMap(":expireAt", testDataExpireAt))
        );
    }

    private TransactWriteItem testOrderTtlAndCounterUpdateOp(CtaToken ctaToken, long testDataExpireAt) {
        return new TransactWriteItem().withUpdate(
            new Update()
                .withTableName(config.testOrdersTable)
                .withKey(attributeMap("ctaToken", ctaToken.value))
                .withUpdateExpression("set expireAt = :expireAt add downloadCount :dc")
                .withExpressionAttributeValues(
                    Map.of(
                        ":expireAt", numericAttribute(testDataExpireAt),
                        ":dc", numericAttribute(1)
                    )
                )
        );
    }

    private TransactWriteItem testResultTtlUpdateOp(TestResultPollingToken testResultPollingToken, long testDataExpireAt) {
        return new TransactWriteItem().withUpdate(
            new Update()
                .withTableName(config.testResultsTable)
                .withKey(attributeMap("testResultPollingToken", testResultPollingToken.value))
                .withUpdateExpression("set expireAt = :expireAt")
                .withExpressionAttributeValues(attributeMap(":expireAt", testDataExpireAt))
        );
    }

    private TransactWriteItem submissionTokenTimeToLiveUpdateOp(DiagnosisKeySubmissionToken diagnosisKeySubmissionToken,
                                                                long submissionDataExpireAt) {
        return new TransactWriteItem().withUpdate(
            new Update()
                .withTableName(config.submissionTokensTable)
                .withKey(attributeMap("diagnosisKeySubmissionToken", diagnosisKeySubmissionToken.value))
                .withConditionExpression("attribute_exists(diagnosisKeySubmissionToken)")
                .withUpdateExpression("set expireAt = :expireAt")
                .withExpressionAttributeValues(attributeMap(":expireAt", submissionDataExpireAt))
        );
    }

    public VirologyResultPersistOperation persistPositiveTestResult(VirologyResultRequest.Positive testResult,
                                                                    long expireAt) {
        return persistTestResult(
            testResult,
            order -> List.of(
                resultPollingTokenUpdateOp(order, testResult),
                submissionTokenCreateOp(order, expireAt, testResult.testKit)
            )
        );
    }

    public VirologyResultPersistOperation persistNonPositiveTestResult(VirologyResultRequest.NonPositive testResult) {
        return persistTestResult(
            testResult,
            order -> List.of(resultPollingTokenUpdateOp(order, testResult))
        );
    }

    private VirologyResultPersistOperation persistTestResult(VirologyResultRequest testResult,
                                                             Function<TestOrder, List<TransactWriteItem>> transactWriteItems) {
        return getOrder(testResult.ctaToken)
            .map(order -> {
                try {
                    executeTransaction(dynamoDbClient, transactWriteItems.apply(order));
                } catch (TransactionException e) {
                    if (e.isConditionFailure()) {
                        logger.warn("Failed to persist test result due to duplicate ctaToken: " + order.ctaToken);
                    }
                    return new VirologyResultPersistOperation.TransactionFailed(e.getMessage());
                }
                return new VirologyResultPersistOperation.Success();
            })
            .orElseGet(VirologyResultPersistOperation.OrderNotFound::new);
    }

    private Optional<TestOrder> getOrder(CtaToken ctaToken) {
        var itemResult = dynamoDbClient.getItem(config.testOrdersTable, attributeMap("ctaToken", ctaToken.value));
        return Optional.ofNullable(itemResult.getItem())
            .map(it ->
                new TestOrder(
                    itemValueOrThrow(it, "ctaToken"),
                    itemValueOrThrow(it, "testResultPollingToken"),
                    itemValueOrThrow(it, "diagnosisKeySubmissionToken")
                )
            );
    }

    private TransactWriteItem resultPollingTokenUpdateOp(TestOrder testOrder, VirologyResultRequest testResult) {
        return new TransactWriteItem().withUpdate(
            new Update()
                .withTableName(config.testResultsTable)
                .withKey(attributeMap("testResultPollingToken", testOrder.testResultPollingToken.value))
                .withUpdateExpression("set #s = :status, testEndDate = :testEndDate, testResult = :testResult, testKit = :testKit")
                .withConditionExpression("#s = :pendingStatus")
                .withExpressionAttributeValues(
                    Map.of(
                        ":status", stringAttribute("available"),
                        ":testEndDate", stringAttribute(testResult.testEndDate),
                        ":testResult", stringAttribute(testResult.testResult),
                        ":pendingStatus", stringAttribute("pending"),
                        ":testKit", stringAttribute(testResult.testKit.name())
                    )
                )
                .withExpressionAttributeNames(
                    Map.of("#s", "status")
                )
        );
    }

    private TransactWriteItem submissionTokenCreateOp(TestOrder testOrder, long expireAt, TestKit testKit) {
        return new TransactWriteItem().withPut(
            new Put()
                .withTableName(config.submissionTokensTable)
                .withItem(
                    Map.of(
                        "diagnosisKeySubmissionToken", stringAttribute(testOrder.diagnosisKeySubmissionToken.value),
                        "testKit", stringAttribute(testKit.name()),
                        "expireAt", numericAttribute(expireAt)
                    )
                )
        );
    }
}
