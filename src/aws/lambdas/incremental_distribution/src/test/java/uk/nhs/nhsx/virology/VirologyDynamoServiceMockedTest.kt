package uk.nhs.nhsx.virology

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import uk.nhs.nhsx.core.aws.dynamodb.DynamoAttributes
import uk.nhs.nhsx.core.aws.dynamodb.DynamoAttributes.numericAttribute
import uk.nhs.nhsx.core.aws.dynamodb.DynamoTransactions
import uk.nhs.nhsx.virology.order.TokensGenerator
import uk.nhs.nhsx.virology.persistence.*
import uk.nhs.nhsx.virology.result.VirologyResultRequest
import java.util.*

class VirologyDynamoServiceMockedTest {

    private val dynamoDbClient = mockk<AmazonDynamoDB>()

    private val virologyConfig = VirologyConfig(
        "testOrdersTableName",
        "testResultsTableName",
        "submissionTokensTableName",
        "testOrdersIndex",
        VirologyConfig.MAX_TOKEN_PERSISTENCE_RETRY_COUNT
    )

    private val service = VirologyDynamoService(dynamoDbClient, virologyConfig)

    private val itemResult = mockk<GetItemResult>()

    @Test
    fun `test result with no data returns empty`() {
        every { itemResult.item } returns null
        every { dynamoDbClient.getItem(any()) } returns itemResult
        assertThat(service.getTestResult(TestResultPollingToken.of("token"))).isEmpty
    }

    @Test
    fun `test result with empty data throws exception`() {
        every { itemResult.item } returns emptyMap()
        every { dynamoDbClient.getItem(any()) } returns itemResult
        assertThatThrownBy { service.getTestResult(TestResultPollingToken.of("token")) }
            .isInstanceOf(RuntimeException::class.java)
            .hasMessage("Required field missing")
    }

    @Test
    fun `test result with missing field throws exception`() {
        val pollingTokenValue = mockk<AttributeValue> {
            every { s } returns null
        }
        val statusValue = mockk<AttributeValue> {
            every { s } returns "status"
        }
        val valueMap: Map<String, AttributeValue> = mapOf(
            "testResultPollingToken" to pollingTokenValue,
            "status" to statusValue
        )
        every { itemResult.item } returns valueMap
        every { dynamoDbClient.getItem(any()) } returns itemResult
        assertThatThrownBy { service.getTestResult(TestResultPollingToken.of("token")) }
            .isInstanceOf(RuntimeException::class.java)
            .hasMessage("Required field missing")
    }

    @Test
    fun `test result with missing status value throws exception`() {
        val pollingTokenValue = mockk<AttributeValue> {
            every { s } returns "token"
        }
        val statusValue = mockk<AttributeValue> {
            every { s } returns null
        }
        val valueMap: Map<String, AttributeValue> = mapOf(
            "testResultPollingToken" to pollingTokenValue,
            "status" to statusValue
        )
        every { itemResult.item } returns valueMap
        every { dynamoDbClient.getItem(any()) } returns itemResult

        assertThatThrownBy { service.getTestResult(TestResultPollingToken.of("token")) }
            .isInstanceOf(RuntimeException::class.java)
            .hasMessage("Required field missing")
    }

    @Test
    fun `test order given cta token`() {
        val ctaToken = "cc8f0b6z"
        val testResultToken = "poll-abcd"
        val subToken = "sub-zyxw"
        val itemResult = GetItemResult()
            .withItem(
                java.util.Map.of(
                    "testResultPollingToken", DynamoAttributes.stringAttribute(testResultToken),
                    "diagnosisKeySubmissionToken", DynamoAttributes.stringAttribute(subToken),
                    "ctaToken", DynamoAttributes.stringAttribute(ctaToken)
                )
            )
        every { dynamoDbClient.getItem(any()) } returns itemResult

        val testOrderTokens = service.getTestOrder(CtaToken.of(ctaToken))

        assertThat(testOrderTokens).isNotEmpty
        assertThat(testOrderTokens.get().ctaToken).isEqualTo(CtaToken.of(ctaToken))
        assertThat(testOrderTokens.get().testResultPollingToken).isEqualTo(TestResultPollingToken.of(testResultToken))
        assertThat(testOrderTokens.get().diagnosisKeySubmissionToken).isEqualTo(DiagnosisKeySubmissionToken.of(subToken))

        verify(exactly = 1) {
            dynamoDbClient.getItem(
                GetItemRequest(
                    "testOrdersTableName",
                    DynamoAttributes.attributeMap("ctaToken", ctaToken)
                )
            )
        }
    }

