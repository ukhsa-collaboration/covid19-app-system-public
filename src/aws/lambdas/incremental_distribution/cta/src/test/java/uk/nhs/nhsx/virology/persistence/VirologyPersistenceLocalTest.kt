@file:Suppress("unused")

package uk.nhs.nhsx.virology.persistence

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.amazonaws.services.dynamodbv2.document.Item
import com.amazonaws.services.dynamodbv2.document.Table
import com.amazonaws.services.dynamodbv2.local.embedded.DynamoDBEmbedded
import com.amazonaws.services.dynamodbv2.local.shared.access.AmazonDynamoDBLocal
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest
import com.amazonaws.services.dynamodbv2.model.DeleteItemRequest
import com.amazonaws.services.dynamodbv2.model.GlobalSecondaryIndex
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement
import com.amazonaws.services.dynamodbv2.model.KeyType.HASH
import com.amazonaws.services.dynamodbv2.model.Projection
import com.amazonaws.services.dynamodbv2.model.ProjectionType.INCLUDE
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput
import com.amazonaws.services.dynamodbv2.model.PutItemRequest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import strikt.api.Assertion
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isNotNull
import strikt.assertions.message
import strikt.java.isAbsent
import uk.nhs.nhsx.core.aws.dynamodb.DynamoAttributes.numericAttribute
import uk.nhs.nhsx.core.aws.dynamodb.DynamoAttributes.stringAttribute
import uk.nhs.nhsx.core.aws.dynamodb.IndexName
import uk.nhs.nhsx.core.aws.dynamodb.TableName
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.core.exceptions.TransactionException
import uk.nhs.nhsx.domain.CtaToken
import uk.nhs.nhsx.domain.DiagnosisKeySubmissionToken
import uk.nhs.nhsx.domain.TestEndDate
import uk.nhs.nhsx.domain.TestKit
import uk.nhs.nhsx.domain.TestKit.LAB_RESULT
import uk.nhs.nhsx.domain.TestKit.RAPID_RESULT
import uk.nhs.nhsx.domain.TestResult.Negative
import uk.nhs.nhsx.domain.TestResult.Positive
import uk.nhs.nhsx.domain.TestResult.Void
import uk.nhs.nhsx.domain.TestResultPollingToken
import uk.nhs.nhsx.testhelper.data.asInstant
import uk.nhs.nhsx.virology.VirologyConfig
import uk.nhs.nhsx.virology.order.TokensGenerator
import uk.nhs.nhsx.virology.persistence.TestResultAvailability.AVAILABLE
import uk.nhs.nhsx.virology.persistence.TestState.AvailableTestResult
import uk.nhs.nhsx.virology.persistence.TestState.PendingTestResult
import uk.nhs.nhsx.virology.persistence.VirologyResultPersistOperation.OrderNotFound
import uk.nhs.nhsx.virology.persistence.VirologyResultPersistOperation.Success
import uk.nhs.nhsx.virology.persistence.VirologyResultPersistOperation.TransactionFailed
import uk.nhs.nhsx.virology.result.VirologyResultRequestV2
import java.time.Instant
import java.time.LocalDateTime
import java.time.Period
import java.util.function.Supplier

class VirologyPersistenceLocalTest {

    private lateinit var dbLocal: AmazonDynamoDBLocal
    private lateinit var dbClient: AmazonDynamoDB
    private lateinit var dynamoDB: DynamoDB

    private val virologyConfig = VirologyConfig(
        TableName.of("virology-ordertokens"),
        TableName.of("virology-testresults"),
        TableName.of("virology-submissiontokens"),
        IndexName.of("virology-ordertokens-index")
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
        DiagnosisKeySubmissionToken.of("9dd3a549-2db0-4ba4-aadb-b32e235d4cc0"),
        LocalDateTime.now().plusWeeks(4)
    )

    private val pendingTestResult = PendingTestResult(
        testOrder.testResultPollingToken,
        LAB_RESULT
    )

