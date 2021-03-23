package uk.nhs.nhsx.virology

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class Country private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<Country>(::Country) {
        val England = Country.of("England")
        val Wales = Country.of("Wales")
    }
}
