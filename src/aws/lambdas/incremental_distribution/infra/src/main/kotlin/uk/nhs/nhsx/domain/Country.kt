package uk.nhs.nhsx.domain

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class Country private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<Country>(::Country) {
        val England = Country.of("England")
        val Wales = Country.of("Wales")
        val UNKNOWN = Country.of("UNKNOWN")

        fun from(country: String): Country {
            return when (country) {
                "England", "Eng" -> {
                    England
                }
                "Wales", "Wls" -> {
                    Wales
                }
                "UNKNOWN" -> {
                    UNKNOWN
                }
                else -> {
                    error("provided an erroneous country code")
                }
            }
        }
    }
}
