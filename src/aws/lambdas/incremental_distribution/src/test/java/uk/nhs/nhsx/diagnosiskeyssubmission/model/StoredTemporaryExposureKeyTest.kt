package uk.nhs.nhsx.diagnosiskeyssubmission.model

import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert.assertEquals
import org.skyscreamer.jsonassert.JSONCompareMode.STRICT
import uk.nhs.nhsx.core.Jackson

class StoredTemporaryExposureKeyTest {

    @Test
    fun `converts to JSON with daysSinceOnsetOfSymptoms`() {
        val json = Jackson.toJson(
            StoredTemporaryExposureKey(
                "W2zb3BeMWt6Xr2u0ABG32Q==",
                5,
                2,
                3,
                5
            )
        )

        assertEquals(
            """
            {
              "key": "W2zb3BeMWt6Xr2u0ABG32Q==",
              "rollingStartNumber": 5,
              "rollingPeriod": 2,
              "transmissionRisk": 3,
              "daysSinceOnsetOfSymptoms": 5
            }
            """.trimIndent(), json, STRICT
        )
    }

    @Test
    fun `converts to JSON without daysSinceOnsetOfSymptoms`() {
        val json = Jackson.toJson(
            StoredTemporaryExposureKey(
                "W2zb3BeMWt6Xr2u0ABG32Q==",
                5,
                2,
                3
            )
        )

        assertEquals(
            """
            {
              "key": "W2zb3BeMWt6Xr2u0ABG32Q==",
              "rollingStartNumber": 5,
              "rollingPeriod": 2,
              "transmissionRisk": 3
            }
            """.trimIndent(), json, STRICT
        )
    }
}
