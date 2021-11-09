package uk.nhs.nhsx.virology.persistence

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.GetItemRequest
import com.amazonaws.services.dynamodbv2.model.Put
import com.amazonaws.services.dynamodbv2.model.QueryRequest
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest
import com.amazonaws.services.dynamodbv2.model.TransactionCanceledException
import com.amazonaws.services.dynamodbv2.model.Update
import uk.nhs.nhsx.core.aws.dynamodb.DynamoAttributes.attributeMap
import uk.nhs.nhsx.core.aws.dynamodb.DynamoAttributes.itemIntegerValueOrNull
import uk.nhs.nhsx.core.aws.dynamodb.DynamoAttributes.itemLongValueOrThrow
import uk.nhs.nhsx.core.aws.dynamodb.DynamoAttributes.itemValueOrNull
import uk.nhs.nhsx.core.aws.dynamodb.DynamoAttributes.itemValueOrThrow
import uk.nhs.nhsx.core.aws.dynamodb.DynamoAttributes.numericAttribute
import uk.nhs.nhsx.core.aws.dynamodb.DynamoAttributes.stringAttribute
import uk.nhs.nhsx.core.aws.dynamodb.DynamoTransactions.executeTransaction
import uk.nhs.nhsx.core.aws.dynamodb.DynamoTransactions.reasons
import uk.nhs.nhsx.core.aws.dynamodb.withIndexName
import uk.nhs.nhsx.core.aws.dynamodb.withTableName
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.InfoEvent
import uk.nhs.nhsx.core.exceptions.TransactionException
import uk.nhs.nhsx.domain.CtaToken
import uk.nhs.nhsx.domain.DiagnosisKeySubmissionToken
import uk.nhs.nhsx.domain.TestEndDate
import uk.nhs.nhsx.domain.TestKit
import uk.nhs.nhsx.domain.TestKit.LAB_RESULT
import uk.nhs.nhsx.domain.TestResult
import uk.nhs.nhsx.domain.TestResult.Positive
import uk.nhs.nhsx.domain.TestResultPollingToken
import uk.nhs.nhsx.virology.CtaUpdateOnExchangeFailure
import uk.nhs.nhsx.virology.TestResultMarkForDeletionFailure
import uk.nhs.nhsx.virology.TestResultPersistenceFailure
import uk.nhs.nhsx.virology.VirologyConfig
import uk.nhs.nhsx.virology.VirologyOrderNotFound
import uk.nhs.nhsx.virology.persistence.TestResultAvailability.AVAILABLE
import uk.nhs.nhsx.virology.persistence.TestResultAvailability.PENDING
import uk.nhs.nhsx.virology.persistence.TestState.AvailableTestResult
import uk.nhs.nhsx.virology.persistence.TestState.PendingTestResult
import uk.nhs.nhsx.virology.persistence.VirologyResultPersistOperation.OrderNotFound
import uk.nhs.nhsx.virology.persistence.VirologyResultPersistOperation.TransactionFailed
import uk.nhs.nhsx.virology.result.VirologyResultRequestV2
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset.UTC

