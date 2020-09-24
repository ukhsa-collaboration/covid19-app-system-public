package db.virology

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.amazonaws.services.dynamodbv2.document.Item
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import uk.nhs.nhsx.core.aws.xray.Tracing
import uk.nhs.nhsx.core.exceptions.TransactionException
import uk.nhs.nhsx.virology.TestResultPollingToken
import uk.nhs.nhsx.virology.VirologyConfig
import uk.nhs.nhsx.virology.order.TokensGenerator
import uk.nhs.nhsx.virology.persistence.TestOrder
import uk.nhs.nhsx.virology.persistence.VirologyDataTimeToLiveCalculator
import uk.nhs.nhsx.virology.persistence.VirologyDynamoService
import uk.nhs.nhsx.virology.persistence.VirologyResultPersistOperation
import uk.nhs.nhsx.virology.result.VirologyResultRequest
import java.time.Instant
import java.util.*
import java.util.function.Supplier

class VirologyDynamoServiceManualTest {

    init {
        Tracing.disableXRayComplaintsForMainClasses()
    }

    private val targetEnv = "450c66"

    private val virologyConfig = VirologyConfig(
        "$targetEnv-virology-ordertokens",
        "$targetEnv-virology-testresults",
        "$targetEnv-virology-submissiontokens",
        "$targetEnv-virology-ordertokens-index",
        VirologyConfig.MAX_TOKEN_PERSISTENCE_RETRY_COUNT
    )

    private val tokensGenerator = TokensGenerator()
    private val expireAt = 1_000_000L
    private val dynamoDbClient = AmazonDynamoDBClientBuilder.defaultClient()
    private val dynamoDb = DynamoDB(dynamoDbClient)
    private val service = VirologyDynamoService(dynamoDbClient, virologyConfig)
    private val testEndDate = "2020-04-23T18:34:03Z"

    @Test
    fun `get test order that does not exist`() {
        val ctaToken = tokensGenerator.generateVirologyTokens().ctaToken
        assertThat(service.getTestOrder(ctaToken)).isEmpty
    }

    @Test
    fun `get test result that does not exist`() {
        val testResultPollingToken = TestResultPollingToken.of(UUID.randomUUID().toString())
        assertThat(service.getTestResult(testResultPollingToken)).isEmpty
    }

    @Test
    fun `create test order`() {
        val testOrder = service.persistTestOrder({ tokensGenerator.generateVirologyTokens() }, expireAt)

        assertTestOrderInDb(testOrder)
        assertPendingTestResultInDb(testOrder)
        assertNoSubmissionTokenInDb(testOrder)
    }

    @Test
    fun `create test order and final result`() {
        val testOrder = service.persistTestOrderAndResult(
            { tokensGenerator.generateVirologyTokens() },
            expireAt,
            "POSITIVE",
            testEndDate
        )

        assertTestOrderInDb(testOrder)
        assertAvailableTestResultInDb(testOrder)
        assertSubmissionTokenInDb(testOrder, expireAt)
    }

    @Test
    fun `update positive test result`() {
        val testOrder = service.persistTestOrder({ tokensGenerator.generateVirologyTokens() }, expireAt)
        val virologyResultRequest = VirologyResultRequest(testOrder.ctaToken.value, testEndDate, "POSITIVE")
        service.persistPositiveTestResult(VirologyResultRequest.Positive.from(virologyResultRequest), expireAt)

        assertTestOrderInDb(testOrder)
        assertAvailableTestResultInDb(testOrder)
        assertSubmissionTokenInDb(testOrder, expireAt)
    }

    @Test
    fun `update negative test result`() {
        val testOrder = service.persistTestOrder({ tokensGenerator.generateVirologyTokens() }, expireAt)
        val virologyResultRequest = VirologyResultRequest(testOrder.ctaToken.value, testEndDate, "NEGATIVE")
        service.persistNonPositiveTestResult(VirologyResultRequest.NonPositive.from(virologyResultRequest))

        assertTestOrderInDb(testOrder)
        assertAvailableTestResultInDb(testOrder, "NEGATIVE")
        assertNoSubmissionTokenInDb(testOrder)
    }

