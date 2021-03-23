package uk.nhs.nhsx.diagnosiskeyssubmission.model

import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode.STRICT
import uk.nhs.nhsx.core.Jackson.toJson

class ClientTemporaryExposureKeyTest {
    @Test
    fun `marshall to JSON`() {
        assertTheSame(
            ClientTemporaryExposureKey("key", 0, 0), """{"key":"key","rollingStartNumber":0,"rollingPeriod":0,"transmissionRiskLevel":7}"""
        )
        assertTheSame(
            ClientTemporaryExposureKey(null, 0, 0), """{"key":null,"rollingStartNumber":0,"rollingPeriod":0,"transmissionRiskLevel":7}"""
        )
        assertTheSame(
            ClientTemporaryExposureKey(null, 0, 0).apply {
                daysSinceOnsetOfSymptoms = 5
            }, """{"key":null,"rollingStartNumber":0,"rollingPeriod":0,"transmissionRiskLevel":7,"daysSinceOnsetOfSymptoms":5}"""
        )
    }

    private fun assertTheSame(value: ClientTemporaryExposureKey, expected: String) {
        JSONAssert.assertEquals(expected.trimIndent(), toJson(value), STRICT)
    }
}
