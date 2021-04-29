package uk.nhs.nhsx.isolationpayment

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.amazonaws.services.dynamodbv2.document.Table
import com.amazonaws.services.dynamodbv2.local.embedded.DynamoDBEmbedded
import com.amazonaws.services.dynamodbv2.local.shared.access.AmazonDynamoDBLocal
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement
import com.amazonaws.services.dynamodbv2.model.KeyType
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput
import com.amazonaws.services.dynamodbv2.model.PutItemRequest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.aws.dynamodb.DynamoAttributes.numericAttribute
import uk.nhs.nhsx.core.aws.dynamodb.DynamoAttributes.numericNullableAttribute
import uk.nhs.nhsx.core.aws.dynamodb.DynamoAttributes.stringAttribute
import uk.nhs.nhsx.isolationpayment.model.IsolationToken
import uk.nhs.nhsx.isolationpayment.model.TokenStateInternal
import uk.nhs.nhsx.domain.IpcTokenId
import java.time.Instant
import java.time.Period
import java.util.ArrayList

class IsolationPersistenceLocalTest {
    private val tableName = "isolation_tokens_payment_table"
    private val clock = { Instant.parse("2020-12-01T00:00:00Z") }

    private lateinit var persistence: IsolationPaymentPersistence
    private lateinit var isolationTokenTable: Table

    companion object {
        private lateinit var dbLocal: AmazonDynamoDBLocal
        private lateinit var dbClient: AmazonDynamoDB
        private lateinit var dynamoDB: DynamoDB

        @JvmStatic
        @BeforeAll
        fun setupLocalDynamo() {
            dbLocal = DynamoDBEmbedded.create()
            dbClient = dbLocal.amazonDynamoDB()
            dynamoDB = DynamoDB(dbClient)
        }

        @JvmStatic
        @AfterAll
        fun destroyLocalDynamo() {
            dbLocal.shutdownNow()
        }
    }

    @BeforeEach
    fun setup() {
        val attributeDefinitions: MutableList<AttributeDefinition> = ArrayList()
        attributeDefinitions.add(AttributeDefinition().withAttributeName("tokenId").withAttributeType("S"))

        val keySchema: MutableList<KeySchemaElement> = ArrayList()
        keySchema.add(KeySchemaElement().withAttributeName("tokenId").withKeyType(KeyType.HASH))

        val request = CreateTableRequest()
            .withTableName(tableName)
            .withKeySchema(keySchema)
            .withAttributeDefinitions(attributeDefinitions)
            .withProvisionedThroughput(ProvisionedThroughput(100L, 100L))
        dbClient.createTable(request)

        persistence = IsolationPaymentPersistence(dbClient, tableName)
        dynamoDB = DynamoDB(dbClient)
        isolationTokenTable = dynamoDB.getTable(tableName)
    }

    @AfterEach
    fun destroy() {
        dbClient.deleteTable(tableName)
    }

    @Test
    fun `gets isolation payment token`() {
        val token = getIsolationToken()

        val itemMap = itemMapFrom(token)

        val request = PutItemRequest()
            .withTableName(tableName)
            .withItem(itemMap)

        dbClient.putItem(request)

        persistence
            .getIsolationToken(token.tokenId)
            .map {
                assertThat(it.tokenId).isEqualTo(token.tokenId)
                assertThat(it.tokenStatus).isEqualTo(TokenStateInternal.INT_CREATED.value)
                assertThat(it.riskyEncounterDate).isEqualTo(token.riskyEncounterDate)
                assertThat(it.isolationPeriodEndDate).isEqualTo(token.isolationPeriodEndDate)
                assertThat(it.createdTimestamp).isEqualTo(token.createdTimestamp)
                assertThat(it.updatedTimestamp).isEqualTo(token.updatedTimestamp)
                assertThat(it.validatedTimestamp).isEqualTo(token.validatedTimestamp)
                assertThat(it.consumedTimestamp).isEqualTo(token.consumedTimestamp)
                assertThat(it.expireAt).isEqualTo(token.expireAt)
            }
            .orElseThrow { RuntimeException("Token not found") }
    }

