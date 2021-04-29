package uk.nhs.nhsx.domain

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class LocalAuthority private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<LocalAuthority>(::LocalAuthority)
}
