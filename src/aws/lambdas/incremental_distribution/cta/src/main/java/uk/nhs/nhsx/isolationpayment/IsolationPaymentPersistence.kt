package uk.nhs.nhsx.isolationpayment

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.DeleteItemRequest
import com.amazonaws.services.dynamodbv2.model.GetItemRequest
import com.amazonaws.services.dynamodbv2.model.PutItemRequest
import uk.nhs.nhsx.core.aws.dynamodb.DynamoAttributes.itemLongValueOrNull
import uk.nhs.nhsx.core.aws.dynamodb.DynamoAttributes.itemLongValueOrThrow
import uk.nhs.nhsx.core.aws.dynamodb.DynamoAttributes.itemValueOrThrow
import uk.nhs.nhsx.core.aws.dynamodb.DynamoAttributes.numericAttribute
import uk.nhs.nhsx.core.aws.dynamodb.DynamoAttributes.numericNullableAttribute
import uk.nhs.nhsx.core.aws.dynamodb.DynamoAttributes.stringAttribute
import uk.nhs.nhsx.isolationpayment.model.IsolationToken
import uk.nhs.nhsx.isolationpayment.model.TokenStateInternal
import uk.nhs.nhsx.domain.IpcTokenId
import java.util.Optional

class IsolationPaymentPersistence(private val dynamoDbClient: AmazonDynamoDB, private val tableName: String) {
    fun insertIsolationToken(token: IsolationToken) {
        dynamoDbClient.putItem(
            PutItemRequest()
                .withTableName(tableName)
                .withItem(asAttributes(token))
        )
    }

    fun updateIsolationToken(token: IsolationToken, currentTokenStatus: TokenStateInternal) {
        dynamoDbClient.putItem(
            PutItemRequest()
                .withTableName(tableName)
                .withItem(asAttributes(token))
                .withConditionExpression("attribute_exists(tokenId) and tokenStatus=:tokenStatus")
                .withExpressionAttributeValues(
                    mapOf(
                        ":tokenStatus" to stringAttribute(currentTokenStatus.value)
                    )
                )
        )
    }

    private fun asAttributes(token: IsolationToken): Map<String, AttributeValue> = mapOf(
        "tokenId" to stringAttribute(token.tokenId),
        "tokenStatus" to stringAttribute(token.tokenStatus),
        "riskyEncounterDate" to numericNullableAttribute(token.riskyEncounterDate),
        "isolationPeriodEndDate" to numericNullableAttribute(token.isolationPeriodEndDate),
        "createdTimestamp" to numericAttribute(token.createdTimestamp),
        "updatedTimestamp" to numericNullableAttribute(token.updatedTimestamp),
        "validatedTimestamp" to numericNullableAttribute(token.validatedTimestamp),
        "consumedTimestamp" to numericNullableAttribute(token.consumedTimestamp),
        "expireAt" to numericAttribute(token.expireAt)
    )

    fun getIsolationToken(ipcToken: IpcTokenId): Optional<IsolationToken> {
        val request = GetItemRequest()
            .withTableName(tableName)
            .withKey(mapOf("tokenId" to stringAttribute(ipcToken)))
        val itemResult = dynamoDbClient.getItem(request)

        return Optional.ofNullable(itemResult.item?.let {
            IsolationToken(
                IpcTokenId.of(itemValueOrThrow(it, "tokenId")),
                itemValueOrThrow(it, "tokenStatus"),
                itemLongValueOrNull(it, "riskyEncounterDate"),
                itemLongValueOrNull(it, "isolationPeriodEndDate"),
                itemLongValueOrThrow(it, "createdTimestamp"),
                itemLongValueOrNull(it, "updatedTimestamp"),
                itemLongValueOrNull(it, "validatedTimestamp"),
                itemLongValueOrNull(it, "consumedTimestamp"),
                itemLongValueOrThrow(it, "expireAt")
            )
        })
    }

    fun deleteIsolationToken(ipcToken: IpcTokenId, currentTokenStatus: TokenStateInternal) {
        dynamoDbClient.deleteItem(
            DeleteItemRequest()
                .withTableName(tableName)
                .withKey(java.util.Map.of("tokenId", stringAttribute(ipcToken)))
                .withConditionExpression("attribute_exists(tokenId) and tokenStatus=:tokenStatus")
                .withExpressionAttributeValues(mapOf(":tokenStatus" to stringAttribute(currentTokenStatus.value)))
        )
    }
}
