@file:Suppress("unused")

package uk.nhs.nhsx.isolationpayment

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.amazonaws.services.dynamodbv2.document.Table
import com.amazonaws.services.dynamodbv2.local.embedded.DynamoDBEmbedded
import com.amazonaws.services.dynamodbv2.local.shared.access.AmazonDynamoDBLocal
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement
import com.amazonaws.services.dynamodbv2.model.KeyType.HASH
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput
import com.amazonaws.services.dynamodbv2.model.PutItemRequest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThan
import strikt.assertions.isNull
import uk.nhs.nhsx.core.aws.dynamodb.DynamoAttributes.numericAttribute
import uk.nhs.nhsx.core.aws.dynamodb.DynamoAttributes.numericNullableAttribute
import uk.nhs.nhsx.core.aws.dynamodb.DynamoAttributes.stringAttribute
import uk.nhs.nhsx.core.aws.dynamodb.TableName
import uk.nhs.nhsx.core.aws.dynamodb.withTableName
import uk.nhs.nhsx.domain.IpcTokenId
import uk.nhs.nhsx.isolationpayment.model.IsolationToken
import uk.nhs.nhsx.isolationpayment.model.TokenStateInternal.INT_CREATED
import uk.nhs.nhsx.isolationpayment.model.TokenStateInternal.INT_UPDATED
import java.time.Instant
import java.time.Period

class IsolationPersistenceLocalTest {
    private val tableName = TableName.of("isolation_tokens_payment_table")
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
        val attributeDefinitions = listOf(
            AttributeDefinition()
                .withAttributeName("tokenId")
                .withAttributeType("S")
        )

        val keySchema = listOf(
            KeySchemaElement()
                .withAttributeName("tokenId")
                .withKeyType(HASH)
        )

        val request = CreateTableRequest()
            .withTableName(tableName)
            .withKeySchema(keySchema)
            .withAttributeDefinitions(attributeDefinitions)
            .withProvisionedThroughput(ProvisionedThroughput(100L, 100L))

        dbClient.createTable(request)

        persistence = IsolationPaymentPersistence(dbClient, tableName)
        dynamoDB = DynamoDB(dbClient)
        isolationTokenTable = dynamoDB.getTable(tableName.value)
    }

    @AfterEach
    fun destroy() {
        dbClient.deleteTable(tableName.value)
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

        val item = isolationTokenTable.getItem("tokenId", token.tokenId.value)

        expectThat(item) {
            get { getString("tokenStatus") }.isEqualTo(INT_CREATED.value)
            get { getLong("expireAt") }.isEqualTo(token.expireAt)
            get { getLong("createdTimestamp") }.isGreaterThan(0)
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
}