    @Test
    fun `update void test result`() {
        val testOrder = service.persistTestOrder({ tokensGenerator.generateVirologyTokens() }, expireAt)
        val virologyResultRequest = VirologyResultRequest(testOrder.ctaToken.value, testEndDate, "VOID")
        service.persistNonPositiveTestResult(VirologyResultRequest.NonPositive.from(virologyResultRequest))

        assertTestOrderInDb(testOrder)
        assertAvailableTestResultInDb(testOrder, "VOID")
        assertNoSubmissionTokenInDb(testOrder)
    }

    @Test
    fun `throws when updating test result from positive to negative`() {
        val testOrder = service.persistTestOrder({ tokensGenerator.generateVirologyTokens() }, expireAt)
        service.persistPositiveTestResult(
            VirologyResultRequest.Positive.from(
                VirologyResultRequest(testOrder.ctaToken.value, testEndDate, "POSITIVE")
            ), expireAt)

        val result = service.persistNonPositiveTestResult(
            VirologyResultRequest.NonPositive.from(
                VirologyResultRequest(testOrder.ctaToken.value, testEndDate, "NEGATIVE")
            )
        )

        assertThat(result)
            .isInstanceOf(VirologyResultPersistOperation.TransactionFailed::class.java)
    }

    @Test
    fun `throws when updating test result from negative to positive`() {
        val testOrder = service.persistTestOrder({ tokensGenerator.generateVirologyTokens() }, expireAt)

        service.persistNonPositiveTestResult(
            VirologyResultRequest.NonPositive.from(
                VirologyResultRequest(testOrder.ctaToken.value, testEndDate, "NEGATIVE")
            )
        )

        val result = service.persistPositiveTestResult(
            VirologyResultRequest.Positive.from(
                VirologyResultRequest(testOrder.ctaToken.value, testEndDate, "POSITIVE")
            ), expireAt)

        assertThat(result).isInstanceOf(VirologyResultPersistOperation.TransactionFailed::class.java)
    }

    @Test
    fun `persist tokens with collision should generate new token set for persistence`() {
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
        service.persistTestOrder({ tokens }, expireAt)
        assertTestOrderInDb(tokens)
        assertPendingTestResultInDb(tokens)

        val persistedTokens = service.persistTestOrder(tokensSupplier, expireAt)
        assertTestOrderInDb(persistedTokens)
        assertPendingTestResultInDb(persistedTokens)
        verify(exactly = 3) { tokensSupplier.get() }
    }

    @Test
    fun `persist tokens with collision should stop after too many retries`() {
        val tokens = TokensGenerator().generateVirologyTokens()
        val tokensWithCollision = TestOrder(
            tokens.ctaToken.value,
            "testResultPollingToken-collision",
            "diagnosisKeySubmissionToken-collision"
        )
        service.persistTestOrder({ tokens }, expireAt)
        assertTestOrderInDb(tokens)
        assertPendingTestResultInDb(tokens)
        assertThatThrownBy { service.persistTestOrder({ tokensWithCollision }, expireAt) }
            .isInstanceOf(RuntimeException::class.java)
    }

    @Test
    fun `mark for deletion should not re-activate submission token after it is deleted`() {
        val testOrder = service.persistTestOrder({ tokensGenerator.generateVirologyTokens() }, expireAt)
        service.persistPositiveTestResult(VirologyResultRequest.Positive.from(VirologyResultRequest(testOrder.ctaToken.value, "", "POSITIVE")), expireAt)
        val testResult = service.getTestResult(testOrder.testResultPollingToken)

        dynamoDb.getTable(virologyConfig.submissionTokensTable).deleteItem("diagnosisKeySubmissionToken", testOrder.diagnosisKeySubmissionToken.value)
        assertNoSubmissionTokenInDb(testOrder)

        assertThatThrownBy {
            service.markForDeletion(testResult.get(), VirologyDataTimeToLiveCalculator.CTA_EXCHANGE_TTL.apply { Instant.now() })
        }.isInstanceOf(TransactionException::class.java)

        assertNoSubmissionTokenInDb(testOrder)

    }