    @BeforeEach
    fun setup() {
        dbLocal = DynamoDBEmbedded.create()
        dbClient = dbLocal.amazonDynamoDB()
        dynamoDB = DynamoDB(dbClient)

        createOrderTable(virologyConfig)
        createResultTable(virologyConfig)
        createSubmissionTable(virologyConfig)

        orderTable = dynamoDB.getTable(virologyConfig.testOrdersTable)
        resultTable = dynamoDB.getTable(virologyConfig.testResultsTable)
        submissionTable = dynamoDB.getTable(virologyConfig.submissionTokensTable)
        persistence = VirologyPersistenceService(dbClient, virologyConfig, RecordingEvents())
    }

    @AfterEach
    fun destroy() {
        try {
            dbClient.deleteTable(virologyConfig.testOrdersTable)
            dbClient.deleteTable(virologyConfig.testResultsTable)
            dbClient.deleteTable(virologyConfig.submissionTokensTable)
        } finally {
            dbLocal.shutdownNow()
        }
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
            .withTableName(virologyConfig.testOrdersTable.value)
            .withItem(itemMap)

        dbClient.putItem(request)

        val order = persistence
            .getTestOrder(testOrder.ctaToken)
            .orElse(null)

        expectThat(order).isNotNull().and {
            get(TestOrder::ctaToken).isEqualTo(testOrder.ctaToken)
            get(TestOrder::diagnosisKeySubmissionToken).isEqualTo(testOrder.diagnosisKeySubmissionToken)
            get(TestOrder::testResultPollingToken).isEqualTo(testOrder.testResultPollingToken)
        }
    }

    @Test
    fun `gets empty for order not found`() {
        expectThat(persistence.getTestOrder(testOrder.ctaToken)).isAbsent()
    }

    @Test
    fun `gets final virology result`() {
        val testResult = testOrder.positiveTestResult()

        val itemMap = mapOf(
            "testResultPollingToken" to stringAttribute(testResult.testResultPollingToken),
            "testEndDate" to stringAttribute(TestEndDate.show(testResult.testEndDate)),
            "testResult" to stringAttribute(testResult.testResult.wireValue),
            "status" to stringAttribute(AVAILABLE.text),
            "testKit" to stringAttribute(testResult.testKit.name)
        )

        val request = PutItemRequest()
            .withTableName(virologyConfig.testResultsTable.value)
            .withItem(itemMap)

        dbClient.putItem(request)

        val result = persistence
            .getTestResult(testResult.testResultPollingToken)
            .orElse(null)

        expectThat(result).isNotNull().isA<AvailableTestResult>().and {
            get(AvailableTestResult::testResultPollingToken).isEqualTo(testResult.testResultPollingToken)
            get(AvailableTestResult::testEndDate).isEqualTo(testResult.testEndDate)
            get(AvailableTestResult::testResult).isEqualTo(testResult.testResult)
            get(AvailableTestResult::testKit).isEqualTo(testResult.testKit)
        }
    }

    @Test
    fun `gets pending virology result`() {
        val itemMap = mapOf(
            "testResultPollingToken" to stringAttribute(pendingTestResult.testResultPollingToken),
            "status" to stringAttribute(TestResultAvailability.PENDING.text),
            "testKit" to stringAttribute(RAPID_RESULT.name)
        )

        val request = PutItemRequest()
            .withTableName(virologyConfig.testResultsTable.value)
            .withItem(itemMap)

        dbClient.putItem(request)

        val result = persistence
            .getTestResult(pendingTestResult.testResultPollingToken)
            .orElse(null)

        expectThat(result).isNotNull().isA<PendingTestResult>().and {
            get(PendingTestResult::testResultPollingToken).isEqualTo(pendingTestResult.testResultPollingToken)
            get(PendingTestResult::testKit).isEqualTo(RAPID_RESULT)
        }
    }

