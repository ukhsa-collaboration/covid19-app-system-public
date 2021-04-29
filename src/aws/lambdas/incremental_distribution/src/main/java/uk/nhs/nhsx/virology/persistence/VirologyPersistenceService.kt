package uk.nhs.nhsx.virology.persistence

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.*
import uk.nhs.nhsx.core.aws.dynamodb.DynamoAttributes.attributeMap
import uk.nhs.nhsx.core.aws.dynamodb.DynamoAttributes.itemIntegerValueOrNull
import uk.nhs.nhsx.core.aws.dynamodb.DynamoAttributes.itemValueOrNull
import uk.nhs.nhsx.core.aws.dynamodb.DynamoAttributes.itemValueOrThrow
import uk.nhs.nhsx.core.aws.dynamodb.DynamoAttributes.numericAttribute
import uk.nhs.nhsx.core.aws.dynamodb.DynamoAttributes.stringAttribute
import uk.nhs.nhsx.core.aws.dynamodb.DynamoTransactions.executeTransaction
import uk.nhs.nhsx.core.aws.dynamodb.DynamoTransactions.reasons
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.InfoEvent
import uk.nhs.nhsx.core.exceptions.TransactionException
import uk.nhs.nhsx.domain.*
import uk.nhs.nhsx.domain.TestKit.LAB_RESULT
import uk.nhs.nhsx.domain.TestResult.Positive
import uk.nhs.nhsx.virology.*
import uk.nhs.nhsx.virology.persistence.TestResultAvailability.AVAILABLE
import uk.nhs.nhsx.virology.persistence.TestResultAvailability.PENDING
import uk.nhs.nhsx.virology.persistence.TestState.AvailableTestResult
import uk.nhs.nhsx.virology.persistence.TestState.PendingTestResult
import uk.nhs.nhsx.virology.persistence.VirologyResultPersistOperation.OrderNotFound
import uk.nhs.nhsx.virology.persistence.VirologyResultPersistOperation.TransactionFailed
import uk.nhs.nhsx.virology.result.VirologyResultRequestV2
import java.time.Instant
import java.util.*
import java.util.function.Function
import java.util.function.Supplier

