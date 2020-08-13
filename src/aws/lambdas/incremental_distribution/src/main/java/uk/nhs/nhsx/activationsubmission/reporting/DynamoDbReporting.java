package uk.nhs.nhsx.activationsubmission.reporting;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeAction;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import uk.nhs.nhsx.activationsubmission.persist.ActivationCodeBatchName;
import uk.nhs.nhsx.activationsubmission.persist.PersistedActivationCode;
import uk.nhs.nhsx.activationsubmission.persist.TableNamingStrategy;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

public class DynamoDbReporting implements PersistedActivationCodeReporting {

    private final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;

    private final AmazonDynamoDB db;
    private final Supplier<Instant> clock;
    private final String tableName;

    public DynamoDbReporting(AmazonDynamoDB db, TableNamingStrategy tableNamingStrategy, Supplier<Instant> clock) {
        this.db = db;
        this.clock = clock;
        this.tableName = tableNamingStrategy.apply("ActivationCode-Reporting");
    }

    private ActivationCodeBatchName NONE = ActivationCodeBatchName.of("NONE");

    @Override
    public void missing() {
        incrementBatchCount(NONE, "Unknown");
    }

    @Override
    public void accepted(PersistedActivationCode code) {
        incrementBatchCount(code.batchName, "Accepted");
    }

    @Override
    public void expired(PersistedActivationCode code) {
        incrementBatchCount(code.batchName, "Expired");
    }

    private void incrementBatchCount(ActivationCodeBatchName batch, String increment) {
        UpdateItemRequest request = new UpdateItemRequest()
            .withTableName(tableName)
            .addKeyEntry("Date", new AttributeValue(today()))
            .addKeyEntry("Batch", new AttributeValue(batch.value))
            .addAttributeUpdatesEntry(increment, new AttributeValueUpdate().withValue(new AttributeValue().withN("1")).withAction(AttributeAction.ADD));
        db.updateItem(request);
    }

    private String today() {
        return formatter.format(ZonedDateTime.ofInstant(clock.get(), ZoneId.of("UTC")));
    }
}
