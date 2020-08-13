package uk.nhs.nhsx.testresultsupload;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.nhs.nhsx.core.exceptions.ApiResponseException;
import uk.nhs.nhsx.core.exceptions.HttpStatusCode;
import uk.nhs.nhsx.diagnosiskeyssubmission.Handler;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;

public class NPEXTestResultPersistenceService {

    private static class Order {
        final String testResultPollingToken;
        final String diagnosisKeySubmissionToken;

        Order(String testResultPollingToken, String diagnosisKeySubmissionToken) {
            this.testResultPollingToken = testResultPollingToken;
            this.diagnosisKeySubmissionToken = diagnosisKeySubmissionToken;
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(Handler.class);
    private final AmazonDynamoDB dynamoDbClient;

    private final String submissionTokensTableName;

    private final String testResultsTableName;

    private final String testOrdersTableName;

    NPEXTestResultPersistenceService(
            AmazonDynamoDB dynamoDbClient,
            String submissionTokensTableName,
            String testResultsTableName,
            String testOrdersTableName
    ) {
        this.dynamoDbClient = dynamoDbClient;
        this.submissionTokensTableName = submissionTokensTableName;
        this.testResultsTableName = testResultsTableName;
        this.testOrdersTableName = testOrdersTableName;
    }

    public void persistPositiveTestResult(NPEXTestResult testResult) {
        if (!"POSITIVE".equals(testResult.testResult)) {
            throw new UnsupportedOperationException("Cannot persist non-positive test result");
        }
        getOrder(testResult.ctaToken).map(order ->
            persistTransactionItems(
                Arrays.asList(toResultPollingTokenWriteItem(order, testResult), toSubmissionTokenWriteItem(order))
            )
        ).orElseThrow(() -> {
            logger.warn("Failed to persist test result to database. Call to get test order table did not return complete result.");
            return new ApiResponseException(HttpStatusCode.BAD_REQUEST_400, "Bad request: test order token not found");
        });
    }

    public void persistNegativeOrVoidTestResult(NPEXTestResult testResult) {
        getOrder(testResult.ctaToken).map(order ->
            persistTransactionItems(
                singletonList(toResultPollingTokenWriteItem(order, testResult))
            )
        ).orElseThrow( () -> {
            logger.warn("Failed to persist test result to database. Call to get test order table did not return complete result.");
            return new ApiResponseException(HttpStatusCode.BAD_REQUEST_400, "Bad request: test order token not found");
        });
    }

    public TransactWriteItemsResult persistTransactionItems(List<TransactWriteItem> items){
        try{
            TransactWriteItemsRequest transactWriteItemsRequest = new TransactWriteItemsRequest().withTransactItems(items);
            return dynamoDbClient.transactWriteItems(transactWriteItemsRequest);
        }
        catch(TransactionCanceledException e) {
            throw new RuntimeException("Failed to persist test result to database with due to database exception: " + reasons(e));
        }
    }

    private TransactWriteItem toResultPollingTokenWriteItem(Order order, NPEXTestResult testResult) {
        Map<String, AttributeValue> testResultData = new LinkedHashMap<>();
        testResultData.put("testResultPollingToken", new AttributeValue(order.testResultPollingToken));
        testResultData.put("status", new AttributeValue("available"));
        testResultData.put("testEndDate", new AttributeValue(testResult.testEndDate));
        testResultData.put("testResult", new AttributeValue(testResult.testResult));

        return new TransactWriteItem().withPut(new Put().withTableName(testResultsTableName)
                .withItem(testResultData));
    }

    private TransactWriteItem toSubmissionTokenWriteItem(Order order) {
        Map<String, AttributeValue> tokenData = singletonMap("diagnosisKeySubmissionToken", new AttributeValue(order.diagnosisKeySubmissionToken));
        Put createSubmissionToken = new Put().withTableName(submissionTokensTableName)
                .withItem(tokenData);
        return new TransactWriteItem().withPut(createSubmissionToken);
    }

    private Optional<Order> getOrder(String ctaToken) {
        GetItemResult itemResult = dynamoDbClient.getItem(testOrdersTableName, singletonMap("ctaToken", new AttributeValue(ctaToken)));
        return Optional.ofNullable(itemResult.getItem())
            .map(it -> new Order(
                requireString(it, "testResultPollingToken"),
                requireString(it, "diagnosisKeySubmissionToken")));
    }

    private static String requireString(Map<String, AttributeValue> item, String key) {
        return Optional.ofNullable(item.get(key)).flatMap(attributeValue -> Optional.ofNullable(attributeValue.getS()))
            .orElseThrow(() -> new RuntimeException("Required field missing"));
    }

    private String reasons(TransactionCanceledException e) {
        return e.getCancellationReasons()
            .stream()
            .map(CancellationReason::getMessage)
            .filter(Objects::nonNull)
            .collect(Collectors.joining(","));
    }

}
