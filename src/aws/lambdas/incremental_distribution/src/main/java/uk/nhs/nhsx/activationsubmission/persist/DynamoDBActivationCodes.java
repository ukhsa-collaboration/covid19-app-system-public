package uk.nhs.nhsx.activationsubmission.persist;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.*;
import uk.nhs.nhsx.activationsubmission.ActivationCode;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static com.amazonaws.services.dynamodbv2.model.AttributeAction.PUT;
import static uk.nhs.nhsx.core.aws.dynamodb.DynamoAttributes.*;

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

        long activationEpochMilli = epochMilli();

        try {
            UpdateItemRequest request = new UpdateItemRequest()
                .withTableName(tableName)
                .addKeyEntry("Code", stringAttribute(code.value))
                .addAttributeUpdatesEntry("Activated", attributeValueUpdate(numericAttribute(activationEpochMilli), PUT))
                .addExpectedEntry("Code", expectedAttributeExists(stringAttribute(code.value)))
                .addExpectedEntry("Activated", expectedAttributeDoesNotExist());

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
            .addKeyEntry("Code", stringAttribute(code.value));

        GetItemResult result = db.getItem(request);

        // due to eventually consistent read, the Activated time *may* not have been set by this point
        // however, we can assume that if it is null, then it was just set in the block above, so we can use the same time.

        return Optional.ofNullable(result.getItem())
            .map(
                attributes ->
                    new PersistedActivationCode(
                        attributeToInstant(attributes.get("Created").getN()),
                        ActivationCodeBatchName.of(attributes.get("Batch").getS()),
                        ActivationCode.of(attributes.get("Code").getS()),
                        Instant.ofEpochMilli(
                            Optional.ofNullable(attributes.get("Activated"))
                            .map(AttributeValue::getN)
                            .map(Long::parseLong)
                            .orElse(activationEpochMilli)
                        ),
                        alreadyUsed.get()
                    )
            );
    }

    private Instant attributeToInstant(String activated) {
        return Instant.ofEpochMilli(Long.parseLong(activated));
    }

    private long epochMilli() {
        return clock.get().toEpochMilli();
    }

    public boolean insert(ActivationCodeBatchName batchName, ActivationCode code) {
        try {
            UpdateItemRequest request = new UpdateItemRequest()
                .withTableName(tableName)
                .withReturnValues(ReturnValue.ALL_NEW)
                .addKeyEntry("Code", stringAttribute(code.value))
                .addAttributeUpdatesEntry("Created", attributeValueUpdate(numericAttribute(epochMilli()), PUT))
                .addAttributeUpdatesEntry("Batch", attributeValueUpdate(stringAttribute(batchName.value), PUT))
                .addExpectedEntry("Code", expectedAttributeDoesNotExist());

            db.updateItem(request);
            return true;
        } catch (ConditionalCheckFailedException e) {
            // duplicate
            return false;
        }
    }
}
