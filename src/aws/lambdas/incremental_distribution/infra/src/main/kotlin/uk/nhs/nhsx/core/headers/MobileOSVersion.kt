package uk.nhs.nhsx.core.headers

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class MobileOSVersion private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<MobileOSVersion>(::MobileOSVersion)
}
