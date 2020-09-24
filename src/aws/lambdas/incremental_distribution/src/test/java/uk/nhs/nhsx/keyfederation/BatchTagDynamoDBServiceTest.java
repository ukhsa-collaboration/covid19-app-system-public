package uk.nhs.nhsx.keyfederation;

import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

public class BatchTagDynamoDBServiceTest {
    DynamoDB dynamoDb = Mockito.mock(DynamoDB.class);
    Table mockTable = Mockito.mock(Table.class);

    public void setUp() {
        Mockito.when(dynamoDb.getTable("table")).thenReturn(mockTable);
        Mockito.when(mockTable.getTableName()).thenReturn("table");
    }

    @Test
    public void canGetLatestBatchTag() {
        BatchTag batchTag = BatchTag.of("75b326f7-ae6f-42f6-9354-00c0a6b797b3");
        Item itemOutcome = new Item().withString("lastReceivedBatchTag", "75b326f7-ae6f-42f6-9354-00c0a6b797b3");
        Mockito.when(mockTable.getItem("id", 0)).thenReturn(itemOutcome);

        BatchTagDynamoDBService service = new BatchTagDynamoDBService("DUMMY_TABLE");
        BatchTag dbBatchTag = service.getLatestBatchTag(mockTable);

        assertEquals(batchTag, dbBatchTag);
    }

    @Test
    public void returnsNullWhenNoLatestBatchTag() {
        Mockito.when(mockTable.getItem("id", 2)).thenReturn(null);

        BatchTagDynamoDBService service = new BatchTagDynamoDBService("DUMMY_TABLE");
        BatchTag dbBatchTag = service.getLatestBatchTag(mockTable);

        assertNull(dbBatchTag);
    }

    @Test
    public void canUpdateLatestBatchTag() {
        BatchTag batchTag = BatchTag.of("75b326f7");
        BatchTagDynamoDBService mockService = Mockito.mock(BatchTagDynamoDBService.class);
        doAnswer(invocation -> {
            Object arg0 = invocation.getArgument(0);

            assertEquals("75b326f7", arg0.toString());
            return null;
        }).when(mockService).updateLatestBatchTag(batchTag, mockTable);
        mockService.updateLatestBatchTag(batchTag, mockTable);
        verify(mockService, times(1)).updateLatestBatchTag(batchTag, mockTable);
    }
}
