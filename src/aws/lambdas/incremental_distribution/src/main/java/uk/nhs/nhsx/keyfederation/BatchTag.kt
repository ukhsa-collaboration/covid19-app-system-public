package uk.nhs.nhsx.keyfederation

import dev.forkhandles.values.NonEmptyStringValueFactory
import dev.forkhandles.values.StringValue

class BatchTag private constructor(value: String) : StringValue(value) {
    companion object : NonEmptyStringValueFactory<BatchTag>(::BatchTag)
}
