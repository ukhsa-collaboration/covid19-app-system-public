package uk.nhs.nhsx.isolationpayment;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.model.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.nhs.nhsx.core.DateFormatValidator;
import uk.nhs.nhsx.core.SystemClock;
import uk.nhs.nhsx.isolationpayment.model.IsolationResponse;
import uk.nhs.nhsx.isolationpayment.model.IsolationToken;
import uk.nhs.nhsx.isolationpayment.model.TokenStatus;
import uk.nhs.nhsx.isolationpayment.model.TokenUpdateRequest;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.chrono.ChronoZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static uk.nhs.nhsx.core.aws.dynamodb.DynamoAttributes.*;

public class IsolationDynamoService {
    private static final Logger logger = LogManager.getLogger(IsolationDynamoService.class);

    private final AmazonDynamoDB dynamoDbClient;
    private final String isolationTokenTableName;
    private final Table table;
    private final DynamoDB dynamoDB;

    public IsolationDynamoService(AmazonDynamoDB dynamoDbClient, String isolationTokenTableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.isolationTokenTableName = isolationTokenTableName;
        this.dynamoDB = new DynamoDB(dynamoDbClient);
        this.table = dynamoDB.getTable(isolationTokenTableName);
    }

    public void insertIsolationToken(IsolationToken token) {
        PutItemRequest request = new PutItemRequest()
            .withTableName(isolationTokenTableName)
            .withItem(asAttributes(token));

        dynamoDbClient.putItem(request);
    }

    public void updateIsolationToken(IsolationToken token, TokenStatus currentTokenStatus) {
        PutItemRequest request = new PutItemRequest()
            .withTableName(isolationTokenTableName)
            .withItem(asAttributes(token))
            .withConditionExpression("attribute_exists(tokenId) and tokenStatus=:tokenStatus")
            .withExpressionAttributeValues(Map.of(":tokenStatus", stringAttribute(currentTokenStatus.value)));

        dynamoDbClient.putItem(request);
    }

    private Map<String, AttributeValue> asAttributes(IsolationToken token) {
        return Map.of(
            "tokenId", stringAttribute(token.tokenId),
            "tokenStatus", stringAttribute(token.tokenStatus),
            "riskyEncounterDate", numericNullableAttribute(token.riskyEncounterDate),
            "isolationPeriodEndDate", numericNullableAttribute(token.isolationPeriodEndDate),
            "createdTimestamp", numericAttribute(token.createdTimestamp),
            "updatedTimestamp", numericNullableAttribute(token.updatedTimestamp),
            "validatedTimestamp", numericNullableAttribute(token.validatedTimestamp),
            "consumedTimestamp", numericNullableAttribute(token.consumedTimestamp),
            "expireAt", numericAttribute(token.expireAt)
        );
    }

    public Optional<IsolationToken> getIsolationToken(String ipcToken) {
        GetItemRequest request = new GetItemRequest()
            .withTableName(isolationTokenTableName)
            .withKey(Map.of("tokenId", stringAttribute(ipcToken)));
        GetItemResult itemResult = dynamoDbClient.getItem(request);

        return Optional.ofNullable(itemResult.getItem())
            .map(it -> new IsolationToken(
                itemValueOrThrow(it, "tokenId"),
                itemValueOrThrow(it, "tokenStatus"),
                itemLongValueMaybe(it, "riskyEncounterDate").orElse(null),
                itemLongValueMaybe(it, "isolationPeriodEndDate").orElse(null),
                itemLongValueMaybe(it, "createdTimestamp").orElse(null),
                itemLongValueMaybe(it, "updatedTimestamp").orElse(null),
                itemLongValueMaybe(it, "validatedTimestamp").orElse(null),
                itemLongValueMaybe(it, "consumedTimestamp").orElse(null),
                itemLongValueMaybe(it, "expireAt").orElse(null)
            ));
    }

    public void deleteIsolationToken(String ipcToken, TokenStatus currentTokenStatus) {
        DeleteItemRequest request = new DeleteItemRequest()
            .withTableName(isolationTokenTableName)
            .withKey(Map.of("tokenId", stringAttribute(ipcToken)))
            .withConditionExpression("attribute_exists(tokenId) and tokenStatus=:tokenStatus")
            .withExpressionAttributeValues(Map.of(":tokenStatus", stringAttribute(currentTokenStatus.value)));

        dynamoDbClient.deleteItem(request);
    }
}