    private fun assertTestOrderInDb(testOrder: TestOrder) {
        val ctaToken = testOrder.ctaToken.value
        val testResultPollingToken = testOrder.testResultPollingToken.value
        val diagnosisKeySubmissionToken = testOrder.diagnosisKeySubmissionToken.value

        val item = dynamoDb
            .getTable(virologyConfig.testOrdersTable)
            .getItem("ctaToken", ctaToken)

        assertThat(item.getString("ctaToken")).isEqualTo(ctaToken)
        assertThat(item.getString("diagnosisKeySubmissionToken")).isEqualTo(diagnosisKeySubmissionToken)
        assertThat(item.getString("testResultPollingToken")).isEqualTo(testResultPollingToken)
        assertThat(item.getString("expireAt")).isEqualTo(expireAt.toString())

        val testOrderMaybe = service.getTestOrder(testOrder.ctaToken)
        assertThat(testOrderMaybe).isNotEmpty
        assertThat(testOrderMaybe.get().ctaToken).isEqualTo(testOrder.ctaToken)
        assertThat(testOrderMaybe.get().testResultPollingToken).isEqualTo(testOrder.testResultPollingToken)
        assertThat(testOrderMaybe.get().diagnosisKeySubmissionToken).isEqualTo(testOrder.diagnosisKeySubmissionToken)
    }

    private fun assertPendingTestResultInDb(testOrder: TestOrder) {
        val item = dynamoDb
            .getTable(virologyConfig.testResultsTable)
            .getItem("testResultPollingToken", testOrder.testResultPollingToken.value)

        assertThat(item.getString("testResultPollingToken")).isEqualTo(testOrder.testResultPollingToken.value)
        assertThat(item.getString("status")).isEqualTo("pending")
        assertThat(item.getString("expireAt")).isEqualTo(expireAt.toString())
        assertThat(item.getString("testEndDate")).isNull()
        assertThat(item.getString("testResult")).isNull()

        val testResult = service.getTestResult(testOrder.testResultPollingToken)
        assertThat(testResult).isNotEmpty
        assertThat(testResult.get().testResultPollingToken).isEqualTo(testOrder.testResultPollingToken.value)
        assertThat(testResult.get().status).isEqualTo("pending")
        assertThat(testResult.get().testEndDate).isEmpty()
        assertThat(testResult.get().testResult).isEmpty()
    }

    private fun assertAvailableTestResultInDb(testOrder: TestOrder, testResult: String = "POSITIVE") {
        val item = dynamoDb
            .getTable(virologyConfig.testResultsTable)
            .getItem("testResultPollingToken", testOrder.testResultPollingToken.value)

        assertThat(item.getString("testResultPollingToken")).isEqualTo(testOrder.testResultPollingToken.value)
        assertThat(item.getString("status")).isEqualTo("available")
        assertThat(item.getString("expireAt")).isEqualTo(expireAt.toString())
        assertThat(item.getString("testEndDate")).isEqualTo(testEndDate)
        assertThat(item.getString("testResult")).isEqualTo(testResult)

        val testResultMaybe = service.getTestResult(testOrder.testResultPollingToken)
        assertThat(testResultMaybe).isNotEmpty
        assertThat(testResultMaybe.get().testResultPollingToken).isEqualTo(testOrder.testResultPollingToken.value)
        assertThat(testResultMaybe.get().status).isEqualTo("available")
        assertThat(testResultMaybe.get().testEndDate).isEqualTo(testEndDate)
        assertThat(testResultMaybe.get().testResult).isEqualTo(testResult)
    }

    private fun assertNoTestOrderInDb(testOrder: TestOrder) {
        val item = dynamoDb.getTable(virologyConfig.testOrdersTable)
            .getItem("ctaToken", testOrder.ctaToken.value)
        assertThat(item).isNull()
    }

    private fun assertNoSubmissionTokenInDb(testOrder: TestOrder) {
        val item = dynamoDb.getTable(virologyConfig.submissionTokensTable)
            .getItem("diagnosisKeySubmissionToken", testOrder.diagnosisKeySubmissionToken.value)
        assertThat(item).isNull()
    }

    private fun assertSubmissionTokenInDb(testOrder: TestOrder, expireAt: Long) {
        val item = dynamoDb.getTable(virologyConfig.submissionTokensTable)
            .getItem("diagnosisKeySubmissionToken", testOrder.diagnosisKeySubmissionToken.value)

        assertThat(item)
            .isEqualTo(
                Item()
                    .with("diagnosisKeySubmissionToken", testOrder.diagnosisKeySubmissionToken.value)
                    .with("expireAt", expireAt)
            )
    }
}