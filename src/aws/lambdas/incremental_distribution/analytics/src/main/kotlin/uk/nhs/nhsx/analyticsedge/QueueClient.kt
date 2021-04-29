package uk.nhs.nhsx.analyticsedge

import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.model.SendMessageRequest
import uk.nhs.nhsx.core.Json
import uk.nhs.nhsx.core.events.Events

class QueueClient(
    private val sqsClient: AmazonSQS,
    private val queueUrl: String,
    private val events: Events
) {

    fun sendMessage(queueMessage: QueueMessage) {
        events(SendSqsMessageEvent(queueMessage.queryId, queueMessage.dataset))

        val sendMessageRequest = SendMessageRequest()
            .withMessageBody(Json.toJson(queueMessage))
            .withQueueUrl(queueUrl)

        sqsClient.sendMessage(sendMessageRequest)
    }
}
