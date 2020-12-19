package uk.nhs.nhsx.core.aws.dynamodb

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.CancellationReason
import com.amazonaws.services.dynamodbv2.model.TransactionCanceledException
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class DynamoTransactionsTest {

    private val dynamoDbClient = mockk<AmazonDynamoDB>()

    @Test
    fun `throws exception building correct error message`() {
        val reason = CancellationReason()
        reason.message = "reason"
        val reasons = listOf(reason)
        val exception = TransactionCanceledException("message").withCancellationReasons(reasons)

        every { dynamoDbClient.transactWriteItems(any()) } throws exception

        assertThatThrownBy { DynamoTransactions.executeTransaction(dynamoDbClient, emptyList()) }
            .isInstanceOf(RuntimeException::class.java)
            .hasMessage("Transaction cancelled by remote DB service due to: reason")
    }
}