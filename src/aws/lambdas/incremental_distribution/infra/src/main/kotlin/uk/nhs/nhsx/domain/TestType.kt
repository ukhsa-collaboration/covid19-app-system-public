package uk.nhs.nhsx.domain

enum class TestType(val wireValue: Int) {
    LAB_RESULT(1),
    RAPID_RESULT(2),
    RAPID_SELF_REPORTED(3);

    override fun toString() = wireValue.toString()
  
    companion object {
        fun from(wire: Int) =
            values().first { it.wireValue == wire }
    }
}
