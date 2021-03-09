package uk.nhs.nhsx.virology

import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import dev.forkhandles.values.regex

class IpcTokenId private constructor(value: String) : StringValue(value) {
    companion object : StringValueFactory<IpcTokenId>(::IpcTokenId, validation = "[0-9A-Za-z]{64}".regex)
}
