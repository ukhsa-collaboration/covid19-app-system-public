package uk.nhs.nhsx.localstats.domain

import dev.forkhandles.values.AbstractComparableValue
import dev.forkhandles.values.NonBlankStringValueFactory

class AreaCode private constructor(value: String) : AbstractComparableValue<AreaCode, String>(value) {
    companion object : NonBlankStringValueFactory<AreaCode>(::AreaCode) {
        val ENGLAND = AreaCode.of("E92000001")
        val WALES = AreaCode.of("W92000004")
    }
}
