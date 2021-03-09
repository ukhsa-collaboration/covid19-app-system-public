package uk.nhs.nhsx.keyfederation.download

data class ExposureDownload(val keyData: String,
                            val rollingStartNumber: Int,
                            val transmissionRiskLevel: Int,
                            val rollingPeriod: Int,
                            val origin: String,
                            val regions: List<String>,
                            val testType: TestType,
                            val reportType: ReportType,
                            val daysSinceOnset: Int)

enum class TestType(val wireValue: Int) {
    PCR(1),
    LFT(2),
    SELF_ASSISTED_LFT(3);

    override fun toString() = wireValue.toString()

    companion object {
        fun from(wire: Int) =
            values().first { it.wireValue == wire }
    }
}

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