    @Test
    fun `marks test data for deletion for positive test result`() {
        val testOrder = TokensGenerator().generateVirologyTokens()
        val queryResultItems = queryResult(testOrder)
        val queryResult = mockk<QueryResult> {
            every { items } returns queryResultItems
        }
        every { dynamoDbClient.query(any()) } returns queryResult

        val slot = slot<TransactWriteItemsRequest>()
        every { dynamoDbClient.transactWriteItems(capture(slot)) } returns mockk()

        val testResult = TestResult("some-token", "", "POSITIVE", "available")

        service.markForDeletion(testResult, VirologyDataTimeToLive(1, 10))

        verify(exactly = 1) {
            dynamoDbClient.transactWriteItems(any())
        }

        val transactItems = slot.captured.transactItems
        assertThat(transactItems).hasSize(3)

        val testOrderUpdate = transactItems[0].update
        assertThat(testOrderUpdate.tableName).isEqualTo("testOrdersTableName")
        assertThat(testOrderUpdate.expressionAttributeValues).isEqualTo(expireAtWithTtl(1))

        val testResultUpdate = transactItems[1].update
        assertThat(testResultUpdate.tableName).isEqualTo("testResultsTableName")
        assertThat(testResultUpdate.expressionAttributeValues).isEqualTo(expireAtWithTtl(1))

        val submissionUpdate = transactItems[2].update
        assertThat(submissionUpdate.tableName).isEqualTo("submissionTokensTableName")
        assertThat(submissionUpdate.expressionAttributeValues).isEqualTo(expireAtWithTtl(10))
    }

    @Test
    fun `marks test data for deletion for non positive test result`() {
        val testOrder = TokensGenerator().generateVirologyTokens()
        val queryResultItems = queryResult(testOrder)
        val queryResult = mockk<QueryResult> {
            every { items } returns queryResultItems
        }
        every { dynamoDbClient.query(any()) } returns queryResult

        val slot = slot<TransactWriteItemsRequest>()
        every { dynamoDbClient.transactWriteItems(capture(slot)) } returns mockk()

        val testResult = TestResult("some-token", "", "NEGATIVE", "available")

        service.markForDeletion(testResult, VirologyDataTimeToLive(1, 10))

        verify(exactly = 1) {
            dynamoDbClient.transactWriteItems(any())
        }

        val transactItems = slot.captured.transactItems
        assertThat(transactItems).hasSize(2)

        val testOrderUpdate = transactItems[0].update
        assertThat(testOrderUpdate.tableName).isEqualTo("testOrdersTableName")
        assertThat(testOrderUpdate.expressionAttributeValues).isEqualTo(expireAtWithTtl(1))

        val testResultUpdate = transactItems[1].update
        assertThat(testResultUpdate.tableName).isEqualTo("testResultsTableName")
        assertThat(testResultUpdate.expressionAttributeValues).isEqualTo(expireAtWithTtl(1))
    }

