package uk.nhs.nhsx.diagnosiskeyssubmission.model

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import uk.nhs.nhsx.testhelper.assertions.isEqualToJson
import uk.nhs.nhsx.testhelper.assertions.toJson

class ClientTemporaryExposureKeyTest {

    @Test
    fun `marshall to JSON`() {
        expectThat(ClientTemporaryExposureKey("key", 0, 0))
            .toJson()
            .isEqualToJson("""{"key":"key","rollingStartNumber":0,"rollingPeriod":0,"transmissionRiskLevel":7}""")

        expectThat(ClientTemporaryExposureKey(null, 0, 0))
            .toJson()
            .isEqualToJson("""{"key":null,"rollingStartNumber":0,"rollingPeriod":0,"transmissionRiskLevel":7}""")

        expectThat(ClientTemporaryExposureKey(null, 0, 0).apply { daysSinceOnsetOfSymptoms = 5 })
            .toJson()
            .isEqualToJson("""{"key":null,"rollingStartNumber":0,"rollingPeriod":0,"transmissionRiskLevel":7,"daysSinceOnsetOfSymptoms":5}""")

        expectThat(ClientTemporaryExposureKey(null, 0, 0).apply { daysSinceOnsetOfSymptoms = 5 })
            .toJson()
            .isEqualToJson("""{"key":null,"rollingStartNumber":0,"rollingPeriod":0,"transmissionRiskLevel":7,"daysSinceOnsetOfSymptoms":5}""")
    }
}
