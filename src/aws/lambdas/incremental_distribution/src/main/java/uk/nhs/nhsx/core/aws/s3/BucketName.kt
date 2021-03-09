package uk.nhs.nhsx.core.aws.s3

import dev.forkhandles.values.NonEmptyStringValueFactory
import dev.forkhandles.values.StringValue

class BucketName private constructor(value: String) : StringValue(value) {
    companion object : NonEmptyStringValueFactory<BucketName>(::BucketName)
}
