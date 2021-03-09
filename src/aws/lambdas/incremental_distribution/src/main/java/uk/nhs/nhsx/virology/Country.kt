package uk.nhs.nhsx.virology

import dev.forkhandles.values.NonEmptyStringValueFactory
import dev.forkhandles.values.StringValue

class Country private constructor(value: String) : StringValue(value) {
    companion object : NonEmptyStringValueFactory<Country>(::Country) {
        val England = Country.of("England")
        val Wales = Country.of("Wales")
    }
}
