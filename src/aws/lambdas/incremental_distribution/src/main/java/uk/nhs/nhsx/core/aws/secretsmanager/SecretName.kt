package uk.nhs.nhsx.core.aws.secretsmanager

import dev.forkhandles.values.NonEmptyStringValueFactory
import dev.forkhandles.values.StringValue

class SecretName private constructor(value: String) : StringValue(value) {
    companion object : NonEmptyStringValueFactory<SecretName>(::SecretName)
}
