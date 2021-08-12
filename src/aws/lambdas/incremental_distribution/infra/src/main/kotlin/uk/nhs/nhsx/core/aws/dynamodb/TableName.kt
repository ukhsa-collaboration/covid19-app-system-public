package uk.nhs.nhsx.core.aws.dynamodb

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class TableName private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<TableName>(::TableName)
}
