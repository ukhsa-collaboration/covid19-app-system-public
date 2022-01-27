package uk.nhs.nhsx.localstats.domain

import dev.forkhandles.values.AbstractComparableValue
import dev.forkhandles.values.NonBlankStringValueFactory

class MetricName private constructor(value: String) : AbstractComparableValue<MetricName, String>(value) {
    companion object : NonBlankStringValueFactory<MetricName>(::MetricName)
}
