package uk.nhs.nhsx.core.aws.sns

import com.amazonaws.services.sns.model.PublishResult

interface AwsSns {
    fun publishMessage(
        topicArn: String,
        message: String,
        attributes: Map<String, MessageAttribute>,
        subject: String? = null
    ): PublishResult
}

sealed class MessageAttribute(val value: String, val type: String)
class NumericAttribute(value: Int) : MessageAttribute("$value", "Number")
