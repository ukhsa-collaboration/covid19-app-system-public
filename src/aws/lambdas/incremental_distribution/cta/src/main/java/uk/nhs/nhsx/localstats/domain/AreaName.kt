package uk.nhs.nhsx.localstats.domain

import dev.forkhandles.values.AbstractComparableValue
import dev.forkhandles.values.NonBlankStringValueFactory

class AreaName private constructor(value: String) : AbstractComparableValue<AreaName, String>(value) {
    companion object : NonBlankStringValueFactory<AreaName>(::AreaName)
}
