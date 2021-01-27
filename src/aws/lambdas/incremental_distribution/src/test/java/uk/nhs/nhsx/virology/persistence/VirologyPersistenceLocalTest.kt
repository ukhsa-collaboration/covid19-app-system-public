package uk.nhs.nhsx.virology.persistence

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.amazonaws.services.dynamodbv2.document.Table
import com.amazonaws.services.dynamodbv2.local.embedded.DynamoDBEmbedded
import com.amazonaws.services.dynamodbv2.local.shared.access.AmazonDynamoDBLocal
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest
import com.amazonaws.services.dynamodbv2.model.DeleteItemRequest
import com.amazonaws.services.dynamodbv2.model.GlobalSecondaryIndex
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement
import com.amazonaws.services.dynamodbv2.model.KeyType
import com.amazonaws.services.dynamodbv2.model.Projection
import com.amazonaws.services.dynamodbv2.model.ProjectionType
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput
import com.amazonaws.services.dynamodbv2.model.PutItemRequest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.aws.dynamodb.DynamoAttributes.numericAttribute
import uk.nhs.nhsx.core.aws.dynamodb.DynamoAttributes.stringAttribute
import uk.nhs.nhsx.core.exceptions.TransactionException
import uk.nhs.nhsx.virology.CtaToken
import uk.nhs.nhsx.virology.TestKit
import uk.nhs.nhsx.virology.TestKit.LAB_RESULT
import uk.nhs.nhsx.virology.TestResultPollingToken
import uk.nhs.nhsx.virology.VirologyConfig
import uk.nhs.nhsx.virology.order.TokensGenerator
import uk.nhs.nhsx.virology.result.VirologyResultRequest
import java.time.Instant
import java.time.Period
import java.util.function.Supplier

class VirologyPersistenceLocalTest {

    private val virologyConfig = VirologyConfig(
        "virology-ordertokens",
        "virology-testresults",
        "virology-submissiontokens",
        "virology-ordertokens-index",
        VirologyConfig.MAX_TOKEN_PERSISTENCE_RETRY_COUNT
    )

    private lateinit var orderTable: Table
    private lateinit var resultTable: Table
    private lateinit var submissionTable: Table
    private lateinit var persistence: VirologyPersistenceService

    private val nowDateTime = "2020-12-01T00:00:00Z"
    private val clock = Supplier { Instant.parse(nowDateTime) }
    private val fourWeeksTtl = clock.get().plus(Period.ofWeeks(4)).epochSecond

    private val testOrder = TestOrder(
        "cc8f0b6z",
        "09657719-fe58-46a3-a3a3-a8db82d48043",
        "9dd3a549-2db0-4ba4-aadb-b32e235d4cc0"
    )

    private fun positiveTestResult(testKit: TestKit = LAB_RESULT) = TestResult(
        testOrder.testResultPollingToken.value,
        nowDateTime,
        VirologyResultRequest.NPEX_POSITIVE,
        "available",
        testKit
    )

    private val negativeTestResult = TestResult(
        testOrder.testResultPollingToken.value,
        nowDateTime,
        VirologyResultRequest.NPEX_NEGATIVE,
        "available",
        LAB_RESULT
    )

    private val voidTestResult = TestResult(
        testOrder.testResultPollingToken.value,
        nowDateTime,
        VirologyResultRequest.NPEX_VOID,
        "available",
        LAB_RESULT
    )

    private val pendingTestResult = TestResult(
        testOrder.testResultPollingToken.value,
        null,
        null,
        "pending",
        null
    )

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
        createOrderTable()
        createResultTable()
        createSubmissionTable()

