package uk.nhs.nhsx.testkitorder;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Collections.singletonMap;

class TestKitOrderDynamoPersistenceService implements TestKitOrderPersistenceService {

    private static final Logger logger = LoggerFactory.getLogger(uk.nhs.nhsx.testkitorder.TestKitOrderDynamoPersistenceService.class);

    private static final String TEST_RESULTS_TABLE_KEY = "testResultPollingToken";

    private final AmazonDynamoDB dynamoDbClient;

    private final String testOrdersTableName;

    private final String testResultsTableName;

    public TestKitOrderDynamoPersistenceService(AmazonDynamoDB dynamoDbClient, String testOrdersTableName, String testResultsTableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.testOrdersTableName = testOrdersTableName;
        this.testResultsTableName = testResultsTableName;
    }

    public Optional<TestResult> getTestResult(TestResultPollingToken testResultPollingToken) {
        GetItemResult itemResult = dynamoDbClient.getItem(
            new GetItemRequest(
                testResultsTableName,
                singletonMap(TEST_RESULTS_TABLE_KEY, new AttributeValue(testResultPollingToken.value))
            )
        );

        return Optional.ofNullable(itemResult.getItem())
            .map(it -> new TestResult(
                requireString(it, "testResultPollingToken"),
                optionalString(it, "testEndDate"),
                optionalString(it, "testResult"),
                requireString(it, "status")
            ));
    }

    private static String requireString(Map<String, AttributeValue> item, String key) {
        return Optional.ofNullable(item.get(key)).map(AttributeValue::getS)
            .orElseThrow(() -> new RuntimeException("Required field missing"));
    }

    private static String optionalString(Map<String, AttributeValue> item, String key) {
        return Optional.ofNullable(item.get(key)).map(AttributeValue::getS)
            .orElse("");
    }

    public void persistTestOrder(TokensGenerator.TestOrderTokens tokens) {
        try {
            dynamoDbClient.transactWriteItems(
                new TransactWriteItemsRequest().withTransactItems(
                    toTestOrderWriteItem(tokens),
                    toTestResultItem(tokens)
                )
            );
        } catch (TransactionCanceledException e) {
            throw new RuntimeException("Persistence of test order was cancelled by remote DB service due to " + reasons(e));
        }
    }

    private String reasons(TransactionCanceledException e) {
        return e.getCancellationReasons()
            .stream()
            .map(CancellationReason::getMessage)
            .filter(Objects::nonNull)
            .collect(Collectors.joining(","));
    }

    private TransactWriteItem toTestOrderWriteItem(TokensGenerator.TestOrderTokens tokens) {
        Map<String, AttributeValue> testOrderData = new LinkedHashMap<>();
        testOrderData.put("ctaToken", new AttributeValue(tokens.ctaToken));
        testOrderData.put("testResultPollingToken", new AttributeValue(tokens.testResultPollingToken));
        testOrderData.put("diagnosisKeySubmissionToken", new AttributeValue(tokens.diagnosisKeySubmissionToken));

        return new TransactWriteItem().withPut(
            new Put().withTableName(testOrdersTableName).withItem(testOrderData)
        );
    }

    private TransactWriteItem toTestResultItem(TokensGenerator.TestOrderTokens tokens) {
        Map<String, AttributeValue> testResultData = new LinkedHashMap<>();
        testResultData.put("testResultPollingToken", new AttributeValue(tokens.testResultPollingToken));
        testResultData.put("status", new AttributeValue("pending"));

        return new TransactWriteItem().withPut(
            new Put().withTableName(testResultsTableName).withItem(testResultData)
        );
    }
}
