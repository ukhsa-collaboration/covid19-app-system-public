package uk.nhs.nhsx.testkitorder

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.amazonaws.services.dynamodbv2.document.Item
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Ignore
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import uk.nhs.nhsx.testkitorder.order.TokensGenerator
import uk.nhs.nhsx.testkitorder.order.TokensGenerator.TestOrderTokens
import java.util.*
import java.util.function.Supplier

@Ignore("dependency on dynamo db should only be run manually")
class TestKitOrderDynamoPersistenceServiceManualTest {

    private val config =
        TestKitOrderConfig(
            "te-ci-virology-ordertokens",
            "te-ci-virology-testresults",
            "te-ci-virology-submissiontokens",
            "some-order-website",
            "some-register-website",
            3
        )

    private val expireAt = 1_000_000L
    private val dynamoDbClient = AmazonDynamoDBClientBuilder.defaultClient()
    private val dynamoDb = DynamoDB(dynamoDbClient)
    private val service = TestKitOrderDynamoService(
        AmazonDynamoDBClientBuilder.defaultClient(),
        config
    )

    @Test
    fun `get test result that is available`() {
        val testResultPollingToken = TestResultPollingToken.of(UUID.randomUUID().toString())
        dynamoDb
            .getTable(config.testResultsTable)
            .putItem(
                Item()
                    .withPrimaryKey("testResultPollingToken", testResultPollingToken.value)
                    .with("status", "available")
                    .with("testResult", "foo-bar")
                    .with("testEndDate", "2020-04-23T00:00:00Z")
            )
        assertThat(service.getTestResult(testResultPollingToken)).isNotEmpty.map { it.testResult }.hasValue("foo-bar")
    }

    @Test
    fun `get test result that is pending`() {
        val testResultPollingToken = TestResultPollingToken.of(UUID.randomUUID().toString())
        dynamoDb.getTable(config.testResultsTable)
            .putItem(
                Item()
                    .withPrimaryKey("testResultPollingToken", testResultPollingToken.value)
                    .with("status", "pending")
            )
        assertThat(service.getTestResult(testResultPollingToken)).isNotEmpty.map { it.status }.hasValue("pending")
    }

    @Test
    fun `get test result that does not exist`() {
        val testResultPollingToken = TestResultPollingToken.of(UUID.randomUUID().toString())
        assertThat(service.getTestResult(testResultPollingToken)).isEmpty
    }

    @Test
    fun `persist tokens of new order for test kit`() {
        val tokens = TokensGenerator().generateTokens()
        service.persistTestOrder({ tokens }, expireAt)
        assertTokensInDb(tokens)
    }

    @Test
    fun `persist tokens with collision should generate new token set for persistence`() {
        val tokens = TokensGenerator().generateTokens()
        val tokensWithCollision = TestOrderTokens(
            tokens.ctaToken,
            "testResultPollingToken-collision",
            "diagnosisKeySubmissionToken-collision"
        )
        val tokensNoCollision = TestOrderTokens(
            tokens.ctaToken + "-" + UUID.randomUUID().toString(),
            "testResultPollingToken-no-collision",
            "diagnosisKeySubmissionToken-no-collision"
        )
        val tokensSupplier = Mockito.mock(Supplier::class.java)
        `when`(tokensSupplier.get())
            .thenReturn(tokensWithCollision)
            .thenReturn(tokensWithCollision)
            .thenReturn(tokensNoCollision)
        service.persistTestOrder({ tokens }, expireAt)
        assertTokensInDb(tokens)

        val persistedTokens = service.persistTestOrder(tokensSupplier as Supplier<TestOrderTokens>, expireAt)
        assertTokensInDb(persistedTokens)
        verify(tokensSupplier, Mockito.times(3)).get()
    }

    @Test
    fun `persist tokens with collision should stop after too many retries`() {
        val tokens = TokensGenerator().generateTokens()
        val tokensWithCollision = TestOrderTokens(
            tokens.ctaToken,
            "testResultPollingToken-collision",
            "diagnosisKeySubmissionToken-collision"
        )
        service.persistTestOrder({ tokens }, expireAt)
        assertTokensInDb(tokens)
        assertThatThrownBy { service.persistTestOrder({ tokensWithCollision }, expireAt) }
            .isInstanceOf(RuntimeException::class.java)
    }

    @Test
    fun `persist tokens should save to db for single retry config`() {
        val service = TestKitOrderDynamoService(AmazonDynamoDBClientBuilder.defaultClient(), config)
        val tokens = TokensGenerator().generateTokens()
        service.persistTestOrder({ tokens }, expireAt)
        assertTokensInDb(tokens)
    }

    private fun assertTokensInDb(tokens: TestOrderTokens) {
        val testResultItem = dynamoDb
            .getTable(config.testResultsTable)
            .getItem("testResultPollingToken", tokens.testResultPollingToken)
        assertThat(testResultItem.getString("status")).isEqualTo("pending")
        assertThat(testResultItem.getString("expireAt")).isEqualTo(expireAt.toString())
        assertThat(testResultItem.getString("testEndDate")).isNull()
        assertThat(testResultItem.getString("testResult")).isNull()

        val testOrderItem = dynamoDb
            .getTable(config.testOrdersTable)
            .getItem("ctaToken", tokens.ctaToken)
        assertThat(testOrderItem.getString("ctaToken")).isEqualTo(tokens.ctaToken)
        assertThat(testOrderItem.getString("diagnosisKeySubmissionToken")).isEqualTo(tokens.diagnosisKeySubmissionToken)
        assertThat(testOrderItem.getString("testResultPollingToken")).isEqualTo(tokens.testResultPollingToken)
        assertThat(testOrderItem.getString("expireAt")).isEqualTo(expireAt.toString())


        val testResult = service.getTestResult(TestResultPollingToken.of(tokens.testResultPollingToken))
        assertThat(testResult).isNotEmpty.map { it.status }.hasValue("pending")
    }
}