package integration.dynamo

import com.amazonaws.services.dynamodbv2.document.Item
import com.amazonaws.services.dynamodbv2.document.ItemUtils
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException
import com.amazonaws.services.dynamodbv2.model.DeleteItemRequest
import com.amazonaws.services.dynamodbv2.model.GetItemRequest
import com.amazonaws.services.dynamodbv2.model.GetItemResult
import com.amazonaws.services.dynamodbv2.model.PutItemRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThan
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import uk.nhs.nhsx.core.aws.dynamodb.DynamoAttributes.attributeMap
import uk.nhs.nhsx.core.aws.dynamodb.DynamoAttributes.numericAttribute
import uk.nhs.nhsx.core.aws.dynamodb.DynamoAttributes.numericNullableAttribute
import uk.nhs.nhsx.core.aws.dynamodb.DynamoAttributes.stringAttribute
import uk.nhs.nhsx.core.aws.dynamodb.TableName
import uk.nhs.nhsx.core.aws.dynamodb.withTableName
import uk.nhs.nhsx.domain.IpcTokenId
import uk.nhs.nhsx.isolationpayment.IsolationPaymentPersistence
import uk.nhs.nhsx.isolationpayment.model.IsolationToken
import uk.nhs.nhsx.isolationpayment.model.TokenStateInternal.INT_CREATED
import uk.nhs.nhsx.isolationpayment.model.TokenStateInternal.INT_UPDATED
import java.time.Instant
import java.time.Period

class IsolationPersistenceLocalTest : DynamoIntegrationTest() {

    private val tableName = TableName.of("${tgtEnv}-isolation-payment-tokens")
    private val persistence = IsolationPaymentPersistence(dbClient, tableName)

    @BeforeEach
    fun deleteItems() {
        dbClient.deleteItem(
            DeleteItemRequest()
                .withTableName(tableName.value)
                .withKey(attributeMap("tokenId", getIsolationToken().tokenId))
        )
    }

    @Test
    fun `gets isolation payment token`() {
        val token = getIsolationToken()

        dbClient.putItem(
            PutItemRequest()
                .withTableName(tableName)
                .withItem(itemMapFrom(token))
        )

        val response = persistence.getIsolationToken(token.tokenId)

        expectThat(response).isEqualTo(token)
    }

    @Test
    fun `creates isolation payment token`() {
        val token = getIsolationToken().also {
            persistence.insertIsolationToken(it)
        }

        val item = dbClient.getItem(
            GetItemRequest()
                .withTableName(tableName)
                .withKey(attributeMap("tokenId", token.tokenId.value))
        ).toItemMaybe()

        expectThat(item) {
            get { this?.getString("tokenStatus") }.isEqualTo(INT_CREATED.value)
            get { this?.getLong("expireAt") }.isEqualTo(token.expireAt)
            get { this?.getLong("createdTimestamp") }.isNotNull().isGreaterThan(0)
        }
    }

    @Test
    fun `updates isolation payment token`() {
        val token = getIsolationToken().also {
            persistence.insertIsolationToken(it)
        }

        val updatedToken = token.copy(tokenStatus = INT_UPDATED.value).also {
            persistence.updateIsolationToken(it, INT_CREATED)
        }

        val isolationToken = persistence.getIsolationToken(updatedToken.tokenId)

        expectThat(isolationToken).isEqualTo(updatedToken)
    }

    @Test
    fun `update throws when token id condition fails`() {
        val token = getIsolationToken().also {
            persistence.insertIsolationToken(it)
        }

        val updated = token.copy(
            tokenStatus = INT_UPDATED.value,
            tokenId = IpcTokenId.of("2".repeat(64))
        )

        expectThrows<ConditionalCheckFailedException> {
            // wrong token id
            persistence.updateIsolationToken(updated, INT_CREATED)
        }
    }

    @Test
    fun `update throws when status condition fails`() {
        val token = getIsolationToken().also {
            persistence.insertIsolationToken(it)
        }

        val updated = token.copy(tokenStatus = INT_UPDATED.value)

        expectThrows<ConditionalCheckFailedException> {
            // wrong current status
            persistence.updateIsolationToken(updated, INT_UPDATED)
        }
    }

    @Test
    fun `deletes isolation payment token`() {
        val token = getIsolationToken().also {
            persistence.insertIsolationToken(it)
            persistence.deleteIsolationToken(it.tokenId, INT_CREATED)
        }

        expectThat(persistence.getIsolationToken(token.tokenId)).isNull()
    }

    @Test
    fun `delete throws when token id condition fails`() {
        getIsolationToken().also {
            persistence.insertIsolationToken(it)
        }

        expectThrows<ConditionalCheckFailedException> {
            // wrong token id
            persistence.deleteIsolationToken(IpcTokenId.of("2".repeat(64)), INT_CREATED)
        }
    }

    @Test
    fun `delete throws when status condition fails`() {
        val token = getIsolationToken().also {
            persistence.insertIsolationToken(it)
        }

        expectThrows<ConditionalCheckFailedException> {
            // wrong current status
            persistence.deleteIsolationToken(token.tokenId, INT_UPDATED)
        }
    }

    private fun getIsolationToken(): IsolationToken {
        val clock = { Instant.parse("2020-12-01T00:00:00Z") }
        val tokenId = IpcTokenId.of("1".repeat(64))
        val createdDate = clock().epochSecond
        val ttl = clock().plus(Period.ofWeeks(4)).epochSecond
        return IsolationToken(
            tokenId = tokenId,
            tokenStatus = INT_CREATED.value,
            riskyEncounterDate = 0,
            isolationPeriodEndDate = 0,
            createdTimestamp = createdDate,
            updatedTimestamp = 0,
            validatedTimestamp = 0,
            consumedTimestamp = 0,
            expireAt = ttl
        )
    }

    private fun itemMapFrom(token: IsolationToken) = mapOf(
        "tokenId" to stringAttribute(token.tokenId.value),
        "tokenStatus" to stringAttribute(token.tokenStatus),
        "riskyEncounterDate" to numericNullableAttribute(token.riskyEncounterDate),
        "isolationPeriodEndDate" to numericNullableAttribute(token.isolationPeriodEndDate),
        "createdTimestamp" to numericAttribute(token.createdTimestamp),
        "updatedTimestamp" to numericNullableAttribute(token.updatedTimestamp),
        "validatedTimestamp" to numericNullableAttribute(token.validatedTimestamp),
        "consumedTimestamp" to numericNullableAttribute(token.consumedTimestamp),
        "expireAt" to numericAttribute(token.expireAt)
    )

    private fun GetItemResult.toItemMaybe(): Item? =
        Item.fromMap(ItemUtils.toSimpleMapValue(item))
}
