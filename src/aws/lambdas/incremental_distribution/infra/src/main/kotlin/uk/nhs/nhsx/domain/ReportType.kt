package uk.nhs.nhsx.domain

enum class ReportType(val wireValue: Int) {
    UNKNOWN(0),
    CONFIRMED_TEST(1),
    CONFIRMED_CLINICAL_DIAGNOSIS(2),
    SELF_REPORT(3),
    RECURSIVE(4),
    REVOKED(5);

    override fun toString() = wireValue.toString()

    companion object {
        fun from(wire: Int) =
            values().first { it.wireValue == wire }
    }
}
