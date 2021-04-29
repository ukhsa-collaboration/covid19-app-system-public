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
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.core.exceptions.TransactionException
import uk.nhs.nhsx.testhelper.data.asInstant
import uk.nhs.nhsx.domain.CtaToken
import uk.nhs.nhsx.domain.DiagnosisKeySubmissionToken
import uk.nhs.nhsx.domain.TestKit
import uk.nhs.nhsx.domain.TestKit.LAB_RESULT
import uk.nhs.nhsx.domain.TestResultPollingToken
import uk.nhs.nhsx.virology.VirologyConfig
import uk.nhs.nhsx.virology.order.TokensGenerator
import uk.nhs.nhsx.virology.persistence.TestResultAvailability.AVAILABLE
import uk.nhs.nhsx.virology.persistence.TestState.AvailableTestResult
import uk.nhs.nhsx.virology.persistence.TestState.PendingTestResult
import uk.nhs.nhsx.virology.persistence.VirologyResultPersistOperation.OrderNotFound
import uk.nhs.nhsx.virology.persistence.VirologyResultPersistOperation.TransactionFailed
import uk.nhs.nhsx.domain.TestEndDate
import uk.nhs.nhsx.domain.TestResult.Negative
import uk.nhs.nhsx.domain.TestResult.Positive
import uk.nhs.nhsx.domain.TestResult.Void
import uk.nhs.nhsx.virology.result.VirologyResultRequestV2
import java.time.Instant
import java.time.Period
import java.util.function.Supplier

class VirologyPersistenceLocalTest {

    private val virologyConfig = VirologyConfig(
        "virology-ordertokens",
        "virology-testresults",
        "virology-submissiontokens",
        "virology-ordertokens-index"
    )

    private lateinit var orderTable: Table
    private lateinit var resultTable: Table
    private lateinit var submissionTable: Table
    private lateinit var persistence: VirologyPersistenceService

    private val nowString = "2020-12-01T00:00:00Z"
    private val clock = { nowString.asInstant() }
    private val fourWeeksTtl = clock().plus(Period.ofWeeks(4))

    private val testOrder = TestOrder(
        CtaToken.of("cc8f0b6z"),
        TestResultPollingToken.of("09657719-fe58-46a3-a3a3-a8db82d48043"),
        DiagnosisKeySubmissionToken.of("9dd3a549-2db0-4ba4-aadb-b32e235d4cc0")
    )

    private fun positiveTestResult(testKit: TestKit = LAB_RESULT) =
        AvailableTestResult(
            testOrder.testResultPollingToken,
            TestEndDate.parse(nowString),
            Positive,
            testKit
        )

    private fun negativeTestResult(testKit: TestKit = LAB_RESULT) =
        AvailableTestResult(
            testOrder.testResultPollingToken,
            TestEndDate.parse(nowString),
            Negative,
            testKit
        )

    private fun voidTestResult(testKit: TestKit = LAB_RESULT) =
        AvailableTestResult(
            testOrder.testResultPollingToken,
            TestEndDate.parse(nowString),
            Void,
            testKit
        )

    private val pendingTestResult = PendingTestResult(
        testOrder.testResultPollingToken,
        LAB_RESULT
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
        persistence = VirologyPersistenceService(dbClient, virologyConfig, RecordingEvents())
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
            "ctaToken" to stringAttribute(testOrder.ctaToken),
            "testResultPollingToken" to stringAttribute(testOrder.testResultPollingToken),
            "diagnosisKeySubmissionToken" to stringAttribute(testOrder.diagnosisKeySubmissionToken),
            "expireAt" to numericAttribute(fourWeeksTtl)
        )

        val request = PutItemRequest()
            .withTableName(virologyConfig.testOrdersTable)
            .withItem(itemMap)

        dbClient.putItem(request)

