package uk.nhs.nhsx.isolationpayment;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.*;
import uk.nhs.nhsx.isolationpayment.model.IsolationToken;
import uk.nhs.nhsx.isolationpayment.model.TokenStateInternal;

import java.util.Map;
import java.util.Optional;

import static uk.nhs.nhsx.core.aws.dynamodb.DynamoAttributes.*;

public class IsolationPaymentPersistence {

    private final AmazonDynamoDB dynamoDbClient;
    private final String tableName;

    public IsolationPaymentPersistence(AmazonDynamoDB dynamoDbClient, String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
    }

    public void insertIsolationToken(IsolationToken token) {
        var request = new PutItemRequest()
            .withTableName(tableName)
            .withItem(asAttributes(token));

        dynamoDbClient.putItem(request);
    }

    public void updateIsolationToken(IsolationToken token, TokenStateInternal currentTokenStatus) {
        var request = new PutItemRequest()
            .withTableName(tableName)
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
        var request = new GetItemRequest()
            .withTableName(tableName)
            .withKey(Map.of("tokenId", stringAttribute(ipcToken)));
        
        var itemResult = dynamoDbClient.getItem(request);

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

    public void deleteIsolationToken(String ipcToken, TokenStateInternal currentTokenStatus) {
        var request = new DeleteItemRequest()
            .withTableName(tableName)
            .withKey(Map.of("tokenId", stringAttribute(ipcToken)))
            .withConditionExpression("attribute_exists(tokenId) and tokenStatus=:tokenStatus")
            .withExpressionAttributeValues(Map.of(":tokenStatus", stringAttribute(currentTokenStatus.value)));

        dynamoDbClient.deleteItem(request);
    }
}