    @Test
    fun `creates isolation payment token`() {
        val token = getIsolationToken()

        persistence.insertIsolationToken(token)

        val item = isolationTokenTable.getItem("tokenId", token.tokenId.value)

        assertThat(item.getString("tokenStatus")).isEqualTo(TokenStateInternal.INT_CREATED.value)
        assertThat(item.getLong("expireAt")).isEqualTo(token.expireAt)
        assertThat(item.getLong("createdTimestamp")).isNotNull
    }

    @Test
    fun `updates isolation payment token`() {
        val token = getIsolationToken()
        persistence.insertIsolationToken(token)

        val updated = token.copy(tokenStatus = TokenStateInternal.INT_UPDATED.value)
        persistence.updateIsolationToken(updated, TokenStateInternal.INT_CREATED)

        persistence
            .getIsolationToken(updated.tokenId)
            .map {
                assertThat(it.tokenId).isEqualTo(updated.tokenId)
                assertThat(it.tokenStatus).isEqualTo(TokenStateInternal.INT_UPDATED.value)
                assertThat(it.riskyEncounterDate).isEqualTo(updated.riskyEncounterDate)
                assertThat(it.isolationPeriodEndDate).isEqualTo(updated.isolationPeriodEndDate)
                assertThat(it.createdTimestamp).isEqualTo(updated.createdTimestamp)
                assertThat(it.updatedTimestamp).isEqualTo(updated.updatedTimestamp)
                assertThat(it.validatedTimestamp).isEqualTo(updated.validatedTimestamp)
                assertThat(it.consumedTimestamp).isEqualTo(updated.consumedTimestamp)
                assertThat(it.expireAt).isEqualTo(updated.expireAt)
            }
            .orElseThrow { RuntimeException("Token not found") }
    }

    @Test
    fun `update throws when token id condition fails`() {
        val token = getIsolationToken().also {
            persistence.insertIsolationToken(it)
        }

        val updated = token.copy(
            tokenStatus = TokenStateInternal.INT_UPDATED.value,
            tokenId = IpcTokenId.of("2".repeat(64))
        )

        assertThatThrownBy {
            persistence.updateIsolationToken(updated, TokenStateInternal.INT_CREATED) // wrong token id
        }.isInstanceOf(ConditionalCheckFailedException::class.java)
    }

    @Test
    fun `update throws when status condition fails`() {
        val token = getIsolationToken()
        persistence.insertIsolationToken(token)

        val updated = token.copy(
            tokenStatus = TokenStateInternal.INT_UPDATED.value,
        )

        assertThatThrownBy {
            persistence.updateIsolationToken(updated, TokenStateInternal.INT_UPDATED) // wrong current status
        }.isInstanceOf(ConditionalCheckFailedException::class.java)
    }

    @Test
    fun `deletes isolation payment token`() {
        val token = getIsolationToken()
        persistence.insertIsolationToken(token)

        persistence.deleteIsolationToken(token.tokenId, TokenStateInternal.INT_CREATED)

        assertThat(persistence.getIsolationToken(token.tokenId)).isEmpty
    }

    @Test
    fun `delete throws when token id condition fails`() {
        getIsolationToken().also {
            persistence.insertIsolationToken(it)
        }

        assertThatThrownBy {
            persistence.deleteIsolationToken(
                IpcTokenId.of("2".repeat(64)),
                TokenStateInternal.INT_CREATED
            ) // wrong token id
        }.isInstanceOf(ConditionalCheckFailedException::class.java)
    }

    @Test
    fun `delete throws when status condition fails`() {
        val token = getIsolationToken()
        persistence.insertIsolationToken(token)

        assertThatThrownBy {
            persistence.deleteIsolationToken(token.tokenId, TokenStateInternal.INT_UPDATED) // wrong current status
        }.isInstanceOf(ConditionalCheckFailedException::class.java)
    }

    private fun getIsolationToken(): IsolationToken {
        val tokenId = IpcTokenId.of("1".repeat(64))
        val createdDate = clock().epochSecond
        val ttl = clock().plus(Period.ofWeeks(4)).epochSecond
        return IsolationToken(tokenId, TokenStateInternal.INT_CREATED.value, 0, 0, createdDate, 0, 0, 0, ttl)
    }

    private fun itemMapFrom(token: IsolationToken): Map<String, AttributeValue> =
        mapOf(
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
}