    @Test
    fun `cta exchange updates ttl and counter for positive test result`() {
        val testOrder = TokensGenerator().generateVirologyTokens()
        val queryResultItems = queryResult(testOrder)
        val queryResult = mockk<QueryResult> {
            every { items } returns queryResultItems
        }
        every { dynamoDbClient.query(any()) } returns queryResult

        val slot = slot<TransactWriteItemsRequest>()
        every { dynamoDbClient.transactWriteItems(capture(slot)) } returns mockk()

        val testResult = TestResult("some-token", "", "POSITIVE", "available")

        service.updateOnCtaExchange(testOrder, testResult, VirologyDataTimeToLive(1, 10))

        verify(exactly = 1) {
            dynamoDbClient.transactWriteItems(any())
        }

        val transactItems = slot.captured.transactItems
        assertThat(transactItems).hasSize(3)

        val testOrderUpdate = transactItems[0].update
        assertThat(testOrderUpdate.tableName).isEqualTo("testOrdersTableName")
        assertThat(testOrderUpdate.expressionAttributeValues)
            .isEqualTo(expireAtWithTtl(1).apply { put(":dc", numericAttribute(1)) })

        val testResultUpdate = transactItems[1].update
        assertThat(testResultUpdate.tableName).isEqualTo("testResultsTableName")
        assertThat(testResultUpdate.expressionAttributeValues).isEqualTo(expireAtWithTtl(1))

        val submissionUpdate = transactItems[2].update
        assertThat(submissionUpdate.tableName).isEqualTo("submissionTokensTableName")
        assertThat(submissionUpdate.expressionAttributeValues).isEqualTo(expireAtWithTtl(10))
    }

    @Test
    fun `cta exchange updates ttl and counter for non-positive test result`() {
        val testOrder = TokensGenerator().generateVirologyTokens()
        val queryResultItems = queryResult(testOrder)
        val queryResult = mockk<QueryResult> {
            every { items } returns queryResultItems
        }
        every { dynamoDbClient.query(any()) } returns queryResult

        val slot = slot<TransactWriteItemsRequest>()
        every { dynamoDbClient.transactWriteItems(capture(slot)) } returns mockk()

        val testResult = TestResult("some-token", "", "NEGATIVE", "available")

        service.updateOnCtaExchange(testOrder, testResult, VirologyDataTimeToLive(1, 10))

        verify(exactly = 1) {
            dynamoDbClient.transactWriteItems(any())
        }

        val transactItems = slot.captured.transactItems
        assertThat(transactItems).hasSize(2)

        val testOrderUpdate = transactItems[0].update
        assertThat(testOrderUpdate.tableName).isEqualTo("testOrdersTableName")
        assertThat(testOrderUpdate.expressionAttributeValues)
            .isEqualTo(expireAtWithTtl(1).apply { put(":dc", numericAttribute(1)) })

        val testResultUpdate = transactItems[1].update
        assertThat(testResultUpdate.tableName).isEqualTo("testResultsTableName")
        assertThat(testResultUpdate.expressionAttributeValues).isEqualTo(expireAtWithTtl(1))
    }

    @Test
    fun `persists token gen positive test result`() {
        val slot = slot<TransactWriteItemsRequest>()
        every { dynamoDbClient.transactWriteItems(capture(slot)) } returns mockk()

        service.persistTestOrderAndResult(
            { TokensGenerator().generateVirologyTokens() },
            1_000_000,
            "POSITIVE",
            "2020-08-07T00:00:00Z"
        )

        verify(exactly = 1) {
            dynamoDbClient.transactWriteItems(any())
        }

        val transactItems = slot.captured.transactItems
        assertThat(transactItems).hasSize(3)

        val testOrderUpdate = transactItems[0].put
        assertThat(testOrderUpdate.tableName).isEqualTo("testOrdersTableName")

        val testResultUpdate = transactItems[1].put
        assertThat(testResultUpdate.tableName).isEqualTo("testResultsTableName")

        val submissionUpdate = transactItems[2].put
        assertThat(submissionUpdate.tableName).isEqualTo("submissionTokensTableName")
    }

    @Test
    fun `persists token gen negative test result`() {
        val slot = slot<TransactWriteItemsRequest>()
        every { dynamoDbClient.transactWriteItems(capture(slot)) } returns mockk()

        service.persistTestOrderAndResult(
            { TokensGenerator().generateVirologyTokens() },
            1_000_000,
            "NEGATIVE",
            "2020-08-07T00:00:00Z"
        )

        verify(exactly = 1) {
            dynamoDbClient.transactWriteItems(any())
        }

        val transactItems = slot.captured.transactItems
        assertThat(transactItems).hasSize(2)

        val testOrderUpdate = transactItems[0].put
        assertThat(testOrderUpdate.tableName).isEqualTo("testOrdersTableName")

        val testResultUpdate = transactItems[1].put
        assertThat(testResultUpdate.tableName).isEqualTo("testResultsTableName")
    }

