package uk.nhs.nhsx.core.aws.dynamodb

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class IndexName private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<IndexName>(::IndexName)
}
