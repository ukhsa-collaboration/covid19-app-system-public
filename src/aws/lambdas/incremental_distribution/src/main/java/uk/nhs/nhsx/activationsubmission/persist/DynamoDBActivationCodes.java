package uk.nhs.nhsx.activationsubmission.persist;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.*;
import uk.nhs.nhsx.activationsubmission.ActivationCode;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class DynamoDBActivationCodes implements PersistedActivationCodeLookup {

    private final AmazonDynamoDB db;
    private final Supplier<Instant> clock;
    private final String tableName;

    public DynamoDBActivationCodes(AmazonDynamoDB db, TableNamingStrategy tableNamingStrategy, Supplier<Instant> clock) {
        this.db = db;
        this.clock = clock;
        this.tableName = tableNamingStrategy.apply("ActivationCodes");
    }

    @Override
    public Optional<PersistedActivationCode> find(ActivationCode code) {

        AtomicBoolean alreadyUsed = new AtomicBoolean(false);

        try {
            UpdateItemRequest request = new UpdateItemRequest()
                .withTableName(tableName)
                .addKeyEntry("Code", new AttributeValue(code.value))
                .addAttributeUpdatesEntry("Activated", new AttributeValueUpdate(new AttributeValue().withN(epochMilli()), AttributeAction.PUT))
                .addExpectedEntry("Code", new ExpectedAttributeValue(new AttributeValue(code.value)))
                .addExpectedEntry("Activated", new ExpectedAttributeValue(false));

            db.updateItem(request);
        } catch (ConditionalCheckFailedException e) {
            // doesn't exist or activated already, we don't know which here, but GetItem will fail if it doesn't exist
            alreadyUsed.set(true);
        } catch (ResourceNotFoundException e) {
            // annoyingly resource error message doesn't have the table name in it.
            throw new RuntimeException("Cannot find table " + tableName, e);
        }

        GetItemRequest request = new GetItemRequest()
            .withTableName(tableName)
            .addKeyEntry("Code", new AttributeValue(code.value));

        GetItemResult result = db.getItem(request);

        return Optional.ofNullable(result.getItem())
            .map(
                attributes ->
                    new PersistedActivationCode(
                        attributeToInstant(attributes.get("Created").getN()),
                        ActivationCodeBatchName.of(attributes.get("Batch").getS()),
                        ActivationCode.of(attributes.get("Code").getS()),
                        attributeToInstant(attributes.get("Activated").getN()),
                        alreadyUsed.get()
                    )
            );
    }

    private Instant attributeToInstant(String activated) {
        return Instant.ofEpochMilli(Long.parseLong(activated));
    }

    private String epochMilli() {
        return String.valueOf(clock.get().toEpochMilli());
    }

    public boolean insert(ActivationCodeBatchName batchName, ActivationCode code) {
        try {
            UpdateItemRequest request = new UpdateItemRequest()
                .withTableName(tableName)
                .withReturnValues(ReturnValue.ALL_NEW)
                .addKeyEntry("Code", new AttributeValue(code.value))
                .addAttributeUpdatesEntry("Created", new AttributeValueUpdate(new AttributeValue().withN(epochMilli()), AttributeAction.PUT))
                .addAttributeUpdatesEntry("Batch", new AttributeValueUpdate(new AttributeValue(batchName.value), AttributeAction.PUT))
                .addExpectedEntry("Code", new ExpectedAttributeValue(false));

            db.updateItem(request);
            return true;
        } catch (ConditionalCheckFailedException e) {
            // duplicate
            return false;
        }
    }
}