    @Test
    fun `gets pending virology result - defaults to LAB_RESULT if no testKit stored`() {
        val itemMap = mapOf(
            "testResultPollingToken" to stringAttribute(pendingTestResult.testResultPollingToken),
            "status" to stringAttribute(TestResultAvailability.PENDING.text)
        )

        val request = PutItemRequest()
            .withTableName(virologyConfig.testResultsTable.value)
            .withItem(itemMap)

        dbClient.putItem(request)

        val result = persistence
            .getTestResult(pendingTestResult.testResultPollingToken)
            .orElse(null)

        expectThat(result).isNotNull().isA<PendingTestResult>().and {
            get(PendingTestResult::testResultPollingToken).isEqualTo(pendingTestResult.testResultPollingToken)
            get(PendingTestResult::testKit).isEqualTo(LAB_RESULT)
        }
    }

    @Test
    fun `gets empty for result not found`() {
        expectThat(persistence.getTestResult(TestResultPollingToken.of("some token"))).isAbsent()
    }

    @Test
    fun `throws when test result with missing status`() {
        val itemMap = mapOf(
            "testResultPollingToken" to stringAttribute(pendingTestResult.testResultPollingToken),
        )

        val request = PutItemRequest()
            .withTableName(virologyConfig.testResultsTable.value)
            .withItem(itemMap)

        dbClient.putItem(request)

        expectThrows<RuntimeException> { persistence.getTestResult(pendingTestResult.testResultPollingToken) }
            .message.isEqualTo("Required field missing")
    }

    @Test
    fun `creates test order returning test order unmodified`() {
        val persisted = persistence.persistTestOrder({ testOrder }, fourWeeksTtl)

        expectThat(persisted) {
            get(TestOrder::ctaToken).isEqualTo(testOrder.ctaToken)
            get(TestOrder::diagnosisKeySubmissionToken).isEqualTo(testOrder.diagnosisKeySubmissionToken)
            get(TestOrder::testResultPollingToken).isEqualTo(testOrder.testResultPollingToken)
        }
    }

    @Test
    fun `creates test order and initial result`() {
        persistence.persistTestOrder({ testOrder }, fourWeeksTtl)

        expectThat(testOrder)
            .testOrderIsPresent()
            .testResultIsPresent(pendingTestResult)
            .not().hasSubmission()
    }

    @Test
    fun `creating tokens with collision should generate new token`() {
        val tokens = TokensGenerator.generateVirologyTokens()
        val tokensWithCollision = TestOrder(
            tokens.ctaToken,
            TestResultPollingToken.of("testResultPollingToken-collision"),
            DiagnosisKeySubmissionToken.of("diagnosisKeySubmissionToken-collision"),
            LocalDateTime.now().plusWeeks(4)
        )
        val tokensNoCollision = TestOrder(
            TokensGenerator.generateVirologyTokens().ctaToken,
            TestResultPollingToken.of("testResultPollingToken-no-collision"),
            DiagnosisKeySubmissionToken.of("diagnosisKeySubmissionToken-no-collision"),
            LocalDateTime.now().plusWeeks(4)
        )

        val tokensSupplier = mockk<Supplier<TestOrder>>()
        every { tokensSupplier.get() } returns tokensWithCollision andThen tokensWithCollision andThen tokensNoCollision
        persistence.persistTestOrder({ tokens }, fourWeeksTtl)
        expectThat(tokens).testOrderIsPresent().testResultIsPresent(pendingTestResult)

        val persistedTokens = persistence.persistTestOrder(tokensSupplier, fourWeeksTtl)
        expectThat(persistedTokens).testOrderIsPresent().testResultIsPresent(pendingTestResult)
        verify(exactly = 3) { tokensSupplier.get() }
    }

