package uk.nhs.nhsx.core.aws.ssm

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class ParameterName private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<ParameterName>(::ParameterName)

    fun withPrefix(prefix: String) = of("$prefix/$value")
}
