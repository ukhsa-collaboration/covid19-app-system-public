package uk.nhs.nhsx.core.aws.secretsmanager

import dev.forkhandles.values.Maskers.hidden
import dev.forkhandles.values.NonEmptyStringValueFactory
import dev.forkhandles.values.StringValue

class SecretValue private constructor(value: String) : StringValue(value, hidden()) {
    companion object : NonEmptyStringValueFactory<SecretValue>(::SecretValue)
}