class VirologyPersistenceService(
    private val dynamoDbClient: AmazonDynamoDB,
    private val config: VirologyConfig,
    private val events: Events
) {
    fun getTestResult(pollingToken: TestResultPollingToken): Optional<TestState> {
        val itemResult = dynamoDbClient.getItem(
            GetItemRequest(
                config.testResultsTable,
                attributeMap("testResultPollingToken", pollingToken)
            )
        )
        return Optional.ofNullable(itemResult.item)
            .map {
                val status = itemValueOrThrow(it, "status")
                if (AVAILABLE.text == status) {
                    AvailableTestResult(
                        TestResultPollingToken.of(itemValueOrThrow(it, "testResultPollingToken")),
                        itemValueOrThrow(it, "testEndDate").let(TestEndDate.Companion::parse),
                        TestResult.from(itemValueOrThrow(it, "testResult")),
                        itemValueOrNull(it, "testKit")?.let { TestKit.valueOf(it) } ?: LAB_RESULT
                    )
                } else {
                    PendingTestResult(
                        TestResultPollingToken.of(itemValueOrThrow(it, "testResultPollingToken")),
                        itemValueOrNull(it, "testKit")?.let { TestKit.valueOf(it) } ?: LAB_RESULT
                    )
                }
            }
    }

    fun getTestOrder(ctaToken: CtaToken): Optional<TestOrder> {
        val itemResult = dynamoDbClient.getItem(
            GetItemRequest(
                config.testOrdersTable,
                attributeMap("ctaToken", ctaToken)
            )
        )

        return Optional.ofNullable(itemResult.item)
            .map {
                TestOrder(
                    itemValueOrThrow(it, "ctaToken").let(CtaToken::of),
                    itemIntegerValueOrNull(it, "downloadCount") ?: 0,
                    itemValueOrThrow(it, "testResultPollingToken").let(TestResultPollingToken.Companion::of),
                    itemValueOrThrow(it, "diagnosisKeySubmissionToken").let(DiagnosisKeySubmissionToken.Companion::of)
                )
            }
    }

    fun persistTestOrder(
        testOrderSupplier: Supplier<TestOrder>,
        expireAt: Instant
    ): TestOrder = persistTestOrderTransactItems(
        testOrderSupplier,
        { testOrder: TestOrder ->
            listOf(
                testOrderCreateOp(testOrder, expireAt),
                testResultPendingCreateOp(testOrder, expireAt)
            )
        }
    )

    fun persistTestOrderAndResult(
        testOrderSupplier: Supplier<TestOrder>,
        expireAt: Instant,
        testResult: TestResult,
        testEndDate: TestEndDate,
        testKit: TestKit
    ): TestOrder = persistTestOrderTransactItems(
        testOrderSupplier,
        { testOrder: TestOrder ->
            if (Positive == testResult) {
                listOf(
                    testOrderCreateOp(testOrder, expireAt),
                    testResultAvailableCreateOp(testOrder, expireAt, testResult, testEndDate, testKit),
                    submissionTokenCreateOp(testOrder, expireAt, testKit)
                )
            } else
                listOf(
                    testOrderCreateOp(testOrder, expireAt),
                    testResultAvailableCreateOp(testOrder, expireAt, testResult, testEndDate, testKit)
                )
        }
    )

    private fun persistTestOrderTransactItems(
        testOrderSupplier: Supplier<TestOrder>,
        transactWriteItems: Function<TestOrder, List<TransactWriteItem>>
    ): TestOrder {
        var numberOfTries = 0
        do {
            try {
                val testOrder = testOrderSupplier.get()
                val transactItems = transactWriteItems.apply(testOrder)
                dynamoDbClient.transactWriteItems(TransactWriteItemsRequest().withTransactItems(transactItems))
                return testOrder
            } catch (e: TransactionCanceledException) {
                events(
                    InfoEvent(
                        "Persistence of test order was cancelled by remote DB service due to " + reasons(e)
                    )
                )
                numberOfTries++
            }
        } while (numberOfTries < config.maxTokenPersistenceRetryCount)

        throw RuntimeException("""Persistence of test order exceeded maximum of ${config.maxTokenPersistenceRetryCount} retries""")
    }

    fun markForDeletion(testResult: AvailableTestResult, virologyTimeToLive: VirologyDataTimeToLive) {
        queryTestOrderFor(testResult)
            .ifPresentOrElse(
                { testOrder: TestOrder -> markTestResultForDeletion(testResult, virologyTimeToLive, testOrder) }
            ) { events(TestResultMarkForDeletionFailure(testResult.testResultPollingToken, "")) }
    }

    private fun markTestResultForDeletion(
        testResult: AvailableTestResult,
        virologyTimeToLive: VirologyDataTimeToLive,
        testOrder: TestOrder
    ) {
        val testDataExpireAt = virologyTimeToLive.testDataExpireAt
        if (testResult.isPositive()) {
            executeTransaction(
                dynamoDbClient,
                listOf(
                    testOrderTtlUpdateOp(testOrder.ctaToken, testDataExpireAt),
                    testResultTtlUpdateOp(testOrder.testResultPollingToken, testDataExpireAt),
                    submissionTokenTimeToLiveUpdateOp(
                        testOrder.diagnosisKeySubmissionToken,
                        virologyTimeToLive.submissionDataExpireAt
                    )
                )
            )
        } else {
            executeTransaction(
                dynamoDbClient,
                listOf(
                    testOrderTtlUpdateOp(testOrder.ctaToken, testDataExpireAt),
                    testResultTtlUpdateOp(testOrder.testResultPollingToken, testDataExpireAt)
                )
            )
        }
    }

    fun updateOnCtaExchange(
        testOrder: TestOrder,
        testResult: AvailableTestResult,
        virologyTimeToLive: VirologyDataTimeToLive
    ) {
        try {
            updateCtaExchangeTimeToLiveAndCounter(testOrder, testResult, virologyTimeToLive)
        } catch (e: TransactionException) {
            events(
                CtaUpdateOnExchangeFailure(
                    testOrder.ctaToken,
                    testOrder.testResultPollingToken,
                    testOrder.diagnosisKeySubmissionToken
                )
            )
        }
    }

    private fun updateCtaExchangeTimeToLiveAndCounter(
        testOrder: TestOrder,
        testResult: AvailableTestResult,
        virologyTimeToLive: VirologyDataTimeToLive
    ) {
        if (testResult.isPositive() && submissionTokenPresent(testOrder)) {
            executeTransaction(
                dynamoDbClient,
                listOf(
                    testOrderTtlAndCounterUpdateOp(testOrder.ctaToken, virologyTimeToLive.testDataExpireAt),
                    testResultTtlUpdateOp(testOrder.testResultPollingToken, virologyTimeToLive.testDataExpireAt),
                    submissionTokenTimeToLiveUpdateOp(
                        testOrder.diagnosisKeySubmissionToken,
                        virologyTimeToLive.submissionDataExpireAt
                    )
                )
            )
        } else {
            executeTransaction(
                dynamoDbClient,
                listOf(
                    testOrderTtlAndCounterUpdateOp(testOrder.ctaToken, virologyTimeToLive.testDataExpireAt),
                    testResultTtlUpdateOp(testOrder.testResultPollingToken, virologyTimeToLive.testDataExpireAt)
                )
            )
        }
    }

    private fun submissionTokenPresent(testOrder: TestOrder): Boolean = dynamoDbClient.getItem(
        config.submissionTokensTable,
        attributeMap("diagnosisKeySubmissionToken", testOrder.diagnosisKeySubmissionToken)
    ).item != null

    private fun queryTestOrderFor(testState: TestState): Optional<TestOrder> {
        val request = QueryRequest()
            .withTableName(config.testOrdersTable)
            .withIndexName(config.testOrdersIndex)
            .withKeyConditionExpression("testResultPollingToken = :pollingToken")
            .withExpressionAttributeValues(
                attributeMap(
                    ":pollingToken",
                    testState.testResultPollingToken
                )
            )

        return dynamoDbClient.query(request).items.stream().findFirst()
            .map {
                TestOrder(
                    itemValueOrThrow(it, "ctaToken").let(CtaToken::of),
                    itemValueOrThrow(it, "testResultPollingToken").let(TestResultPollingToken::of),
                    itemValueOrThrow(it, "diagnosisKeySubmissionToken").let(DiagnosisKeySubmissionToken::of)
                )
            }
    }

    private fun testOrderCreateOp(testOrder: TestOrder, expireAt: Instant): TransactWriteItem =
        TransactWriteItem().withPut(
            Put()
                .withTableName(config.testOrdersTable)
                .withItem(
                    mapOf(
                        "ctaToken" to stringAttribute(testOrder.ctaToken),
                        "testResultPollingToken" to stringAttribute(testOrder.testResultPollingToken),
                        "diagnosisKeySubmissionToken" to stringAttribute(testOrder.diagnosisKeySubmissionToken),
                        "expireAt" to numericAttribute(expireAt)
                    )
                )
                .withConditionExpression("attribute_not_exists(ctaToken)")
        )

    private fun testResultPendingCreateOp(testOrder: TestOrder, expireAt: Instant): TransactWriteItem =
        TransactWriteItem().withPut(
            Put()
                .withTableName(config.testResultsTable)
                .withItem(
                    mapOf(
                        "testResultPollingToken" to stringAttribute(testOrder.testResultPollingToken),
                        "status" to stringAttribute(PENDING.text),
                        "expireAt" to numericAttribute(expireAt)
                    )
                )
        )

    private fun testResultAvailableCreateOp(
        testOrder: TestOrder,
        expireAt: Instant,
        testResult: TestResult,
        testEndDate: TestEndDate,
        testKit: TestKit
    ): TransactWriteItem = TransactWriteItem().withPut(
        Put()
            .withTableName(config.testResultsTable)
            .withItem(
                mapOf(
                    "testResultPollingToken" to stringAttribute(testOrder.testResultPollingToken),
                    "status" to stringAttribute(AVAILABLE.text),
                    "testResult" to stringAttribute(testResult.wireValue),
                    "testEndDate" to stringAttribute(TestEndDate.show(testEndDate)),
                    "expireAt" to numericAttribute(expireAt),
                    "testKit" to stringAttribute(testKit.name)
                )
            )
    )

    private fun testOrderTtlUpdateOp(ctaToken: CtaToken, testDataExpireAt: Instant): TransactWriteItem =
        TransactWriteItem().withUpdate(
            Update()
                .withTableName(config.testOrdersTable)
                .withKey(attributeMap("ctaToken", ctaToken))
                .withUpdateExpression("set expireAt = :expireAt")
                .withExpressionAttributeValues(attributeMap(":expireAt", testDataExpireAt.epochSecond))
        )

    private fun testOrderTtlAndCounterUpdateOp(ctaToken: CtaToken, testDataExpireAt: Instant): TransactWriteItem =
        TransactWriteItem().withUpdate(
            Update()
                .withTableName(config.testOrdersTable)
                .withKey(attributeMap("ctaToken", ctaToken))
                .withUpdateExpression("set expireAt = :expireAt add downloadCount :dc")
                .withExpressionAttributeValues(
                    mapOf(
                        ":expireAt" to numericAttribute(testDataExpireAt),
                        ":dc" to numericAttribute(1)
                    )
                )
        )

    private fun testResultTtlUpdateOp(
        testResultPollingToken: TestResultPollingToken,
        testDataExpireAt: Instant
    ): TransactWriteItem = TransactWriteItem().withUpdate(
        Update()
            .withTableName(config.testResultsTable)
            .withKey(attributeMap("testResultPollingToken", testResultPollingToken))
            .withUpdateExpression("set expireAt = :expireAt")
            .withExpressionAttributeValues(attributeMap(":expireAt", testDataExpireAt.epochSecond))
    )

    private fun submissionTokenTimeToLiveUpdateOp(
        diagnosisKeySubmissionToken: DiagnosisKeySubmissionToken,
        submissionDataExpireAt: Instant
    ): TransactWriteItem = TransactWriteItem().withUpdate(
        Update()
            .withTableName(config.submissionTokensTable)
            .withKey(attributeMap("diagnosisKeySubmissionToken", diagnosisKeySubmissionToken))
            .withConditionExpression("attribute_exists(diagnosisKeySubmissionToken)")
            .withUpdateExpression("set expireAt = :expireAt")
            .withExpressionAttributeValues(attributeMap(":expireAt", submissionDataExpireAt.epochSecond))
    )

    fun persistPositiveTestResult(
        testResult: VirologyResultRequestV2,
        expireAt: Instant
    ): VirologyResultPersistOperation = persistTestResult(
        testResult
    ) { order: TestOrder ->
        listOf(
            resultPollingTokenUpdateOp(order, testResult),
            submissionTokenCreateOp(order, expireAt, testResult.testKit)
        )
    }

    fun persistNonPositiveTestResult(testResult: VirologyResultRequestV2): VirologyResultPersistOperation =
        persistTestResult(testResult) { order: TestOrder -> listOf(resultPollingTokenUpdateOp(order, testResult)) }

    private fun persistTestResult(
        testResult: VirologyResultRequestV2,
        transactWriteItems: Function<TestOrder, List<TransactWriteItem>>
    ): VirologyResultPersistOperation = getOrder(testResult.ctaToken)
        .map { order: TestOrder ->
            try {
                executeTransaction(dynamoDbClient, transactWriteItems.apply(order))
                VirologyResultPersistOperation.Success()
            } catch (e: TransactionException) {
                events(TestResultPersistenceFailure(order.ctaToken, e))
                TransactionFailed()
            }
        }
        .orElseGet {
            events(VirologyOrderNotFound(testResult.ctaToken))
            OrderNotFound()
        }

    private fun getOrder(ctaToken: CtaToken): Optional<TestOrder> {
        val itemResult = dynamoDbClient.getItem(config.testOrdersTable, attributeMap("ctaToken", ctaToken.value))
        return Optional.ofNullable(itemResult.item)
            .map {
                TestOrder(
                    itemValueOrThrow(it, "ctaToken").let(CtaToken::of),
                    itemValueOrThrow(it, "testResultPollingToken").let(TestResultPollingToken::of),
                    itemValueOrThrow(it, "diagnosisKeySubmissionToken").let(DiagnosisKeySubmissionToken::of)
                )
            }
    }

    private fun resultPollingTokenUpdateOp(
        testOrder: TestOrder,
        testResult: VirologyResultRequestV2
    ): TransactWriteItem = TransactWriteItem().withUpdate(
        Update()
            .withTableName(config.testResultsTable)
            .withKey(
                attributeMap(
                    "testResultPollingToken",
                    testOrder.testResultPollingToken
                )
            )
            .withUpdateExpression("set #s = :status, testEndDate = :testEndDate, testResult = :testResult, testKit = :testKit")
            .withConditionExpression("#s = :pendingStatus")
            .withExpressionAttributeValues(
                mapOf(
                    ":status" to stringAttribute(AVAILABLE.text),
                    ":testEndDate" to stringAttribute(TestEndDate.show(testResult.testEndDate)),
                    ":testResult" to stringAttribute(testResult.testResult.wireValue),
                    ":pendingStatus" to stringAttribute(PENDING.text),
                    ":testKit" to stringAttribute(testResult.testKit.name)
                )
            )
            .withExpressionAttributeNames(mapOf("#s" to "status"))
    )

    private fun submissionTokenCreateOp(testOrder: TestOrder, expireAt: Instant, testKit: TestKit): TransactWriteItem =
        TransactWriteItem().withPut(
            Put()
                .withTableName(config.submissionTokensTable)
                .withItem(
                    mapOf(
                        "diagnosisKeySubmissionToken" to stringAttribute(testOrder.diagnosisKeySubmissionToken),
                        "testKit" to stringAttribute(testKit.name),
                        "expireAt" to numericAttribute(expireAt)
                    )
                )
        )
}
