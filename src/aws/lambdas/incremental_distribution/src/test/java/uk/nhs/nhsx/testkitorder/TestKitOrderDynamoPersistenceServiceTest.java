package uk.nhs.nhsx.testkitorder;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Ignore("dependency on dynamo db should only be run manually")
public class TestKitOrderDynamoPersistenceServiceTest {

    private final AmazonDynamoDB dynamoDbClient = AmazonDynamoDBClientBuilder.defaultClient();
    private final DynamoDB dynamoDb = new DynamoDB(dynamoDbClient);

    // Test table names need to be set for tests
    private final String submissionTokensTableName = "dc1bb3-testing-submissiontokens";
    private final String testResultsTableName = "dc1bb3-testing-testresults";
    private final String testOrdersTableName = "dc1bb3-testing-ordertokens";

    private final TestKitOrderDynamoPersistenceService service = new TestKitOrderDynamoPersistenceService(AmazonDynamoDBClientBuilder.defaultClient(), testOrdersTableName, testResultsTableName);

    @Test
    public void findResultAvailable() {

        TestResultPollingToken testResultPollingToken = TestResultPollingToken.of(UUID.randomUUID().toString());

        dynamoDb.getTable(testResultsTableName).putItem(
            new Item().withPrimaryKey("testResultPollingToken", testResultPollingToken)
                .with("status", "available")
                .with("testResult", "foo-bar")
                .with("testEndDate", "2020-04-23T00:00:00Z")
        );

        assertThat(
            service.getTestResult(testResultPollingToken)
        ).isNotEmpty().map(it -> it.testResult).hasValue("foo-bar");
    }

    @Test
    public void findResultPending() {

        TestResultPollingToken testResultPollingToken = TestResultPollingToken.of(UUID.randomUUID().toString());

        dynamoDb.getTable(testResultsTableName).putItem(
            new Item().withPrimaryKey("testResultPollingToken", testResultPollingToken)
                .with("status", "pending")
        );

        assertThat(
            service.getTestResult(testResultPollingToken)
        ).isNotEmpty().map(it -> it.status).hasValue("pending");
    }


    @Test
    public void noResult() {
        TestResultPollingToken testResultPollingToken = TestResultPollingToken.of(UUID.randomUUID().toString());

        assertThat(
            service.getTestResult(testResultPollingToken)
        ).isEmpty();
    }

    @Test
    public void persistTestOrder() {

        TokensGenerator.TestOrderTokens tokens = new TokensGenerator().generateTokens();
        service.persistTestOrder(tokens);

        Item testResultItem = dynamoDb.getTable(testResultsTableName).getItem("testResultPollingToken", tokens.testResultPollingToken);
        assertThat(testResultItem.getString("status")).isEqualTo("pending");
        assertThat(testResultItem.getString("testEndDate")).isNull();
        assertThat(testResultItem.getString("testResult")).isNull();

        Item testOrderItem = dynamoDb.getTable(testOrdersTableName).getItem("ctaToken", tokens.ctaToken);
        assertThat(testOrderItem.getString("ctaToken")).isEqualTo(tokens.ctaToken);
        assertThat(testOrderItem.getString("diagnosisKeySubmissionToken")).isEqualTo(tokens.diagnosisKeySubmissionToken);
        assertThat(testOrderItem.getString("testResultPollingToken")).isEqualTo(tokens.testResultPollingToken);

        Optional<TestResult> testResult = service.getTestResult(TestResultPollingToken.of(tokens.testResultPollingToken));
        assertThat(testResult).isNotEmpty().map(it -> it.status).hasValue("pending");

    }
}
