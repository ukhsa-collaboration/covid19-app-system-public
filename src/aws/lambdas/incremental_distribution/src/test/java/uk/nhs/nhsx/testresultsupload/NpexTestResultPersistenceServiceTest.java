package uk.nhs.nhsx.testresultsupload;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import org.junit.Ignore;
import org.junit.Test;

import java.time.Clock;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@Ignore("dependency on dynamo db should only be run manually")
public class NpexTestResultPersistenceServiceTest {

    private final AmazonDynamoDB dynamoDbClient = AmazonDynamoDBClientBuilder.defaultClient();
    private final DynamoDB dynamoDb = new DynamoDB(dynamoDbClient);

    private final NpexTestResultConfig config =
        new NpexTestResultConfig(
            "te-ci-virology-submissiontokens",
            "te-ci-virology-testresults",
            "te-ci-virology-ordertokens"
        );

    private final NpexTestResultPersistenceService service =
        new NpexTestResultPersistenceService(dynamoDbClient, config);

    @Test
    public void persistPositiveResult() {
        String ctaToken = "ctaToken123";
        String diagnosisKeySubToken = "dks-123";
        String testResultPollingToken = "trp-123";
        long testOrderExpireAt = Clock.systemUTC().instant().plus(Duration.ofMinutes(10)).getEpochSecond();

        // create test order
        dynamoDb.getTable(config.testOrdersTable)
            .putItem(
                new Item()
                    .withPrimaryKey("ctaToken", ctaToken)
                    .with("diagnosisKeySubmissionToken", diagnosisKeySubToken)
                    .with("testResultPollingToken", testResultPollingToken)
                    .with("expireAt", testOrderExpireAt)
            );

        // create test result
        dynamoDb.getTable(config.testResultsTable)
            .putItem(
                new Item()
                    .withPrimaryKey("testResultPollingToken", testResultPollingToken)
                    .with("status", "pending")
                    .with("expireAt", testOrderExpireAt)
            );

        // update test result and creates the submission token
        NpexTestResult testResult = new NpexTestResult(ctaToken, "2020-04-23T18:34:03Z", "POSITIVE");
        long testResultExpireAt = Clock.systemUTC().instant().plus(Duration.ofMinutes(20)).getEpochSecond();
        service.persistPositiveTestResult(NpexTestResult.Positive.from(testResult), testResultExpireAt);

        // get and assert from test result table
        Item testResultItem = dynamoDb.getTable(config.testResultsTable)
            .getItem("testResultPollingToken", testResultPollingToken);

        assertThat(testResultItem.getString("status")).isEqualTo("available");
        assertThat(testResultItem.getString("testEndDate")).isEqualTo("2020-04-23T18:34:03Z");
        assertThat(testResultItem.getString("testResult")).isEqualTo("POSITIVE");
        assertThat(testResultItem.getString("expireAt")).isEqualTo(String.valueOf(testOrderExpireAt));

        // get and assert from submission tokens table
        Item submissionTokenItem =
            dynamoDb.getTable(config.submissionTokensTable)
                .getItem("diagnosisKeySubmissionToken", diagnosisKeySubToken);

        assertThat(submissionTokenItem.getString("diagnosisKeySubmissionToken")).isEqualTo(diagnosisKeySubToken);
        assertThat(submissionTokenItem.getString("expireAt")).isEqualTo(String.valueOf(testResultExpireAt));
    }

    @Test
    public void persistNegativeResult() {
        dynamoDb.getTable(config.testOrdersTable)
            .putItem(
                new Item()
                    .withPrimaryKey("ctaToken", "ctaToken789")
                    .with("diagnosisKeySubmissionToken", "dks-789")
                    .with("testResultPollingToken", "trp-789")
            );

        NpexTestResult testResult =
            new NpexTestResult("ctaToken789", "2020-04-23T18:34:03Z", "NEGATIVE");

        service.persistNonPositiveTestResult(NpexTestResult.NonPositive.from(testResult));

        Item testResultItem = dynamoDb
            .getTable(config.testResultsTable)
            .getItem("testResultPollingToken", "trp-789");

        assertThat(testResultItem.getString("status")).isEqualTo("available");
        assertThat(testResultItem.getString("testEndDate")).isEqualTo("2020-04-23T18:34:03Z");
        assertThat(testResultItem.getString("testResult")).isEqualTo("NEGATIVE");

        Item submissionTokenItem =
            dynamoDb.getTable(config.submissionTokensTable)
                .getItem("diagnosisKeySubmissionToken", "dks-789");
        assertThat(submissionTokenItem).isNull();
    }
}
