package uk.nhs.nhsx.testresultsupload;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.*;
import org.junit.Test;
import uk.nhs.nhsx.core.aws.dynamodb.DynamoTransactions;
import uk.nhs.nhsx.core.exceptions.ApiResponseException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class NpexTestResultPersistenceServiceMockedTest {

    private final AmazonDynamoDB dynamoDbClient = mock(AmazonDynamoDB.class);
    private final NpexTestResultConfig npexTestResultConfig =
        new NpexTestResultConfig("a", "b", "c");
    private final NpexTestResultPersistenceService service =
        new NpexTestResultPersistenceService(dynamoDbClient, npexTestResultConfig);

    @Test
    public void persistTransactionItemsExceptionCauses500(){
        List<TransactWriteItem> items = new ArrayList<>();
        CancellationReason reason = new CancellationReason();
        reason.setMessage("reason");
        @SuppressWarnings("serial")
		List<CancellationReason> reasons = new ArrayList<>() {{
            add(reason);
        }};
        TransactionCanceledException exception = new TransactionCanceledException("message").withCancellationReasons(reasons);
        when(dynamoDbClient.transactWriteItems(any(TransactWriteItemsRequest.class))).thenThrow(exception);

        assertThatThrownBy(() -> DynamoTransactions.executeTransaction(dynamoDbClient, items))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Transaction cancelled by remote DB service due to: reason");
    }

    @Test
    public void getOrderReturningNullItemCauses400(){
        GetItemResult itemResult = mock(GetItemResult.class);
        when(dynamoDbClient.getItem(anyString(), anyMap())).thenReturn(itemResult);
        when(itemResult.getItem()).thenReturn(null);
        NpexTestResult testResult = new NpexTestResult("some-token", "2020-04-23T18:34:03Z", "POSITIVE");

        assertThatThrownBy(() -> service.persistNonPositiveTestResult(NpexTestResult.NonPositive.from(testResult)))
            .isInstanceOf(ApiResponseException.class)
            .hasMessage("Bad request: test order token not found");
        verify(dynamoDbClient, never()).transactWriteItems(any(TransactWriteItemsRequest.class));
    }

    @Test
    public void getOrderReturningNullTokenValueCauses400(){
        GetItemResult itemResult = mock(GetItemResult.class);
        Map<String, AttributeValue> emptyMap = new HashMap<>();
        when(itemResult.getItem()).thenReturn(emptyMap);
        when(dynamoDbClient.getItem(anyString(), anyMap())).thenReturn(itemResult);

        NpexTestResult testResult = new NpexTestResult("some-token", "2020-04-23T18:34:03Z", "POSITIVE");

        assertThatThrownBy(() -> service.persistNonPositiveTestResult(NpexTestResult.NonPositive.from(testResult)))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Required field missing");
        verify(dynamoDbClient, never()).transactWriteItems(any(TransactWriteItemsRequest.class));
    }

    @Test
    public void getOrderReturningNullTokenStringCauses400(){
        GetItemResult itemResult = mock(GetItemResult.class);
        AttributeValue pollingTokenValue = mock(AttributeValue.class);
        AttributeValue submissionTokenValue = mock(AttributeValue.class);
        when(pollingTokenValue.getS()).thenReturn(null);
        when(submissionTokenValue.getS()).thenReturn(null);
        @SuppressWarnings("serial")
		Map<String, AttributeValue> map = new HashMap<String, AttributeValue>(){{
            put("testResultPollingToken", pollingTokenValue);
            put("diagnosisKeySubmissionToken", submissionTokenValue);
        }};
        when(itemResult.getItem()).thenReturn(map);
        when(dynamoDbClient.getItem(anyString(), anyMap())).thenReturn(itemResult);

        NpexTestResult testResult = new NpexTestResult("some-token", "2020-04-23T18:34:03Z", "POSITIVE");

        assertThatThrownBy(() -> service.persistNonPositiveTestResult(NpexTestResult.NonPositive.from(testResult)))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Required field missing");
        verify(dynamoDbClient, never()).transactWriteItems(any(TransactWriteItemsRequest.class));
    }
}