        persistence
            .getTestOrder(testOrder.ctaToken)
            .map {
                assertThat(it.ctaToken).isEqualTo(testOrder.ctaToken)
                assertThat(it.diagnosisKeySubmissionToken).isEqualTo(testOrder.diagnosisKeySubmissionToken)
                assertThat(it.testResultPollingToken).isEqualTo(testOrder.testResultPollingToken)
            }
            .orElseThrow { RuntimeException("Token not found") }
    }

    @Test
    fun `gets empty for order not found`() {
        assertThat(persistence.getTestOrder(testOrder.ctaToken)).isEmpty
    }

    @Test
    fun `gets final virology result`() {
        val testResult = positiveTestResult()

        val itemMap = mapOf(
            "testResultPollingToken" to stringAttribute(testResult.testResultPollingToken),
            "testEndDate" to stringAttribute(TestEndDate.show(testResult.testEndDate)),
            "testResult" to stringAttribute(testResult.testResult.wireValue),
            "status" to stringAttribute(AVAILABLE.text),
            "testKit" to stringAttribute(testResult.testKit.name)
        )

        val request = PutItemRequest()
            .withTableName(virologyConfig.testResultsTable)
            .withItem(itemMap)

        dbClient.putItem(request)

        persistence
            .getTestResult(testResult.testResultPollingToken)
            .map {
                it as AvailableTestResult
                assertThat(it.testResultPollingToken).isEqualTo(testResult.testResultPollingToken)
                assertThat(it.testEndDate).isEqualTo(testResult.testEndDate)
                assertThat(it.testResult).isEqualTo(testResult.testResult)
                assertThat(it.testKit).isEqualTo(testResult.testKit)
            }
            .orElseThrow { RuntimeException("Token not found") }
    }

    @Test
    fun `gets pending virology result`() {
        val itemMap = mapOf(
            "testResultPollingToken" to stringAttribute(pendingTestResult.testResultPollingToken),
            "status" to stringAttribute(TestResultAvailability.PENDING.text),
            "testKit" to stringAttribute(TestKit.RAPID_RESULT.name)
        )

        val request = PutItemRequest()
            .withTableName(virologyConfig.testResultsTable)
            .withItem(itemMap)

        dbClient.putItem(request)

        persistence
            .getTestResult(pendingTestResult.testResultPollingToken)
            .map {
                it as PendingTestResult
                assertThat(it.testResultPollingToken).isEqualTo(pendingTestResult.testResultPollingToken)
                assertThat(it.testKit).isEqualTo(TestKit.RAPID_RESULT)
            }
            .orElseThrow { RuntimeException("Token not found") }
    }

    @Test
    fun `gets pending virology result - defaults to LAB_RESULT if no testKit stored`() {
        val itemMap = mapOf(
            "testResultPollingToken" to stringAttribute(pendingTestResult.testResultPollingToken),
            "status" to stringAttribute(TestResultAvailability.PENDING.text)
        )

        val request = PutItemRequest()
            .withTableName(virologyConfig.testResultsTable)
            .withItem(itemMap)

        dbClient.putItem(request)

        persistence
            .getTestResult(pendingTestResult.testResultPollingToken)
            .map {
                it as PendingTestResult
                assertThat(it.testResultPollingToken).isEqualTo(pendingTestResult.testResultPollingToken)
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

        assertThatThrownBy { persistence.getTestResult(pendingTestResult.testResultPollingToken) }
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
        val tokens = TokensGenerator.generateVirologyTokens()
        val tokensWithCollision = TestOrder(
            tokens.ctaToken,
            TestResultPollingToken.of("testResultPollingToken-collision"),
            DiagnosisKeySubmissionToken.of("diagnosisKeySubmissionToken-collision")
        )
        val tokensNoCollision = TestOrder(
            TokensGenerator.generateVirologyTokens().ctaToken,
            TestResultPollingToken.of("testResultPollingToken-no-collision"),
            DiagnosisKeySubmissionToken.of("diagnosisKeySubmissionToken-no-collision")
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
        val tokens = TokensGenerator.generateVirologyTokens()
        val tokensWithCollision = TestOrder(
            tokens.ctaToken,
            TestResultPollingToken.of("testResultPollingToken-collision"),
            DiagnosisKeySubmissionToken.of("diagnosisKeySubmissionToken-collision")
        )
        persistence.persistTestOrder({ tokens }, fourWeeksTtl)
        assertOrderIsPresent(tokens)
        assertTestResultIsPresent(tokens, pendingTestResult)
        assertThatThrownBy { persistence.persistTestOrder({ tokensWithCollision }, fourWeeksTtl) }
            .isInstanceOf(RuntimeException::class.java)
    }

    @Test
    fun `creates test order and final positive result`() {
        val testResult = positiveTestResult()

        persistence.persistTestOrderAndResult(
            { testOrder },
            fourWeeksTtl,
            testResult.testResult,
            testResult.testEndDate,
            testResult.testKit
        )

        assertOrderIsPresent(testOrder)
        assertTestResultIsPresent(testOrder, testResult)
        assertSubmissionIsPresent(testOrder, LAB_RESULT)
    }

    @Test
    fun `creates test order and final negative result`() {
        val testResult = negativeTestResult()

        persistence.persistTestOrderAndResult(
            { testOrder },
            fourWeeksTtl,
            testResult.testResult,
            testResult.testEndDate,
            testResult.testKit
        )

        assertOrderIsPresent(testOrder)
        assertTestResultIsPresent(testOrder, testResult)
        assertSubmissionIsNotPresent()
    }

    private val virologyDataTimeToLive = VirologyDataTimeToLive(
        Instant.ofEpochSecond(10),
        Instant.ofEpochSecond(20)
    )

    @Test
    fun `marks existing positive test result for deletion`() {
        val testResult = positiveTestResult()

        persistence.persistTestOrderAndResult(
            { testOrder },
            fourWeeksTtl,
            testResult.testResult,
            testResult.testEndDate,
            testResult.testKit
        )

        persistence.markForDeletion(
            testResult,
            virologyDataTimeToLive
        )

        assertOrderIsPresent(testOrder, expireAt = Instant.ofEpochSecond(10))
        assertTestResultIsPresent(testOrder, testResult, expireAt = Instant.ofEpochSecond(10))
        assertSubmissionIsPresent(testOrder, LAB_RESULT, expireAt = Instant.ofEpochSecond(20))
    }

    @Test
    fun `marks existing negative test result for deletion`() {
        val testResult = negativeTestResult()

        persistence.persistTestOrderAndResult(
            { testOrder },
            fourWeeksTtl,
            testResult.testResult,
            testResult.testEndDate,
            testResult.testKit
        )

        persistence.markForDeletion(
            testResult,
            virologyDataTimeToLive
        )

        assertOrderIsPresent(testOrder, expireAt = Instant.ofEpochSecond(10))
        assertTestResultIsPresent(testOrder, testResult, expireAt = Instant.ofEpochSecond(10))
        assertSubmissionIsNotPresent()
    }

    @Test
    fun `mark for deletion throws not allowing re-activating submission token after it is deleted`() {
        val testOrder = persistence.persistTestOrder({ testOrder }, fourWeeksTtl)

        val testResult = positiveTestResult()

        val positiveResult = VirologyResultRequestV2(
            testOrder.ctaToken,
            testResult.testEndDate,
            testResult.testResult,
            testResult.testKit
        )
        persistence.persistPositiveTestResult(
            positiveResult,
            fourWeeksTtl
        )

        submissionTable.deleteItem("diagnosisKeySubmissionToken", testOrder.diagnosisKeySubmissionToken.value)
        assertSubmissionIsNotPresent()

        assertThatThrownBy {
            persistence.markForDeletion(testResult, virologyDataTimeToLive)
        }.isInstanceOf(TransactionException::class.java)

        assertSubmissionIsNotPresent()
    }

    @Test
    fun `does not mark for deletion if test result not found`() {
        val randomResult = AvailableTestResult(
            TestResultPollingToken.of("some-random-id"),
            TestEndDate.parse(nowString),
            Positive,
            LAB_RESULT
        )

        persistence.markForDeletion(
            randomResult,
            virologyDataTimeToLive
        )

        assertOrderIsNotPresent()
        assertResultIsNotPresent()
        assertSubmissionIsNotPresent()
    }

    @Test
    fun `persists positive test result for all test kits`() {
        TestKit.values().forEach {
            val testOrder = TokensGenerator.generateVirologyTokens()
            persistence.persistTestOrder({ testOrder }, fourWeeksTtl)

            val testResult = positiveTestResult(it)

            val virologyResultRequest = VirologyResultRequestV2(
                testOrder.ctaToken,
                testResult.testEndDate,
                testResult.testResult,
                it
            )
            val result = persistence.persistPositiveTestResult(
                virologyResultRequest,
                fourWeeksTtl
            )

            assertOrderIsPresent(testOrder)
            assertTestResultIsPresent(testOrder, testResult)
            assertSubmissionIsPresent(testOrder, it)
            assertThat(result).isInstanceOf(VirologyResultPersistOperation.Success::class.java)
        }
    }

    @Test
    fun `persists positive test result in db`() {
        persistence.persistTestOrder({ testOrder }, fourWeeksTtl)

        val testResult = positiveTestResult()

        val virologyResultRequest = VirologyResultRequestV2(
            testOrder.ctaToken,
            testResult.testEndDate,
            testResult.testResult,
            testResult.testKit
        )
        persistence.persistPositiveTestResult(
            virologyResultRequest,
            fourWeeksTtl
        )

        val result = resultTable.getItem("testResultPollingToken", testOrder.testResultPollingToken.value)
        val resultMap = result.asMap()

        assertThat(resultMap).isEqualTo(
            mapOf(
                "testResultPollingToken" to testOrder.testResultPollingToken.value,
                "status" to AVAILABLE.text,
                "testEndDate" to nowString,
                "testResult" to testResult.testResult.wireValue,
                "expireAt" to fourWeeksTtl.epochSecond.toBigDecimal(),
                "testKit" to testResult.testKit.name
            )
        )
    }

    @Test
    fun `persists void test result for Lab test kit`() {
        val testOrder = TokensGenerator.generateVirologyTokens()
        persistence.persistTestOrder({ testOrder }, fourWeeksTtl)

        val testResult = voidTestResult(LAB_RESULT)

        val virologyResultRequest = VirologyResultRequestV2(
            testOrder.ctaToken,
            testResult.testEndDate,
            testResult.testResult,
            testResult.testKit
        )

        val result = persistence.persistNonPositiveTestResult(
            virologyResultRequest
        )

        assertOrderIsPresent(testOrder)
        assertTestResultIsPresent(testOrder, testResult)
        assertSubmissionIsNotPresent()
        assertThat(result).isInstanceOf(VirologyResultPersistOperation.Success::class.java)
    }

    @Test
    fun `transaction fails when persisting test result that is already available`() {
        persistence.persistTestOrder({ testOrder }, fourWeeksTtl)

        val positiveTestResult = positiveTestResult()
        val positiveResult = VirologyResultRequestV2(
            testOrder.ctaToken,
            positiveTestResult.testEndDate,
            positiveTestResult.testResult,
            positiveTestResult.testKit
        )
        persistence.persistPositiveTestResult(
            positiveResult,
            fourWeeksTtl
        )

        val negativeTestResult = negativeTestResult()
        val negativeResult = VirologyResultRequestV2(
            testOrder.ctaToken,
            negativeTestResult.testEndDate,
            negativeTestResult.testResult,
            negativeTestResult.testKit
        )
        val result = persistence.persistNonPositiveTestResult(
            negativeResult
        )

        assertThat(result)
            .isInstanceOf(TransactionFailed::class.java)
    }

    @Test
    fun `order not found when persisting a result for a order that does not exist`() {
        val positiveTestResult = positiveTestResult()
        val positiveResult = VirologyResultRequestV2(
            testOrder.ctaToken,
            positiveTestResult.testEndDate,
            positiveTestResult.testResult,
            positiveTestResult.testKit
        )
        persistence.persistPositiveTestResult(
            positiveResult,
            fourWeeksTtl
        )

        val negativeTestResult = negativeTestResult()
        val negativeResult = VirologyResultRequestV2(
            testOrder.ctaToken,
            negativeTestResult.testEndDate,
            negativeTestResult.testResult,
            negativeTestResult.testKit
        )
        val result = persistence.persistNonPositiveTestResult(
            negativeResult
        )

        assertThat(result)
            .isInstanceOf(OrderNotFound::class.java)
    }

    @Test
    fun `updates cta exchange positive test result`() {
        val testResult = positiveTestResult()

        persistence.persistTestOrderAndResult(
            { testOrder },
            fourWeeksTtl,
            testResult.testResult,
            testResult.testEndDate,
            testResult.testKit
        )

        persistence.updateOnCtaExchange(
            testOrder,
            testResult,
            virologyDataTimeToLive
        )

        assertOrderIsPresent(testOrder, expireAt = Instant.ofEpochSecond(10), downloadCount = 1)
        assertTestResultIsPresent(testOrder, testResult, expireAt = Instant.ofEpochSecond(10))
        assertSubmissionIsPresent(testOrder, LAB_RESULT, expireAt = Instant.ofEpochSecond(20))
    }

    @Test
    fun `updates cta exchange positive test result with missing submission token`() {
        val testResult = positiveTestResult()

        persistence.persistTestOrderAndResult(
            { testOrder },
            fourWeeksTtl,
            testResult.testResult,
            testResult.testEndDate,
            testResult.testKit
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
            testResult,
            virologyDataTimeToLive
        )

        assertOrderIsPresent(testOrder, expireAt = Instant.ofEpochSecond(10), downloadCount = 1)
        assertTestResultIsPresent(testOrder, testResult, expireAt = Instant.ofEpochSecond(10))
        assertSubmissionIsNotPresent()
    }

    @Test
    fun `updates cta exchange negative test result`() {
        val testResult = negativeTestResult()

        persistence.persistTestOrderAndResult(
            { testOrder },
            fourWeeksTtl,
            testResult.testResult,
            testResult.testEndDate,
            testResult.testKit
        )

        persistence.updateOnCtaExchange(
            testOrder,
            testResult,
            virologyDataTimeToLive
        )

        assertOrderIsPresent(testOrder, expireAt = Instant.ofEpochSecond(10), downloadCount = 1)
        assertTestResultIsPresent(testOrder, testResult, expireAt = Instant.ofEpochSecond(10))
        assertSubmissionIsNotPresent()
    }

    @Test
    fun `updates cta exchange increments downloadCount field`() {
        val testResult = negativeTestResult()

        persistence.persistTestOrderAndResult(
            { testOrder },
            fourWeeksTtl,
            testResult.testResult,
            testResult.testEndDate,
            testResult.testKit
        )

        persistence.updateOnCtaExchange(
            testOrder,
            testResult,
            virologyDataTimeToLive
        )

        persistence.updateOnCtaExchange(
            testOrder,
            testResult,
            virologyDataTimeToLive
        )

        assertOrderIsPresent(testOrder, expireAt = Instant.ofEpochSecond(10), downloadCount = 2)
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

    private fun assertOrderIsPresent(
        testOrder: TestOrder,
        expireAt: Instant = fourWeeksTtl,
        downloadCount: Int? = null
    ) {
        val ctaToken = testOrder.ctaToken
        val order = orderTable.getItem("ctaToken", ctaToken.value)

        val expectedOrderMap = mutableMapOf<String, Any>(
            "ctaToken" to ctaToken.value,
            "diagnosisKeySubmissionToken" to testOrder.diagnosisKeySubmissionToken.value,
            "testResultPollingToken" to testOrder.testResultPollingToken.value,
            "expireAt" to expireAt.epochSecond.toBigDecimal()
        )

        if (downloadCount != null) {
            expectedOrderMap["downloadCount"] = downloadCount.toBigDecimal()
        }

        assertThat(order.asMap()).isEqualTo(expectedOrderMap)
    }

    private fun assertTestResultIsPresent(
        testOrder: TestOrder,
        testState: TestState,
        expireAt: Instant = fourWeeksTtl
    ) {
        val testResultPollingToken = testOrder.testResultPollingToken.value
        val result = resultTable.getItem("testResultPollingToken", testResultPollingToken)
        val resultMap = result.asMap()

        when (testState) {
            is AvailableTestResult -> {
                assertThat(resultMap).isEqualTo(
                    mapOf(
                        "testResultPollingToken" to testResultPollingToken,
                        "status" to AVAILABLE.text,
                        "testEndDate" to TestEndDate.show(testState.testEndDate),
                        "testResult" to testState.testResult.wireValue,
                        "expireAt" to expireAt.epochSecond.toBigDecimal(),
                        "testKit" to testState.testKit.name
                    )
                )
            }
            is PendingTestResult -> {
                assertThat(resultMap).isEqualTo(
                    mapOf(
                        "testResultPollingToken" to testResultPollingToken,
                        "status" to TestResultAvailability.PENDING.text,
                        "expireAt" to expireAt.epochSecond.toBigDecimal()
                    )
                )
            }
        }
    }

    private fun assertSubmissionIsPresent(
        testOrder: TestOrder,
        testKit: TestKit,
        expireAt: Instant = fourWeeksTtl
    ) {
        val diagnosisKeySubmissionToken = testOrder.diagnosisKeySubmissionToken.value
        val submission = submissionTable.getItem("diagnosisKeySubmissionToken", diagnosisKeySubmissionToken)

        assertThat(submission.asMap()).isEqualTo(
            mapOf(
                "diagnosisKeySubmissionToken" to diagnosisKeySubmissionToken,
                "expireAt" to expireAt.epochSecond.toBigDecimal(),
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
        val submission =
            submissionTable.getItem("diagnosisKeySubmissionToken", testOrder.diagnosisKeySubmissionToken.value)
        assertThat(submission).isNull()
    }
}
