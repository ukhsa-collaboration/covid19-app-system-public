package uk.nhs.nhsx.core.aws.sns

import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.AmazonSNSClientBuilder
import com.amazonaws.services.sns.model.MessageAttributeValue
import com.amazonaws.services.sns.model.PublishRequest
import com.amazonaws.services.sns.model.PublishResult
import uk.nhs.nhsx.core.events.Event
import uk.nhs.nhsx.core.events.EventCategory
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.ExceptionThrown
import java.lang.Exception


class AwsSnsClient (val events : Events) : AwsSns{
    private val awsSnsClient : AmazonSNS = AmazonSNSClientBuilder.defaultClient()

    override fun publishMessage(
        topicArn: String,
        message: String,
        attributes: Map<String, MessageAttribute>,
        subject: String?
    ) : PublishResult {
        try {
            val publishRequest = PublishRequest()
                .withTopicArn(topicArn)
                .withMessage(message)
                .withMessageAttributes(attributes.map { it.key to it.value.let { attribute -> MessageAttributeValue().withDataType(attribute.type).withStringValue(attribute.value) }}.toMap())

            if(subject !=null) publishRequest.subject = subject

            val publishResult =  awsSnsClient.publish(publishRequest)

            events(MessagePublishedToSnsTopic(publishResult.messageId, message))

            return publishResult

        } catch (e: Exception) {
            events(ExceptionThrown(e, "Publishing to sns topic $topicArn failed"))
            throw e
        }

    }
}
class MessagePublishedToSnsTopic(val messageId: String, val message: String) : Event(EventCategory.Info)

