package uk.nhs.nhsx.testresultsupload;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import org.junit.Ignore;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Ignore("dependency on dynamo db should only be run manually")
public class NPEXTestResultPersistenceServiceTest {

    private final AmazonDynamoDB dynamoDbClient = AmazonDynamoDBClientBuilder.defaultClient();
    private final DynamoDB dynamoDb = new DynamoDB(dynamoDbClient);

    // Test table names need to be set for tests
    private final String submissionTokensTableName = "te-ci-testing-submissiontokens";
    private final String testResultsTableName = "te-ci-testing-testresults";
    private final String testOrdersTableName = "te-ci-testing-ordertokens";

    private final NPEXTestResultPersistenceService service = new NPEXTestResultPersistenceService(dynamoDbClient, submissionTokensTableName, testResultsTableName, testOrdersTableName);

    @Test
    public void persistPositiveResult() {
        dynamoDb.getTable(testOrdersTableName).putItem(
                new Item().withPrimaryKey("ctaToken", "ctaToken123")
                        .with("diagnosisKeySubmissionToken", "dks-123")
                        .with("testResultPollingToken", "trp-123")
        );

        NPEXTestResult testResult =
            new NPEXTestResult("ctaToken123", "2020-04-23T18:34:03Z", "POSITIVE");

        service.persistPositiveTestResult(testResult);

        Item testResultItem = dynamoDb.getTable(testResultsTableName).getItem("testResultPollingToken", "trp-123");
        assertThat(testResultItem.getString("status")).isEqualTo("available");
        assertThat(testResultItem.getString("testEndDate")).isEqualTo("2020-04-23T18:34:03Z");
        assertThat(testResultItem.getString("testResult")).isEqualTo("POSITIVE");

        Item submissionTokenItem  = dynamoDb.getTable(submissionTokensTableName).getItem("diagnosisKeySubmissionToken", "dks-123");
        assertThat(submissionTokenItem).isNotNull();
    }

    @Test
    public void persistNegativeResult() {
        dynamoDb.getTable(testOrdersTableName).putItem(
                new Item().withPrimaryKey("ctaToken", "ctaToken789")
                        .with("diagnosisKeySubmissionToken", "dks-789")
                        .with("testResultPollingToken", "trp-789")
        );

        NPEXTestResult testResult =
            new NPEXTestResult("ctaToken789", "2020-04-23T18:34:03Z", "NEGATIVE");

        service.persistNegativeOrVoidTestResult(testResult);

        Item testResultItem = dynamoDb.getTable(testResultsTableName).getItem("testResultPollingToken", "trp-789");
        assertThat(testResultItem.getString("status")).isEqualTo("available");
        assertThat(testResultItem.getString("testEndDate")).isEqualTo("2020-04-23T18:34:03Z");
        assertThat(testResultItem.getString("testResult")).isEqualTo("NEGATIVE");

        Item submissionTokenItem  = dynamoDb.getTable(submissionTokensTableName).getItem("diagnosisKeySubmissionToken", "dks-789");
        assertThat(submissionTokenItem).isNull();
    }
}
