package uk.nhs.nhsx.core

import dev.forkhandles.values.NonEmptyStringValueFactory
import dev.forkhandles.values.StringValue

class EnvironmentName private constructor(value: String) : StringValue(value) {
    companion object : NonEmptyStringValueFactory<EnvironmentName>(::EnvironmentName)
}
