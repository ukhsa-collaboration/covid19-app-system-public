package uk.nhs.nhsx.domain

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class BatchTag private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<BatchTag>(::BatchTag)
}
