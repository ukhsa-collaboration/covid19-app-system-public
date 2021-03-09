package uk.nhs.nhsx.virology

import dev.forkhandles.values.NonEmptyStringValueFactory
import dev.forkhandles.values.StringValue

class TestResultPollingToken private constructor(value: String) : StringValue(value) {
    companion object : NonEmptyStringValueFactory<TestResultPollingToken>(::TestResultPollingToken)
}
