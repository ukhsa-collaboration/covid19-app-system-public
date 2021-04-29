package uk.nhs.nhsx.domain

import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import dev.forkhandles.values.regex

private const val MESSAGE_TYPE_PATTERN = "M[1-2]"

class MessageType(val type: String): StringValue(type) {
    companion object : StringValueFactory<MessageType>(
        ::MessageType,
        MESSAGE_TYPE_PATTERN.regex.withMessage("validation error: Message Type must match $MESSAGE_TYPE_PATTERN")
    )
}
