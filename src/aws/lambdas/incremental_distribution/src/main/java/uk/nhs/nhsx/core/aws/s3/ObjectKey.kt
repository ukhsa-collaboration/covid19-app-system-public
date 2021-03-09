package uk.nhs.nhsx.core.aws.s3

import dev.forkhandles.values.NonEmptyStringValueFactory
import dev.forkhandles.values.StringValue

class ObjectKey private constructor(value: String) : StringValue(value) {
    fun append(suffix: String) = of(value + suffix)

    companion object : NonEmptyStringValueFactory<ObjectKey>(::ObjectKey)
}
