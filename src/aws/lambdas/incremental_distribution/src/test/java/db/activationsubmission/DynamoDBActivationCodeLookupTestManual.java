package db.activationsubmission;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.*;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import uk.nhs.nhsx.activationsubmission.ActivationCode;
import uk.nhs.nhsx.activationsubmission.persist.ActivationCodeBatchName;
import uk.nhs.nhsx.activationsubmission.persist.DynamoDBActivationCodes;
import uk.nhs.nhsx.activationsubmission.persist.PersistedActivationCode;
import uk.nhs.nhsx.activationsubmission.persist.TableNamingStrategy;
import uk.nhs.nhsx.activationsubmission.reporting.DynamoDbReporting;
import uk.nhs.nhsx.core.SystemClock;
import uk.nhs.nhsx.core.random.crockford.CrockfordDammRandomStringGenerator;

import java.time.Instant;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;

public class DynamoDBActivationCodeLookupTestManual {

    AmazonDynamoDB db = AmazonDynamoDBClientBuilder
            .standard()
//            .withRegion("eu-west-2")
            .withEndpointConfiguration(
                    new AwsClientBuilder.EndpointConfiguration("http://localhost:8000", "eu-west-2")
            )
            .build();

    TableNamingStrategy tableNamingStrategy = TableNamingStrategy.PREFIX.apply("manual-testing-");

    DynamoDBActivationCodes activationCodes = new DynamoDBActivationCodes(db, tableNamingStrategy, SystemClock.CLOCK);
    DynamoDbReporting reporting = new DynamoDbReporting(db, tableNamingStrategy, SystemClock.CLOCK);

    @Before
    public void onlyRunIfManualTesting() throws Exception {
        Assume.assumeThat("Running manual testing", System.getenv("MANUAL_TESTING"), not(CoreMatchers.nullValue()));
    }

    @Test
    public void createTheAnalyticsTable() throws Exception {
        db.createTable(
            new CreateTableRequest()
                .withTableName(tableNamingStrategy.apply("ActivationCode-Reporting"))
                .withKeySchema(
                    new KeySchemaElement("Date", KeyType.HASH),
                    new KeySchemaElement("Batch", KeyType.RANGE)
                )
                .withAttributeDefinitions(
                    new AttributeDefinition("Date", ScalarAttributeType.S),
                    new AttributeDefinition("Batch", ScalarAttributeType.S)
                )
                .withBillingMode(BillingMode.PAY_PER_REQUEST)
        );
    }

    @Test
    public void createTheTable() throws Exception {
        db.createTable(
                new CreateTableRequest()
                        .withTableName(tableNamingStrategy.apply("ActivationCodes"))
                        .withKeySchema(
                                new KeySchemaElement("Code", KeyType.HASH)
                        )
                        .withAttributeDefinitions(
                                new AttributeDefinition("Code", ScalarAttributeType.S)
                        )
                        .withBillingMode(BillingMode.PAY_PER_REQUEST)
        );
    }

    ActivationCode code = ActivationCode.of("code-8");

    @Test
    public void accepted() throws Exception {
        reporting.accepted(new PersistedActivationCode(
            Instant.now(), ActivationCodeBatchName.of("batch-1"), ActivationCode.of("1234"), Instant.now(),false
        ));
    }

    @Test
    public void expired() throws Exception {
        reporting.expired(new PersistedActivationCode(
            Instant.now(), ActivationCodeBatchName.of("batch-1"), ActivationCode.of("1234"), Instant.now(), false
        ));
    }

    @Test
    public void unknown() throws Exception {
        reporting.missing();
    }

    @Test
    public void insertARow() throws Exception {
        MatcherAssert.assertThat(activationCodes.insert(ActivationCodeBatchName.of("batch"), code), is(true));
    }

    @Test
    public void insertABunchOfRows() throws Exception {
        CrockfordDammRandomStringGenerator generator = new CrockfordDammRandomStringGenerator();
        for ( int i = 0 ; i < 1000 ; i++ ) {
            activationCodes.insert(ActivationCodeBatchName.of("batch000"), ActivationCode.of(generator.generate()));
        }
    }

    @Test
    public void selectARow() throws Exception {

        Optional<PersistedActivationCode> persistedActivationCode = activationCodes.find(code);

        System.out.println(persistedActivationCode);

    }
}