        orderTable = dynamoDB.getTable(virologyConfig.testOrdersTable)
        resultTable = dynamoDB.getTable(virologyConfig.testResultsTable)
        submissionTable = dynamoDB.getTable(virologyConfig.submissionTokensTable)
        persistence = VirologyPersistenceService(dbClient, virologyConfig)
    }

    @AfterEach
    fun destroy() {
        dbClient.deleteTable(virologyConfig.testOrdersTable)
        dbClient.deleteTable(virologyConfig.testResultsTable)
        dbClient.deleteTable(virologyConfig.submissionTokensTable)
    }

    @Test
    fun `gets virology order`() {
        val itemMap = mapOf(
            "ctaToken" to stringAttribute(testOrder.ctaToken.value),
            "testResultPollingToken" to stringAttribute(testOrder.testResultPollingToken.value),
            "diagnosisKeySubmissionToken" to stringAttribute(testOrder.diagnosisKeySubmissionToken.value),
            "expireAt" to numericAttribute(fourWeeksTtl)
        )

        val request = PutItemRequest()
            .withTableName(virologyConfig.testOrdersTable)
            .withItem(itemMap)

        dbClient.putItem(request)

        persistence
            .getTestOrder(CtaToken.of(testOrder.ctaToken.value))
            .map {
                assertThat(it.ctaToken).isEqualTo(CtaToken.of(testOrder.ctaToken.value))
                assertThat(it.diagnosisKeySubmissionToken).isEqualTo(testOrder.diagnosisKeySubmissionToken)
                assertThat(it.testResultPollingToken).isEqualTo(testOrder.testResultPollingToken)
            }
            .orElseThrow { RuntimeException("Token not found") }
    }

    @Test
    fun `gets empty for order not found`() {
        assertThat(persistence.getTestOrder(CtaToken.of(testOrder.ctaToken.value))).isEmpty
    }

    @Test
    fun `gets final virology result`() {
        val itemMap = mapOf(
            "testResultPollingToken" to stringAttribute(positiveTestResult().testResultPollingToken),
            "testEndDate" to stringAttribute(positiveTestResult().testEndDate),
            "testResult" to stringAttribute(positiveTestResult().testResult),
            "status" to stringAttribute(positiveTestResult().status),
            "testKit" to stringAttribute(positiveTestResult().testKit.name)
        )

        val request = PutItemRequest()
            .withTableName(virologyConfig.testResultsTable)
            .withItem(itemMap)

        dbClient.putItem(request)

        persistence
            .getTestResult(TestResultPollingToken.of(positiveTestResult().testResultPollingToken))
            .map {
                assertThat(it.testResultPollingToken).isEqualTo(positiveTestResult().testResultPollingToken)
                assertThat(it.testEndDate).isEqualTo(positiveTestResult().testEndDate)
                assertThat(it.testResult).isEqualTo(positiveTestResult().testResult)
                assertThat(it.status).isEqualTo(positiveTestResult().status)
                assertThat(it.testKit).isEqualTo(positiveTestResult().testKit)
            }
            .orElseThrow { RuntimeException("Token not found") }
    }

    @Test
    fun `gets pending virology result`() {
        val itemMap = mapOf(
            "testResultPollingToken" to stringAttribute(pendingTestResult.testResultPollingToken),
            "status" to stringAttribute(pendingTestResult.status),
            "testKit" to stringAttribute(TestKit.RAPID_RESULT.name)
        )

        val request = PutItemRequest()
            .withTableName(virologyConfig.testResultsTable)
            .withItem(itemMap)

        dbClient.putItem(request)

        persistence
            .getTestResult(TestResultPollingToken.of(pendingTestResult.testResultPollingToken))
            .map {
                assertThat(it.testResultPollingToken).isEqualTo(pendingTestResult.testResultPollingToken)
                assertThat(it.testEndDate).isEmpty()
                assertThat(it.testResult).isEmpty()
                assertThat(it.status).isEqualTo(pendingTestResult.status)
                assertThat(it.testKit).isEqualTo(TestKit.RAPID_RESULT)
            }
            .orElseThrow { RuntimeException("Token not found") }
    }

    @Test
    fun `gets pending virology result - defaults to LAB_RESULT if no testKit stored`() {
        val itemMap = mapOf(
            "testResultPollingToken" to stringAttribute(pendingTestResult.testResultPollingToken),
            "status" to stringAttribute(pendingTestResult.status)
        )

        val request = PutItemRequest()
            .withTableName(virologyConfig.testResultsTable)
            .withItem(itemMap)

        dbClient.putItem(request)

        persistence
            .getTestResult(TestResultPollingToken.of(pendingTestResult.testResultPollingToken))
            .map {
                assertThat(it.testResultPollingToken).isEqualTo(pendingTestResult.testResultPollingToken)
                assertThat(it.testEndDate).isEmpty()
                assertThat(it.testResult).isEmpty()
                assertThat(it.status).isEqualTo(pendingTestResult.status)
                assertThat(it.testKit).isEqualTo(LAB_RESULT)
            }
            .orElseThrow { RuntimeException("Token not found") }
    }

    @Test
    fun `gets empty for result not found`() {
        assertThat(persistence.getTestResult(TestResultPollingToken.of("some token"))).isEmpty
    }

    @Test
    fun `throws when test result with missing status`() {
        val itemMap = mapOf(
            "testResultPollingToken" to stringAttribute(pendingTestResult.testResultPollingToken),
        )

        val request = PutItemRequest()
            .withTableName(virologyConfig.testResultsTable)
            .withItem(itemMap)

        dbClient.putItem(request)

        assertThatThrownBy { persistence.getTestResult(TestResultPollingToken.of(pendingTestResult.testResultPollingToken)) }
            .isInstanceOf(RuntimeException::class.java)
            .hasMessage("Required field missing")
    }

    @Test
    fun `creates test order returning test order unmodified`() {
        val persisted = persistence.persistTestOrder({ testOrder }, fourWeeksTtl)

        assertThat(persisted.ctaToken).isEqualTo(testOrder.ctaToken)
        assertThat(persisted.diagnosisKeySubmissionToken).isEqualTo(testOrder.diagnosisKeySubmissionToken)
        assertThat(persisted.testResultPollingToken).isEqualTo(testOrder.testResultPollingToken)
    }

    @Test
    fun `creates test order and initial result`() {
        persistence.persistTestOrder({ testOrder }, fourWeeksTtl)

        assertOrderIsPresent(testOrder)
        assertTestResultIsPresent(testOrder, pendingTestResult)
        assertSubmissionIsNotPresent()
    }

    @Test
    fun `creating tokens with collision should generate new token`() {
        val tokens = TokensGenerator().generateVirologyTokens()
        val tokensWithCollision = TestOrder(
            tokens.ctaToken.value,
            "testResultPollingToken-collision",
            "diagnosisKeySubmissionToken-collision"
        )
        val tokensNoCollision = TestOrder(
            TokensGenerator().generateVirologyTokens().ctaToken.value,
            "testResultPollingToken-no-collision",
            "diagnosisKeySubmissionToken-no-collision"
        )

        val tokensSupplier = mockk<Supplier<TestOrder>>()
        every { tokensSupplier.get() } returns tokensWithCollision andThen tokensWithCollision andThen tokensNoCollision
        persistence.persistTestOrder({ tokens }, fourWeeksTtl)
        assertOrderIsPresent(tokens)
        assertTestResultIsPresent(tokens, pendingTestResult)

        val persistedTokens = persistence.persistTestOrder(tokensSupplier, fourWeeksTtl)
        assertOrderIsPresent(persistedTokens)
        assertTestResultIsPresent(persistedTokens, pendingTestResult)
        verify(exactly = 3) { tokensSupplier.get() }
    }

    @Test
    fun `creating tokens with collision should stop after too many retries`() {
        val tokens = TokensGenerator().generateVirologyTokens()
        val tokensWithCollision = TestOrder(
            tokens.ctaToken.value,
            "testResultPollingToken-collision",
            "diagnosisKeySubmissionToken-collision"
        )
        persistence.persistTestOrder({ tokens }, fourWeeksTtl)
        assertOrderIsPresent(tokens)
        assertTestResultIsPresent(tokens, pendingTestResult)
        assertThatThrownBy { persistence.persistTestOrder({ tokensWithCollision }, fourWeeksTtl) }
            .isInstanceOf(RuntimeException::class.java)
    }

    @Test
    fun `creates test order and final positive result`() {
        persistence.persistTestOrderAndResult(
            { testOrder },
            fourWeeksTtl,
            positiveTestResult().testResult,
            positiveTestResult().testEndDate,
            positiveTestResult().testKit
        )

        assertOrderIsPresent(testOrder)
        assertTestResultIsPresent(testOrder, positiveTestResult())
        assertSubmissionIsPresent(testOrder, testKit = LAB_RESULT)
    }

    @Test
    fun `creates test order and final negative result`() {
        persistence.persistTestOrderAndResult(
            { testOrder },
            fourWeeksTtl,
            negativeTestResult.testResult,
            negativeTestResult.testEndDate,
            negativeTestResult.testKit
        )

        assertOrderIsPresent(testOrder)
        assertTestResultIsPresent(testOrder, negativeTestResult)
        assertSubmissionIsNotPresent()
    }

    @Test
    fun `marks existing positive test result for deletion`() {
        persistence.persistTestOrderAndResult(
            { testOrder },
            fourWeeksTtl,
            positiveTestResult().testResult,
            positiveTestResult().testEndDate,
            positiveTestResult().testKit
        )

        persistence.markForDeletion(
            positiveTestResult(),
            VirologyDataTimeToLive(10, 20)
        )

        assertOrderIsPresent(testOrder, expireAt = 10)
        assertTestResultIsPresent(testOrder, positiveTestResult(), expireAt = 10)
        assertSubmissionIsPresent(testOrder, expireAt = 20, testKit = LAB_RESULT)
    }

    @Test
    fun `marks existing negative test result for deletion`() {
        persistence.persistTestOrderAndResult(
            { testOrder },
            fourWeeksTtl,
            negativeTestResult.testResult,
            negativeTestResult.testEndDate,
            negativeTestResult.testKit
        )

        persistence.markForDeletion(
            negativeTestResult,
            VirologyDataTimeToLive(10, 20)
        )

        assertOrderIsPresent(testOrder, expireAt = 10)
        assertTestResultIsPresent(testOrder, negativeTestResult, expireAt = 10)
        assertSubmissionIsNotPresent()
    }

    @Test
    fun `mark for deletion throws not allowing re-activating submission token after it is deleted`() {
        val testOrder = persistence.persistTestOrder({ testOrder }, fourWeeksTtl)

        val positiveResult = VirologyResultRequest(
            testOrder.ctaToken.value,
            positiveTestResult().testEndDate,
            positiveTestResult().testResult,
            positiveTestResult().testKit
        )
        persistence.persistPositiveTestResult(
            VirologyResultRequest.Positive.from(positiveResult),
            fourWeeksTtl
        )

        submissionTable.deleteItem("diagnosisKeySubmissionToken", testOrder.diagnosisKeySubmissionToken.value)
        assertSubmissionIsNotPresent()

        assertThatThrownBy {
            persistence.markForDeletion(positiveTestResult(), VirologyDataTimeToLive(10, 20))
        }.isInstanceOf(TransactionException::class.java)

        assertSubmissionIsNotPresent()
    }

    @Test
    fun `does not mark for deletion if test result not found`() {
        val randomResult = TestResult(
            "some-random-id",
            nowDateTime,
            VirologyResultRequest.NPEX_POSITIVE,
            "available",
            null
        )

        persistence.markForDeletion(
            randomResult,
            VirologyDataTimeToLive(10, 20)
        )

        assertOrderIsNotPresent()
        assertResultIsNotPresent()
        assertSubmissionIsNotPresent()
    }

    @Test
    fun `persists positive test result`() {
        TestKit.values().forEach {
            val testOrder = TokensGenerator().generateVirologyTokens()
            persistence.persistTestOrder({ testOrder }, fourWeeksTtl)

            val testResult = positiveTestResult(it)

            val virologyResultRequest = VirologyResultRequest(
                testOrder.ctaToken.value,
                testResult.testEndDate,
                testResult.testResult,
                it
            )
            val result = persistence.persistPositiveTestResult(
                VirologyResultRequest.Positive.from(virologyResultRequest),
                fourWeeksTtl
            )

            assertOrderIsPresent(testOrder)
            assertTestResultIsPresent(testOrder, testResult)
            assertSubmissionIsPresent(testOrder, testKit = it)
            assertThat(result).isInstanceOf(VirologyResultPersistOperation.Success::class.java)
        }
    }

    @Test
    fun `persists negative test result`() {
        persistence.persistTestOrder({ testOrder }, fourWeeksTtl)

        val virologyResultRequest = VirologyResultRequest(
            testOrder.ctaToken.value,
            negativeTestResult.testEndDate,
            negativeTestResult.testResult,
            negativeTestResult.testKit
        )
        val result = persistence.persistNonPositiveTestResult(
            VirologyResultRequest.NonPositive.from(virologyResultRequest)
        )

        assertOrderIsPresent(testOrder)
        assertTestResultIsPresent(testOrder, negativeTestResult)
        assertSubmissionIsNotPresent()
        assertThat(result).isInstanceOf(VirologyResultPersistOperation.Success::class.java)
    }

    @Test
    fun `persists void test result`() {
        persistence.persistTestOrder({ testOrder }, fourWeeksTtl)

        val virologyResultRequest = VirologyResultRequest(
            testOrder.ctaToken.value,
            voidTestResult.testEndDate,
            voidTestResult.testResult,
            voidTestResult.testKit
        )
        val result = persistence.persistNonPositiveTestResult(
            VirologyResultRequest.NonPositive.from(virologyResultRequest)
        )

        assertOrderIsPresent(testOrder)
        assertTestResultIsPresent(testOrder, voidTestResult)
        assertSubmissionIsNotPresent()
        assertThat(result).isInstanceOf(VirologyResultPersistOperation.Success::class.java)
    }

    @Test
    fun `transaction fails when persisting test result that is already available`() {
        persistence.persistTestOrder({ testOrder }, fourWeeksTtl)

        val positiveResult = VirologyResultRequest(
            testOrder.ctaToken.value,
            positiveTestResult().testEndDate,
            positiveTestResult().testResult,
            positiveTestResult().testKit
        )
        persistence.persistPositiveTestResult(
            VirologyResultRequest.Positive.from(positiveResult),
            fourWeeksTtl
        )

        val negativeResult = VirologyResultRequest(
            testOrder.ctaToken.value,
            negativeTestResult.testEndDate,
            negativeTestResult.testResult,
            negativeTestResult.testKit
        )
        val result = persistence.persistNonPositiveTestResult(
            VirologyResultRequest.NonPositive.from(negativeResult)
        )

        assertThat(result)
            .isInstanceOf(VirologyResultPersistOperation.TransactionFailed::class.java)
    }

    @Test
    fun `order not found when persisting a result for a order that does not exist`() {
        val positiveResult = VirologyResultRequest(
            testOrder.ctaToken.value,
            positiveTestResult().testEndDate,
            positiveTestResult().testResult
        )
        persistence.persistPositiveTestResult(
            VirologyResultRequest.Positive.from(positiveResult),
            fourWeeksTtl
        )

        val negativeResult = VirologyResultRequest(
            testOrder.ctaToken.value,
            negativeTestResult.testEndDate,
            negativeTestResult.testResult
        )
        val result = persistence.persistNonPositiveTestResult(
            VirologyResultRequest.NonPositive.from(negativeResult)
        )

        assertThat(result)
            .isInstanceOf(VirologyResultPersistOperation.OrderNotFound::class.java)
    }

    @Test
    fun `updates cta exchange positive test result`() {
        persistence.persistTestOrderAndResult(
            { testOrder },
            fourWeeksTtl,
            positiveTestResult().testResult,
            positiveTestResult().testEndDate,
            positiveTestResult().testKit
        )

        persistence.updateOnCtaExchange(
            testOrder,
            positiveTestResult(),
            VirologyDataTimeToLive(10, 20)
        )

        assertOrderIsPresent(testOrder, expireAt = 10, downloadCount = 1)
        assertTestResultIsPresent(testOrder, positiveTestResult(), expireAt = 10)
        assertSubmissionIsPresent(testOrder, expireAt = 20, testKit = LAB_RESULT)
    }

    @Test
    fun `updates cta exchange positive test result with missing submission token`() {
        persistence.persistTestOrderAndResult(
            { testOrder },
            fourWeeksTtl,
            positiveTestResult().testResult,
            positiveTestResult().testEndDate,
            positiveTestResult().testKit
        )

        dbClient.deleteItem(
            DeleteItemRequest()
                .withTableName(virologyConfig.submissionTokensTable)
                .withKey(
                    mapOf("diagnosisKeySubmissionToken" to stringAttribute(testOrder.diagnosisKeySubmissionToken.value))
                )
        )

        persistence.updateOnCtaExchange(
            testOrder,
            positiveTestResult(),
            VirologyDataTimeToLive(10, 20)
        )

        assertOrderIsPresent(testOrder, expireAt = 10, downloadCount = 1)
        assertTestResultIsPresent(testOrder, positiveTestResult(), expireAt = 10)
        assertSubmissionIsNotPresent()
    }

    @Test
    fun `updates cta exchange negative test result`() {
        persistence.persistTestOrderAndResult(
            { testOrder },
            fourWeeksTtl,
            negativeTestResult.testResult,
            negativeTestResult.testEndDate,
            negativeTestResult.testKit
        )

        persistence.updateOnCtaExchange(
            testOrder,
            negativeTestResult,
            VirologyDataTimeToLive(10, 20)
        )

        assertOrderIsPresent(testOrder, expireAt = 10, downloadCount = 1)
        assertTestResultIsPresent(testOrder, negativeTestResult, expireAt = 10)
        assertSubmissionIsNotPresent()
    }

    @Test
    fun `updates cta exchange increments downloadCount field`() {
        persistence.persistTestOrderAndResult(
            { testOrder },
            fourWeeksTtl,
            negativeTestResult.testResult,
            negativeTestResult.testEndDate,
            negativeTestResult.testKit
        )

        persistence.updateOnCtaExchange(
            testOrder,
            negativeTestResult,
            VirologyDataTimeToLive(10, 20)
        )

        persistence.updateOnCtaExchange(
            testOrder,
            negativeTestResult,
            VirologyDataTimeToLive(10, 20)
        )

        assertOrderIsPresent(testOrder, expireAt = 10, downloadCount = 2)
    }

    private fun createOrderTable() {
        val attributeDefinitions = listOf(
            AttributeDefinition().withAttributeName("ctaToken").withAttributeType("S"),
            AttributeDefinition().withAttributeName("testResultPollingToken").withAttributeType("S")
        )

        val keySchema = listOf(
            KeySchemaElement().withAttributeName("ctaToken").withKeyType(KeyType.HASH)
        )

        val globalSecondaryIndex = GlobalSecondaryIndex()
            .withIndexName(virologyConfig.testOrdersIndex)
            .withKeySchema(
                listOf(
                    KeySchemaElement().withAttributeName("testResultPollingToken").withKeyType(KeyType.HASH)
                )
            )
            .withProjection(
                Projection()
                    .withProjectionType(ProjectionType.INCLUDE)
                    .withNonKeyAttributes(listOf("diagnosisKeySubmissionToken", "ctaToken"))
            )
            .withProvisionedThroughput(ProvisionedThroughput(100L, 100L))

        val request = CreateTableRequest()
            .withTableName(virologyConfig.testOrdersTable)
            .withKeySchema(keySchema)
            .withAttributeDefinitions(attributeDefinitions)
            .withProvisionedThroughput(ProvisionedThroughput(100L, 100L))
            .withGlobalSecondaryIndexes(globalSecondaryIndex)

        dbClient.createTable(request)
    }

    private fun createResultTable() {
        val attributeDefinitions = listOf(
            AttributeDefinition().withAttributeName("testResultPollingToken").withAttributeType("S")
        )

        val keySchema = listOf(
            KeySchemaElement().withAttributeName("testResultPollingToken").withKeyType(KeyType.HASH)
        )

        val request = CreateTableRequest()
            .withTableName(virologyConfig.testResultsTable)
            .withKeySchema(keySchema)
            .withAttributeDefinitions(attributeDefinitions)
            .withProvisionedThroughput(ProvisionedThroughput(100L, 100L))

        dbClient.createTable(request)
    }

    private fun createSubmissionTable() {
        val attributeDefinitions = listOf(
            AttributeDefinition().withAttributeName("diagnosisKeySubmissionToken").withAttributeType("S")
        )

        val keySchema = listOf(
            KeySchemaElement().withAttributeName("diagnosisKeySubmissionToken").withKeyType(KeyType.HASH)
        )

        val request = CreateTableRequest()
            .withTableName(virologyConfig.submissionTokensTable)
            .withKeySchema(keySchema)
            .withAttributeDefinitions(attributeDefinitions)
            .withProvisionedThroughput(ProvisionedThroughput(100L, 100L))

        dbClient.createTable(request)
    }

    private fun assertOrderIsPresent(testOrder: TestOrder,
                                     expireAt: Long = fourWeeksTtl,
                                     downloadCount: Int? = null) {
        val ctaToken = testOrder.ctaToken.value
        val order = orderTable.getItem("ctaToken", ctaToken)

        val expectedOrderMap = mutableMapOf<String, Any>(
            "ctaToken" to ctaToken,
            "diagnosisKeySubmissionToken" to testOrder.diagnosisKeySubmissionToken.value,
            "testResultPollingToken" to testOrder.testResultPollingToken.value,
            "expireAt" to expireAt.toBigDecimal()
        )

        if (downloadCount != null)
            expectedOrderMap["downloadCount"] = downloadCount.toBigDecimal()

        assertThat(order.asMap()).isEqualTo(expectedOrderMap)
    }

    private fun assertTestResultIsPresent(testOrder: TestOrder,
                                          testResult: TestResult,
                                          expireAt: Long = fourWeeksTtl) {
        val testResultPollingToken = testOrder.testResultPollingToken.value
        val result = resultTable.getItem("testResultPollingToken", testResultPollingToken)

        assertThat(result.asMap()).isEqualTo(
            when (testResult.status) {
                "pending" -> mapOf(
                    "testResultPollingToken" to testResultPollingToken,
                    "status" to "pending",
                    "expireAt" to expireAt.toBigDecimal()
                )
                else -> mapOf(
                    "testResultPollingToken" to testResultPollingToken,
                    "status" to testResult.status,
                    "testEndDate" to testResult.testEndDate,
                    "testResult" to testResult.testResult,
                    "expireAt" to expireAt.toBigDecimal(),
                    "testKit" to testResult.testKit.name
                )
            }
        )
    }

    private fun assertSubmissionIsPresent(testOrder: TestOrder,
                                          expireAt: Long = fourWeeksTtl,
                                          testKit: TestKit) {
        val diagnosisKeySubmissionToken = testOrder.diagnosisKeySubmissionToken.value
        val submission = submissionTable.getItem("diagnosisKeySubmissionToken", diagnosisKeySubmissionToken)

        assertThat(submission.asMap()).isEqualTo(
            mapOf(
                "diagnosisKeySubmissionToken" to diagnosisKeySubmissionToken,
                "expireAt" to expireAt.toBigDecimal(),
                "testKit" to testKit.name
            )
        )
    }

    private fun assertOrderIsNotPresent() {
        val order = orderTable.getItem("ctaToken", testOrder.ctaToken.value)
        assertThat(order).isNull()
    }

    private fun assertResultIsNotPresent() {
        val result = resultTable.getItem("testResultPollingToken", testOrder.testResultPollingToken.value)
        assertThat(result).isNull()
    }

    private fun assertSubmissionIsNotPresent() {
        val submission = submissionTable.getItem("diagnosisKeySubmissionToken", testOrder.diagnosisKeySubmissionToken.value)
        assertThat(submission).isNull()
    }
}