class VirologyPersistenceService(
    private val dynamoDbClient: AmazonDynamoDB,
    private val config: VirologyConfig,
    private val events: Events
) {
    fun getTestResult(pollingToken: TestResultPollingToken): TestState? {
        val request = GetItemRequest()
            .withTableName(config.testResultsTable)
            .withKey(attributeMap("testResultPollingToken", pollingToken))

        return dynamoDbClient.getItem(request)?.item?.let {
            val testKit = itemValueOrNull(it, "testKit")?.let(TestKit::valueOf) ?: LAB_RESULT
            val testResultPollingToken = itemValueOrThrow(it, "testResultPollingToken")
                .let(TestResultPollingToken::of)

            when (itemValueOrThrow(it, "status")) {
                AVAILABLE.text -> {
                    val testEndDate = itemValueOrThrow(it, "testEndDate").let(TestEndDate::parse)
                    val testResult = itemValueOrThrow(it, "testResult").let(TestResult::from)
                    AvailableTestResult(
                        testResultPollingToken,
                        testEndDate,
                        testResult,
                        testKit
                    )
                }
                else -> PendingTestResult(testResultPollingToken, testKit)
            }
        }
    }

    fun getTestOrder(ctaToken: CtaToken): TestOrder? {
        val request = GetItemRequest()
            .withTableName(config.testOrdersTable)
            .withKey(attributeMap("ctaToken", ctaToken))

        return dynamoDbClient.getItem(request)?.item?.let {
            TestOrder(
                itemValueOrThrow(it, "ctaToken").let(CtaToken::of),
                itemIntegerValueOrNull(it, "downloadCount") ?: 0,
                itemValueOrThrow(it, "testResultPollingToken").let(TestResultPollingToken::of),
                itemValueOrThrow(it, "diagnosisKeySubmissionToken").let(DiagnosisKeySubmissionToken::of),
                itemLongValueOrThrow(it, "expireAt").let { e -> LocalDateTime.ofEpochSecond(e, 0, UTC) }
            )
        }
    }

    fun persistTestOrder(
        testOrderFn: () -> TestOrder,
        expireAt: Instant
    ) = persistTestOrderTransactItems(testOrderFn) { testOrder ->
        listOf(
            testOrderCreateOp(testOrder, expireAt),
            testResultPendingCreateOp(testOrder, expireAt)
        )
    }

    fun persistTestOrderAndResult(
        testOrderFn: () -> TestOrder,
        expireAt: Instant,
        testResult: TestResult,
        testEndDate: TestEndDate,
        testKit: TestKit
    ) = persistTestOrderTransactItems(testOrderFn) { testOrder ->
        when (testResult) {
            Positive -> listOf(
                testOrderCreateOp(testOrder, expireAt),
                testResultAvailableCreateOp(testOrder, expireAt, testResult, testEndDate, testKit),
                submissionTokenCreateOp(testOrder, expireAt, testKit)
            )
            else -> listOf(
                testOrderCreateOp(testOrder, expireAt),
                testResultAvailableCreateOp(testOrder, expireAt, testResult, testEndDate, testKit)
            )
        }
    }

    private fun persistTestOrderTransactItems(
        testOrderFn: () -> TestOrder,
        transactWriteItems: (TestOrder) -> List<TransactWriteItem>
    ): TestOrder {
        var numberOfTries = 0
        do {
            try {
                val testOrder = testOrderFn()
                val transactItems = transactWriteItems(testOrder)
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

    fun markForDeletion(
        testResult: AvailableTestResult,
        virologyTimeToLive: VirologyDataTimeToLive
    ) {
        val testOrder = queryTestOrderFor(testResult)
        if (testOrder != null) markTestResultForDeletion(testResult, virologyTimeToLive, testOrder)
        else events(TestResultMarkForDeletionFailure(testResult.testResultPollingToken, ""))
    }

    private fun markTestResultForDeletion(
        testResult: AvailableTestResult,
        virologyTimeToLive: VirologyDataTimeToLive,
        testOrder: TestOrder
    ) {
        val testDataExpireAt = virologyTimeToLive.testDataExpireAt
        when {
            testResult.isPositive() -> executeTransaction(
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
            else -> executeTransaction(
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
        when {
            testResult.isPositive() && submissionTokenPresent(testOrder) -> executeTransaction(
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
            else -> executeTransaction(
                dynamoDbClient,
                listOf(
                    testOrderTtlAndCounterUpdateOp(testOrder.ctaToken, virologyTimeToLive.testDataExpireAt),
                    testResultTtlUpdateOp(testOrder.testResultPollingToken, virologyTimeToLive.testDataExpireAt)
                )
            )
        }
    }

    private fun submissionTokenPresent(testOrder: TestOrder) = dynamoDbClient.getItem(
        config.submissionTokensTable.value,
        attributeMap("diagnosisKeySubmissionToken", testOrder.diagnosisKeySubmissionToken)
    ).item != null

    private fun queryTestOrderFor(testState: TestState): TestOrder? {
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

        return dynamoDbClient.query(request)
            ?.items
            ?.firstOrNull()
            ?.let {
                TestOrder(
                    itemValueOrThrow(it, "ctaToken").let(CtaToken::of),
                    itemValueOrThrow(it, "testResultPollingToken").let(TestResultPollingToken::of),
                    itemValueOrThrow(it, "diagnosisKeySubmissionToken").let(DiagnosisKeySubmissionToken::of),
                    LocalDateTime.now() // placeholder, index does not include expireAt
                )
            }
    }

    private fun testOrderCreateOp(
        testOrder: TestOrder,
        expireAt: Instant
    ) = TransactWriteItem().withPut(
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

    private fun testResultPendingCreateOp(
        testOrder: TestOrder,
        expireAt: Instant
    ) = TransactWriteItem().withPut(
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
    ) = TransactWriteItem().withPut(
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

    private fun testOrderTtlUpdateOp(
        ctaToken: CtaToken,
        testDataExpireAt: Instant
    ) = TransactWriteItem().withUpdate(
        Update()
            .withTableName(config.testOrdersTable)
            .withKey(attributeMap("ctaToken", ctaToken))
            .withUpdateExpression("set expireAt = :expireAt")
            .withExpressionAttributeValues(attributeMap(":expireAt", testDataExpireAt.epochSecond))
    )

    private fun testOrderTtlAndCounterUpdateOp(
        ctaToken: CtaToken,
        testDataExpireAt: Instant
    ) = TransactWriteItem().withUpdate(
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
    ) = TransactWriteItem().withUpdate(
        Update()
            .withTableName(config.testResultsTable)
            .withKey(attributeMap("testResultPollingToken", testResultPollingToken))
            .withUpdateExpression("set expireAt = :expireAt")
            .withExpressionAttributeValues(attributeMap(":expireAt", testDataExpireAt.epochSecond))
    )

    private fun submissionTokenTimeToLiveUpdateOp(
        diagnosisKeySubmissionToken: DiagnosisKeySubmissionToken,
        submissionDataExpireAt: Instant
    ) = TransactWriteItem().withUpdate(
        Update()
            .withTableName(config.submissionTokensTable)
            .withKey(attributeMap("diagnosisKeySubmissionToken", diagnosisKeySubmissionToken))
            .withConditionExpression("attribute_exists(diagnosisKeySubmissionToken)")
            .withUpdateExpression("set expireAt = :expireAt")
            .withExpressionAttributeValues(attributeMap(":expireAt", submissionDataExpireAt.epochSecond))
    )

    fun persistTestResultWithKeySubmission(
        testResult: VirologyResultRequestV2,
        expireAt: Instant
    ): VirologyResultPersistOperation = persistTestResult(testResult) { testOrder ->
        listOf(
            resultPollingTokenUpdateOp(testOrder, testResult),
            submissionTokenCreateOp(testOrder, expireAt, testResult.testKit)
        )
    }

    fun persistTestResultWithoutKeySubmission(testResult: VirologyResultRequestV2): VirologyResultPersistOperation =
        persistTestResult(testResult) { testOrder ->
            listOf(
                resultPollingTokenUpdateOp(testOrder, testResult)
            )
        }

    private fun persistTestResult(
        testResult: VirologyResultRequestV2,
        transactWriteItems: (TestOrder) -> List<TransactWriteItem>
    ) = getOrder(testResult.ctaToken)
        ?.let { testOrder ->
            try {
                executeTransaction(dynamoDbClient, transactWriteItems(testOrder))
                VirologyResultPersistOperation.Success()
            } catch (e: TransactionException) {
                events(TestResultPersistenceFailure(testOrder.ctaToken, e))
                TransactionFailed()
            }
        }
        ?: run {
            events(VirologyOrderNotFound(testResult.ctaToken))
            OrderNotFound()
        }

    private fun getOrder(ctaToken: CtaToken): TestOrder? {
        val request = GetItemRequest()
            .withTableName(config.testOrdersTable)
            .withKey(attributeMap("ctaToken", ctaToken.value))

        return dynamoDbClient.getItem(request)
            ?.item
            ?.let {
                TestOrder(
                    itemValueOrThrow(it, "ctaToken").let(CtaToken::of),
                    itemValueOrThrow(it, "testResultPollingToken").let(TestResultPollingToken::of),
                    itemValueOrThrow(it, "diagnosisKeySubmissionToken").let(DiagnosisKeySubmissionToken::of),
                    itemLongValueOrThrow(it, "expireAt").let { e -> LocalDateTime.ofEpochSecond(e, 0, UTC) }
                )
            }
    }

    private fun resultPollingTokenUpdateOp(
        testOrder: TestOrder,
        testResult: VirologyResultRequestV2
    ) = TransactWriteItem().withUpdate(
        Update()
            .withTableName(config.testResultsTable)
            .withKey(attributeMap("testResultPollingToken", testOrder.testResultPollingToken))
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

    private fun submissionTokenCreateOp(
        testOrder: TestOrder,
        expireAt: Instant,
        testKit: TestKit
    ) = TransactWriteItem().withPut(
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
