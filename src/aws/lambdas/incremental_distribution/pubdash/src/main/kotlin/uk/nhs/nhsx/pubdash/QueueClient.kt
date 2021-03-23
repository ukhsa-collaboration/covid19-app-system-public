package uk.nhs.nhsx.pubdash

import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.model.SendMessageRequest
import uk.nhs.nhsx.core.Jackson
import uk.nhs.nhsx.core.events.Events

class QueueClient(
    private val sqsClient: AmazonSQS,
    private val queueUrl: String,
    private val events: Events
) {

    fun sendMessage(queueMessage: QueueMessage) {
        events(SendSqsMessageEvent(queueMessage.queryId, queueMessage.dataset))

        val sendMessageRequest = SendMessageRequest()
            .withMessageBody(Jackson.toJson(queueMessage))
            .withQueueUrl(queueUrl)

        sqsClient.sendMessage(sendMessageRequest)
    }
}
