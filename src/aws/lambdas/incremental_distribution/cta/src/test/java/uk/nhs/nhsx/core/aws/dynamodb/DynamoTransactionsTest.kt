package uk.nhs.nhsx.core.aws.dynamodb

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.CancellationReason
import com.amazonaws.services.dynamodbv2.model.TransactionCanceledException
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import strikt.api.expectThrows
import strikt.assertions.isEqualTo
import strikt.assertions.message

class DynamoTransactionsTest {

    private val dynamoDbClient = mockk<AmazonDynamoDB>()

    @Test
    fun `throws exception building correct error message`() {
        val reason = CancellationReason().apply {
            message = "reason"
        }

        val reasons = listOf(reason)

        val exception = TransactionCanceledException("message").withCancellationReasons(reasons)

        every { dynamoDbClient.transactWriteItems(any()) } throws exception

        expectThrows<RuntimeException> { DynamoTransactions.executeTransaction(dynamoDbClient, emptyList()) }
            .message.isEqualTo("Transaction cancelled by remote DB service due to: reason")
    }
}
