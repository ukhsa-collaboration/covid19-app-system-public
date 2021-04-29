package uk.nhs.nhsx.domain

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class TestResultPollingToken private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<TestResultPollingToken>(::TestResultPollingToken)
}
