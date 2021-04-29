package uk.nhs.nhsx.domain

enum class RiskIndicator(val wireValue: String) {
    HIGH("H"), MEDIUM("M"), LOW("L");

    override fun toString() = wireValue

    companion object {
        fun from(wire: String) = values().first { it.wireValue == wire }
    }
}