    @Test
    fun `creating tokens with collision should stop after too many retries`() {
        val tokens = TokensGenerator.generateVirologyTokens()
        val tokensWithCollision = TestOrder(
            tokens.ctaToken,
            TestResultPollingToken.of("testResultPollingToken-collision"),
            DiagnosisKeySubmissionToken.of("diagnosisKeySubmissionToken-collision"),
            LocalDateTime.now().plusWeeks(4)
        )

        persistence.persistTestOrder({ tokens }, fourWeeksTtl)

        expectThat(tokens).testOrderIsPresent().testResultIsPresent(pendingTestResult)
        expectCatching { persistence.persistTestOrder({ tokensWithCollision }, fourWeeksTtl) }
            .isFailure()
    }

    @Test
    fun `creates test order and final positive result`() {
        val testResult = testOrder.positiveTestResult()

        persistence.persistTestOrderAndResult(
            { testOrder },
            fourWeeksTtl,
            testResult.testResult,
            testResult.testEndDate,
            testResult.testKit
        )

        expectThat(testOrder)
            .testOrderIsPresent()
            .testResultIsPresent(testResult)
            .submissionIsPresent(LAB_RESULT)
    }

    @Test
    fun `creates test order and final negative result`() {
        val testResult = testOrder.negativeTestResult()

        persistence.persistTestOrderAndResult(
            { testOrder },
            fourWeeksTtl,
            testResult.testResult,
            testResult.testEndDate,
            testResult.testKit
        )

        expectThat(testOrder)
            .testOrderIsPresent()
            .testResultIsPresent(testResult)
            .not().hasSubmission()
    }

    private val virologyDataTimeToLive = VirologyDataTimeToLive(
        Instant.ofEpochSecond(10),
        Instant.ofEpochSecond(20)
    )

    @Test
    fun `marks existing positive test result for deletion`() {
        val testResult = testOrder.positiveTestResult()

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

        expectThat(testOrder)
            .testOrderIsPresent(expireAt = Instant.ofEpochSecond(10))
            .testResultIsPresent(testResult, expireAt = Instant.ofEpochSecond(10))
            .submissionIsPresent(LAB_RESULT, expireAt = Instant.ofEpochSecond(20))
    }

    @Test
    fun `marks existing negative test result for deletion`() {
        val testResult = testOrder.negativeTestResult()

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

        expectThat(testOrder)
            .testOrderIsPresent(expireAt = Instant.ofEpochSecond(10))
            .testResultIsPresent(testResult, expireAt = Instant.ofEpochSecond(10))
            .not().hasSubmission()
    }

    @Test
    fun `mark for deletion throws not allowing re-activating submission token after it is deleted`() {
        val testOrder = persistence.persistTestOrder({ testOrder }, fourWeeksTtl)

        val testResult = this.testOrder.positiveTestResult()

        val positiveResult = VirologyResultRequestV2(
            testOrder.ctaToken,
            testResult.testEndDate,
            testResult.testResult,
            testResult.testKit
        )

        persistence.persistTestResultWithKeySubmission(
            positiveResult,
            fourWeeksTtl
        )

        submissionTable.deleteItem("diagnosisKeySubmissionToken", testOrder.diagnosisKeySubmissionToken.value)

        expectThat(testOrder).not().hasSubmission()
        expectThrows<TransactionException> { persistence.markForDeletion(testResult, virologyDataTimeToLive) }
        expectThat(testOrder).not().hasSubmission()
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

        expectThat(testOrder) {
            not().hasOrder()
            not().hasTestResult()
            not().hasSubmission()
        }
    }

    @Test
    fun `persists positive test result for all test kits`() {
        TestKit.values().forEach {
            val testOrder = TokensGenerator.generateVirologyTokens()
            persistence.persistTestOrder({ testOrder }, fourWeeksTtl)

            val testResult = this.testOrder.positiveTestResult(it)

            val virologyResultRequest = VirologyResultRequestV2(
                testOrder.ctaToken,
                testResult.testEndDate,
                testResult.testResult,
                it
            )
            val result = persistence.persistTestResultWithKeySubmission(
                virologyResultRequest,
                fourWeeksTtl
            )

            expectThat(testOrder)
                .testOrderIsPresent()
                .testResultIsPresent(testResult)
                .submissionIsPresent(it)

            expectThat(result).isA<Success>()
        }
    }