    @Test
    fun `persist transaction items exception causes 500`() {
        val reason = CancellationReason()
        reason.message = "reason"
        val reasons = listOf(reason)
        val exception = TransactionCanceledException("message").withCancellationReasons(reasons)

        every { dynamoDbClient.transactWriteItems(any()) } throws exception
        assertThatThrownBy { DynamoTransactions.executeTransaction(dynamoDbClient, emptyList()) }
            .isInstanceOf(RuntimeException::class.java)
            .hasMessage("Transaction cancelled by remote DB service due to: reason")
    }

    @Test
    fun `order returning null item causes 400`() {
        val itemResult = mockk<GetItemResult>()
        every { dynamoDbClient.getItem(any(), any()) } returns itemResult
        every { itemResult.item } returns null
        val testResult = VirologyResultRequest("cc8f0b6z", "2020-04-23T18:34:03Z", "POSITIVE")
        val result = service.persistNonPositiveTestResult(VirologyResultRequest.NonPositive.from(testResult))

        assertThat(result).isInstanceOf(VirologyResultPersistOperation.OrderNotFound::class.java)
        verify(exactly = 0) { dynamoDbClient.transactWriteItems(any()) }
    }

    @Test
    fun `order Returning Null Token Value Causes 400`() {
        val itemResult = mockk<GetItemResult>()
        every { itemResult.item } returns emptyMap()
        every { dynamoDbClient.getItem(any(), any()) } returns itemResult

        val testResult = VirologyResultRequest("cc8f0b6z", "2020-04-23T18:34:03Z", "POSITIVE")

        assertThatThrownBy { service.persistNonPositiveTestResult(VirologyResultRequest.NonPositive.from(testResult)) }
            .isInstanceOf(RuntimeException::class.java)
            .hasMessage("Required field missing")
        verify(exactly = 0) { dynamoDbClient.transactWriteItems(any()) }
    }

    @Test
    fun `order returning null token string causes 400`() {
        val itemResult = mockk<GetItemResult>()
        val pollingTokenValue = mockk<AttributeValue> {
            every { s } returns null
        }
        val submissionTokenValue = mockk<AttributeValue> {
            every { s } returns null
        }

        val map = mapOf(
            "testResultPollingToken" to pollingTokenValue,
            "diagnosisKeySubmissionToken" to submissionTokenValue
        )
        every { itemResult.item } returns map
        every { dynamoDbClient.getItem(any(), any()) } returns itemResult

        val testResult = VirologyResultRequest("cc8f0b6z", "2020-04-23T18:34:03Z", "POSITIVE")

        assertThatThrownBy { service.persistNonPositiveTestResult(VirologyResultRequest.NonPositive.from(testResult)) }
            .isInstanceOf(RuntimeException::class.java)
            .hasMessage("Required field missing")
        verify(exactly = 0) { dynamoDbClient.transactWriteItems(any()) }
    }

    private fun queryResult(testOrder: TestOrder): List<Map<String, AttributeValue>> {
        val item: MutableMap<String, AttributeValue> = LinkedHashMap()
        item["ctaToken"] = DynamoAttributes.stringAttribute(testOrder.ctaToken.value)
        item["diagnosisKeySubmissionToken"] = DynamoAttributes.stringAttribute(testOrder.diagnosisKeySubmissionToken.value)
        item["testResultPollingToken"] = DynamoAttributes.stringAttribute(testOrder.testResultPollingToken.value)
        return listOf(item)
    }

    private fun expireAtWithTtl(s: Number): MutableMap<String, AttributeValue> =
        mutableMapOf(
            ":expireAt" to numericAttribute(s)
        )
}