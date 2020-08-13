package uk.nhs.nhsx.testkitorder;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.Assert.assertEquals;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestKitOrderPersistenceServiceMockedTest {
    
    @Test
    public void getOrderReturningNullReturnsEmpty(){
        AmazonDynamoDB dynamoDbClient = mock(AmazonDynamoDB.class);
        TestKitOrderPersistenceService service = new TestKitOrderDynamoPersistenceService(dynamoDbClient, "", "");
        GetItemResult itemResult = mock(GetItemResult.class);
        when(itemResult.getItem()).thenReturn(null);
        when(dynamoDbClient.getItem(any())).thenReturn(itemResult);
        assertEquals(service.getTestResult(TestResultPollingToken.of("token")), Optional.empty());
    }

    @Test
    public void getOrderReturnsEmptyResultThrowsException(){
        AmazonDynamoDB dynamoDbClient = mock(AmazonDynamoDB.class);
        TestKitOrderPersistenceService service = new TestKitOrderDynamoPersistenceService(dynamoDbClient, "", "");
        GetItemResult itemResult = mock(GetItemResult.class);
        Map<String, AttributeValue> emptyMap = new HashMap<String, AttributeValue>(){};
        when(itemResult.getItem()).thenReturn(emptyMap);
        when(dynamoDbClient.getItem(any())).thenReturn(itemResult);
        assertThatThrownBy(() -> service.getTestResult(TestResultPollingToken.of("token")))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Required field missing");
    }

    @Test
    public void getOrderReturnsResultMissingTokenValueThrowsException(){
        AmazonDynamoDB dynamoDbClient = mock(AmazonDynamoDB.class);
        TestKitOrderPersistenceService service = new TestKitOrderDynamoPersistenceService(dynamoDbClient, "", "");
        GetItemResult itemResult = mock(GetItemResult.class);
        AttributeValue pollingTokenValue = mock(AttributeValue.class);
        AttributeValue statusValue = mock(AttributeValue.class);
        when(pollingTokenValue.getS()).thenReturn(null);
        when(statusValue.getS()).thenReturn("status");
        Map<String, AttributeValue> valueMap = new HashMap<String, AttributeValue>(){{
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
    public void getOrderReturnsResultMissingStatusValueThrowsException(){
        AmazonDynamoDB dynamoDbClient = mock(AmazonDynamoDB.class);
        TestKitOrderPersistenceService service = new TestKitOrderDynamoPersistenceService(dynamoDbClient, "", "");
        GetItemResult itemResult = mock(GetItemResult.class);
        AttributeValue pollingTokenValue = mock(AttributeValue.class);
        AttributeValue statusValue = mock(AttributeValue.class);
        when(pollingTokenValue.getS()).thenReturn("token");
        when(statusValue.getS()).thenReturn(null);
        Map<String, AttributeValue> valueMap = new HashMap<String, AttributeValue>(){{
            put("testResultPollingToken", pollingTokenValue);
            put("status", statusValue);
        }};
        when(itemResult.getItem()).thenReturn(valueMap);
        when(dynamoDbClient.getItem(any())).thenReturn(itemResult);
        assertThatThrownBy(() -> service.getTestResult(TestResultPollingToken.of("token")))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Required field missing");
    }
}