    @Test
    fun `persists positive test result in db`() {
        persistence.persistTestOrder({ testOrder }, fourWeeksTtl)

        val testResult = testOrder.positiveTestResult()

        val virologyResultRequest = VirologyResultRequestV2(
            testOrder.ctaToken,
            testResult.testEndDate,
            testResult.testResult,
            testResult.testKit
        )

        persistence.persistTestResultWithKeySubmission(
            virologyResultRequest,
            fourWeeksTtl
        )

        val result = resultTable.getItem("testResultPollingToken", testOrder.testResultPollingToken.value)
        val resultMap = result.asMap()

        expectThat(resultMap).isEqualTo(
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

        val testResult = this.testOrder.voidTestResult(LAB_RESULT)

        val virologyResultRequest = VirologyResultRequestV2(
            testOrder.ctaToken,
            testResult.testEndDate,
            testResult.testResult,
            testResult.testKit
        )

        val result = persistence.persistTestResultWithoutKeySubmission(
            virologyResultRequest
        )

        expectThat(testOrder)
            .testOrderIsPresent()
            .testResultIsPresent(testResult)
            .not().hasSubmission()

        expectThat(result).isA<Success>()
    }

    @Test
    fun `transaction fails when persisting test result that is already available`() {
        persistence.persistTestOrder({ testOrder }, fourWeeksTtl)

        val positiveTestResult = testOrder.positiveTestResult()
        val positiveResult = VirologyResultRequestV2(
            testOrder.ctaToken,
            positiveTestResult.testEndDate,
            positiveTestResult.testResult,
            positiveTestResult.testKit
        )
        persistence.persistTestResultWithKeySubmission(
            positiveResult,
            fourWeeksTtl
        )

        val negativeTestResult = testOrder.negativeTestResult()
        val negativeResult = VirologyResultRequestV2(
            testOrder.ctaToken,
            negativeTestResult.testEndDate,
            negativeTestResult.testResult,
            negativeTestResult.testKit
        )
        val result = persistence.persistTestResultWithoutKeySubmission(
            negativeResult
        )

        expectThat(result).isA<TransactionFailed>()
    }

    @Test
    fun `order not found when persisting a result for a order that does not exist`() {
        val positiveTestResult = testOrder.positiveTestResult()
        val positiveResult = VirologyResultRequestV2(
            testOrder.ctaToken,
            positiveTestResult.testEndDate,
            positiveTestResult.testResult,
            positiveTestResult.testKit
        )

        persistence.persistTestResultWithKeySubmission(
            positiveResult,
            fourWeeksTtl
        )

        val negativeTestResult = testOrder.negativeTestResult()
        val negativeResult = VirologyResultRequestV2(
            testOrder.ctaToken,
            negativeTestResult.testEndDate,
            negativeTestResult.testResult,
            negativeTestResult.testKit
        )

        val result = persistence.persistTestResultWithoutKeySubmission(
            negativeResult
        )

        expectThat(result).isA<OrderNotFound>()
    }

    @Test
    fun `updates cta exchange positive test result`() {
        val testResult = testOrder.positiveTestResult()

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

        expectThat(testOrder)
            .testOrderIsPresent(expireAt = Instant.ofEpochSecond(10), downloadCount = 1)
            .testResultIsPresent(testState = testResult, expireAt = Instant.ofEpochSecond(10))
            .submissionIsPresent(testKit = LAB_RESULT, expireAt = Instant.ofEpochSecond(20))
    }

    @Test
    fun `updates cta exchange positive test result with missing submission token`() {
        val testResult = testOrder.positiveTestResult()

        persistence.persistTestOrderAndResult(
            { testOrder },
            fourWeeksTtl,
            testResult.testResult,
            testResult.testEndDate,
            testResult.testKit
        )

        dbClient.deleteItem(
            DeleteItemRequest()
                .withTableName(virologyConfig.submissionTokensTable.value)
                .withKey(
                    mapOf("diagnosisKeySubmissionToken" to stringAttribute(testOrder.diagnosisKeySubmissionToken.value))
                )
        )

        persistence.updateOnCtaExchange(
            testOrder,
            testResult,
            virologyDataTimeToLive
        )

        expectThat(testOrder)
            .testOrderIsPresent(expireAt = Instant.ofEpochSecond(10), downloadCount = 1)
            .testResultIsPresent(testResult, expireAt = Instant.ofEpochSecond(10))
            .not().hasSubmission()
    }

    @Test
    fun `updates cta exchange negative test result`() {
        val testResult = testOrder.negativeTestResult()

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

        expectThat(testOrder)
            .testOrderIsPresent(expireAt = Instant.ofEpochSecond(10), downloadCount = 1)
            .testResultIsPresent(testResult, expireAt = Instant.ofEpochSecond(10))
            .not().hasSubmission()
    }

    @Test
    fun `updates cta exchange increments downloadCount field`() {
        val testResult = testOrder.negativeTestResult()

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

        expectThat(testOrder).testOrderIsPresent(expireAt = Instant.ofEpochSecond(10), downloadCount = 2)
    }

    private fun TestOrder.positiveTestResult(testKit: TestKit = LAB_RESULT) =
        AvailableTestResult(
            testResultPollingToken = testResultPollingToken,
            testEndDate = TestEndDate.parse(nowString),
            testResult = Positive,
            testKit = testKit
        )

    private fun TestOrder.negativeTestResult(testKit: TestKit = LAB_RESULT) =
        AvailableTestResult(
            testResultPollingToken = testResultPollingToken,
            testEndDate = TestEndDate.parse(nowString),
            testResult = Negative,
            testKit = testKit
        )

    private fun TestOrder.voidTestResult(testKit: TestKit = LAB_RESULT) =
        AvailableTestResult(
            testResultPollingToken = testResultPollingToken,
            testEndDate = TestEndDate.parse(nowString),
            testResult = Void,
            testKit = testKit
        )

    private fun createOrderTable(config: VirologyConfig) {
        val attributeDefinitions = listOf(
            AttributeDefinition().withAttributeName("ctaToken").withAttributeType("S"),
            AttributeDefinition().withAttributeName("testResultPollingToken").withAttributeType("S")
        )

        val keySchema = listOf(
            KeySchemaElement().withAttributeName("ctaToken").withKeyType(HASH)
        )

        val globalSecondaryIndex = GlobalSecondaryIndex()
            .withIndexName(config.testOrdersIndex.value)
            .withKeySchema(
                listOf(
                    KeySchemaElement().withAttributeName("testResultPollingToken").withKeyType(HASH)
                )
            )
            .withProjection(
                Projection()
                    .withProjectionType(INCLUDE)
                    .withNonKeyAttributes(listOf("diagnosisKeySubmissionToken", "ctaToken"))
            )
            .withProvisionedThroughput(ProvisionedThroughput(100L, 100L))

        val request = CreateTableRequest()
            .withTableName(config.testOrdersTable.value)
            .withKeySchema(keySchema)
            .withAttributeDefinitions(attributeDefinitions)
            .withProvisionedThroughput(ProvisionedThroughput(100L, 100L))
            .withGlobalSecondaryIndexes(globalSecondaryIndex)

        dbClient.createTable(request)
    }

    private fun createResultTable(config: VirologyConfig) {
        val attributeDefinitions = listOf(
            AttributeDefinition().withAttributeName("testResultPollingToken").withAttributeType("S")
        )

        val keySchema = listOf(
            KeySchemaElement().withAttributeName("testResultPollingToken").withKeyType(HASH)
        )

        val request = CreateTableRequest()
            .withTableName(config.testResultsTable.value)
            .withKeySchema(keySchema)
            .withAttributeDefinitions(attributeDefinitions)
            .withProvisionedThroughput(ProvisionedThroughput(100L, 100L))

        dbClient.createTable(request)
    }

    private fun createSubmissionTable(config: VirologyConfig) {
        val attributeDefinitions = listOf(
            AttributeDefinition().withAttributeName("diagnosisKeySubmissionToken").withAttributeType("S")
        )

        val keySchema = listOf(
            KeySchemaElement().withAttributeName("diagnosisKeySubmissionToken").withKeyType(HASH)
        )

        val request = CreateTableRequest()
            .withTableName(config.submissionTokensTable.value)
            .withKeySchema(keySchema)
            .withAttributeDefinitions(attributeDefinitions)
            .withProvisionedThroughput(ProvisionedThroughput(100L, 100L))

        dbClient.createTable(request)
    }

    private fun Assertion.Builder<TestOrder>.hasOrder() =
        get(TestOrder::ctaToken)
            .get("persisted order") { orderTable.getItem("ctaToken", this.value) }
            .isNotNull()

    private fun Assertion.Builder<TestOrder>.hasTestResult() =
        get(TestOrder::testResultPollingToken)
            .get("persisted test result") { resultTable.getItem("testResultPollingToken", this.value) }
            .isNotNull()

    private fun Assertion.Builder<TestOrder>.hasSubmission() =
        get(TestOrder::diagnosisKeySubmissionToken)
            .get("persisted submission") { submissionTable.getItem("diagnosisKeySubmissionToken", this.value) }
            .isNotNull()

    private fun Assertion.Builder<TestOrder>.testOrderIsPresent(
        expireAt: Instant = fourWeeksTtl,
        downloadCount: Int? = null
    ) = assert("test oder is present") { order ->
        val ctaToken = order.ctaToken.value

        val expected = mutableMapOf(
            "ctaToken" to ctaToken,
            "diagnosisKeySubmissionToken" to order.diagnosisKeySubmissionToken.value,
            "testResultPollingToken" to order.testResultPollingToken.value,
            "expireAt" to expireAt.epochSecond.toBigDecimal()
        ).apply {
            if (downloadCount != null) this["downloadCount"] = downloadCount.toBigDecimal()
        }

        hasOrder().get(Item::asMap).isEqualTo(expected.toMap())
    }

    private fun Assertion.Builder<TestOrder>.testResultIsPresent(
        testState: TestState,
        expireAt: Instant = fourWeeksTtl
    ) = assert("test result is present") { order ->
        val testResultPollingToken = order.testResultPollingToken.value
        val expected = when (testState) {
            is AvailableTestResult -> mapOf(
                "testResultPollingToken" to testResultPollingToken,
                "status" to AVAILABLE.text,
                "testEndDate" to TestEndDate.show(testState.testEndDate),
                "testResult" to testState.testResult.wireValue,
                "expireAt" to expireAt.epochSecond.toBigDecimal(),
                "testKit" to testState.testKit.name

            )
            is PendingTestResult -> mapOf(
                "testResultPollingToken" to testResultPollingToken,
                "expireAt" to expireAt.epochSecond.toBigDecimal(),
                "status" to TestResultAvailability.PENDING.text,
            )
        }

        hasTestResult().get(Item::asMap).isEqualTo(expected)
    }

    private fun Assertion.Builder<TestOrder>.submissionIsPresent(
        testKit: TestKit,
        expireAt: Instant = fourWeeksTtl
    ) = assert("submission is present") { order ->
        hasSubmission().get(Item::asMap).isEqualTo(
            mapOf<String, Any>(
                "diagnosisKeySubmissionToken" to order.diagnosisKeySubmissionToken.value,
                "expireAt" to expireAt.epochSecond.toBigDecimal(),
                "testKit" to testKit.name
            )
        )
    }
}

private fun DynamoDB.getTable(name: TableName) = getTable(name.value)
private fun AmazonDynamoDB.deleteTable(name: TableName) = deleteTable(name.value)
