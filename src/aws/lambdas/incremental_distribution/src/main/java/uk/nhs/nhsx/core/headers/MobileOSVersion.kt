package uk.nhs.nhsx.core.headers

import dev.forkhandles.values.NonEmptyStringValueFactory
import dev.forkhandles.values.StringValue

class MobileOSVersion private constructor(value: String) : StringValue(value) {
    companion object : NonEmptyStringValueFactory<MobileOSVersion>(::MobileOSVersion)
}
