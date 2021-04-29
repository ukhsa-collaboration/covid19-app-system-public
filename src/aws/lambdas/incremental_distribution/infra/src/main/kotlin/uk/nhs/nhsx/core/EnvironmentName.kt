package uk.nhs.nhsx.core

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class EnvironmentName private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<EnvironmentName>(::EnvironmentName)
}
