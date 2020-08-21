package uk.nhs.nhsx.testkitorder;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.*;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class TestKitOrderPersistenceServiceMockedTest {

    private final AmazonDynamoDB dynamoDbClient = mock(AmazonDynamoDB.class);

    private final TestKitOrderConfig config = new TestKitOrderConfig(
        "testOrdersTableName",
        "testResultsTableName",
        "submissionTokensTableName",
        "orderWebsite",
        "registerWebsite",
        1
    );

    private final TestKitOrderPersistenceService service = new TestKitOrderDynamoService(
        dynamoDbClient,
        config
    );

    private final GetItemResult itemResult = mock(GetItemResult.class);

    @Test
    public void getOrderReturningNullReturnsEmpty() {
        when(itemResult.getItem()).thenReturn(null);
        when(dynamoDbClient.getItem(any())).thenReturn(itemResult);
        assertEquals(service.getTestResult(TestResultPollingToken.of("token")), Optional.empty());
    }

    @Test
    public void getOrderReturnsEmptyResultThrowsException() {
        @SuppressWarnings("serial")
        Map<String, AttributeValue> emptyMap = new HashMap<String, AttributeValue>() {
        };
        when(itemResult.getItem()).thenReturn(emptyMap);
        when(dynamoDbClient.getItem(any())).thenReturn(itemResult);
        assertThatThrownBy(() -> service.getTestResult(TestResultPollingToken.of("token")))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Required field missing");
    }

    @Test
    public void getOrderReturnsResultMissingTokenValueThrowsException() {
        AttributeValue pollingTokenValue = mock(AttributeValue.class);
        AttributeValue statusValue = mock(AttributeValue.class);
        when(pollingTokenValue.getS()).thenReturn(null);
        when(statusValue.getS()).thenReturn("status");
        @SuppressWarnings("serial")
        Map<String, AttributeValue> valueMap = new HashMap<String, AttributeValue>() {{
            put("testResultPollingToken", pollingTokenValue);
            put("status", statusValue);
        }};
        when(itemResult.getItem()).thenReturn(valueMap);
        when(dynamoDbClient.getItem(any())).thenReturn(itemResult);
        assertThatThrownBy(() -> service.getTestResult(TestResultPollingToken.of("token")))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Required field missing");
    }

    @Test
    public void getOrderReturnsResultMissingStatusValueThrowsException() {
        AttributeValue pollingTokenValue = mock(AttributeValue.class);
        AttributeValue statusValue = mock(AttributeValue.class);
        when(pollingTokenValue.getS()).thenReturn("token");
        when(statusValue.getS()).thenReturn(null);
        @SuppressWarnings("serial")
        Map<String, AttributeValue> valueMap = new HashMap<>() {{
            put("testResultPollingToken", pollingTokenValue);
            put("status", statusValue);
        }};
        when(itemResult.getItem()).thenReturn(valueMap);
        when(dynamoDbClient.getItem(any())).thenReturn(itemResult);
        assertThatThrownBy(() -> service.getTestResult(TestResultPollingToken.of("token")))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Required field missing");
    }


    @Test
    public void marksTestDataForDeletionWithCorrectTimeToLiveValues() {
        List<Map<String, AttributeValue>> items = scanResult();

        ScanResult scanResult = mock(ScanResult.class);
        when(scanResult.getItems()).thenReturn(items);
        when(dynamoDbClient.scan(any())).thenReturn(scanResult);

        ArgumentCaptor<TransactWriteItemsRequest> captor = ArgumentCaptor.forClass(TransactWriteItemsRequest.class);

        service.markForDeletion(
            new VirologyDataTimeToLive(TestResultPollingToken.of("some-token"), 1, 10)
        );

        verify(dynamoDbClient, times(1)).transactWriteItems(captor.capture());

        List<TransactWriteItem> transactItems = captor.getValue().getTransactItems();
        assertThat(transactItems).hasSize(3);

        Update testOrderUpdate = transactItems.get(0).getUpdate();
        assertThat(testOrderUpdate.getTableName()).isEqualTo("testOrdersTableName");
        assertThat(testOrderUpdate.getExpressionAttributeValues()).isEqualTo(expireAtWithTtl("1"));

        Update testResultUpdate = transactItems.get(1).getUpdate();
        assertThat(testResultUpdate.getTableName()).isEqualTo("testResultsTableName");
        assertThat(testResultUpdate.getExpressionAttributeValues()).isEqualTo(expireAtWithTtl("1"));

        Update submissionUpdate = transactItems.get(2).getUpdate();
        assertThat(submissionUpdate.getTableName()).isEqualTo("submissionTokensTableName");
        assertThat(submissionUpdate.getExpressionAttributeValues()).isEqualTo(expireAtWithTtl("10"));
    }

    private List<Map<String, AttributeValue>> scanResult() {
        Map<String, AttributeValue> item = new LinkedHashMap<>();
        item.put("ctaToken", new AttributeValue("some-cta-token"));
        item.put("diagnosisKeySubmissionToken", new AttributeValue("some-submission-token"));
        return Collections.singletonList(item);
    }

    private Map<String, AttributeValue> expireAtWithTtl(String s) {
        Map<String, AttributeValue> testExpireAt = new LinkedHashMap<>();
        testExpireAt.put(":expireAt", new AttributeValue().withN(s));
        return testExpireAt;
    }

}
