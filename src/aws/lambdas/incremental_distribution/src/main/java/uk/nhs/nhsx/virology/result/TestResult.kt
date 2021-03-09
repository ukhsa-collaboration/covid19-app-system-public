package uk.nhs.nhsx.virology.result

enum class TestResult(val wireValue: String) {
    Positive("POSITIVE"), Negative("NEGATIVE"), Void("VOID");

    override fun toString() = wireValue

    companion object {
        fun from(wire: String) =
            if (wire == "INDETERMINATE") Void
            else values().first { it.wireValue == wire }
    }
}
