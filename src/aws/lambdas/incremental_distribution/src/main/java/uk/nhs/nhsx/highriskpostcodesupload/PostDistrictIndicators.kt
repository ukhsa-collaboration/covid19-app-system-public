package uk.nhs.nhsx.highriskpostcodesupload

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

enum class RiskIndicator(val wireValue: String) {
    HIGH("H"), MEDIUM("M"), LOW("L");

    override fun toString() = wireValue

    companion object {
        fun from(wire: String) = values().first { it.wireValue == wire }
    }
}

class TierIndicator private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<TierIndicator>(::TierIndicator)
}

data class PostDistrictIndicators(
    val riskIndicator: RiskIndicator,
    val tierIndicator: TierIndicator
)
