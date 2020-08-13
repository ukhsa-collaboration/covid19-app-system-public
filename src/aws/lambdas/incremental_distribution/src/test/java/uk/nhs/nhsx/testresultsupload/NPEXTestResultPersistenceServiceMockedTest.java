package uk.nhs.nhsx.testresultsupload;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.*;
import org.junit.Test;
import uk.nhs.nhsx.core.exceptions.ApiResponseException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class NPEXTestResultPersistenceServiceMockedTest {

    private final AmazonDynamoDB dynamoDbClient = mock(AmazonDynamoDB.class);
    private final NPEXTestResultPersistenceService service =
        new NPEXTestResultPersistenceService(dynamoDbClient, "", "", "");

    @Test
    public void persistTransactionItemsExceptionCauses500(){
        List<TransactWriteItem> items = new ArrayList<>();
        CancellationReason reason = new CancellationReason();
        reason.setMessage("reason");
        List<CancellationReason> reasons = new ArrayList<CancellationReason>(){{add(reason);}};
        TransactionCanceledException exception = new TransactionCanceledException("message").withCancellationReasons(reasons);
        when(dynamoDbClient.transactWriteItems(any(TransactWriteItemsRequest.class))).thenThrow(exception);

        assertThatThrownBy(() -> service.persistTransactionItems(items))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Failed to persist test result to database with due to database exception: reason");
    }

    @Test
    public void getOrderReturningNullItemCauses400(){
        GetItemResult itemResult = mock(GetItemResult.class);
        when(dynamoDbClient.getItem(anyString(), anyMap())).thenReturn(itemResult);
        when(itemResult.getItem()).thenReturn(null);
        NPEXTestResult testResult = new NPEXTestResult("some-token", "2020-04-23T18:34:03Z", "POSITIVE");

        assertThatThrownBy(() -> service.persistNegativeOrVoidTestResult(testResult))
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

        NPEXTestResult testResult = new NPEXTestResult("some-token", "2020-04-23T18:34:03Z", "POSITIVE");

        assertThatThrownBy(() -> service.persistNegativeOrVoidTestResult(testResult))
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
        Map<String, AttributeValue> map = new HashMap<String, AttributeValue>(){{
            put("testResultPollingToken", pollingTokenValue);
            put("diagnosisKeySubmissionToken", submissionTokenValue);
        }};
        when(itemResult.getItem()).thenReturn(map);
        when(dynamoDbClient.getItem(anyString(), anyMap())).thenReturn(itemResult);

        NPEXTestResult testResult = new NPEXTestResult("some-token", "2020-04-23T18:34:03Z", "POSITIVE");

        assertThatThrownBy(() -> service.persistNegativeOrVoidTestResult(testResult))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Required field missing");
        verify(dynamoDbClient, never()).transactWriteItems(any(TransactWriteItemsRequest.class));
    }
}
