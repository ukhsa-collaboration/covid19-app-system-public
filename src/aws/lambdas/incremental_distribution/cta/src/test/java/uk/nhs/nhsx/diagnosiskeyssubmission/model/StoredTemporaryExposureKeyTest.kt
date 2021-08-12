package uk.nhs.nhsx.diagnosiskeyssubmission.model

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import uk.nhs.nhsx.testhelper.assertions.isEqualToJson
import uk.nhs.nhsx.testhelper.assertions.toJson

class StoredTemporaryExposureKeyTest {

    @Test
    fun `converts to JSON with daysSinceOnsetOfSymptoms`() {
        expectThat(
            StoredTemporaryExposureKey(
                key = "W2zb3BeMWt6Xr2u0ABG32Q==",
                rollingStartNumber = 5,
                rollingPeriod = 2,
                transmissionRisk = 3,
                daysSinceOnsetOfSymptoms = 5
            )
        ).toJson().isEqualToJson(
            """
            {
              "key": "W2zb3BeMWt6Xr2u0ABG32Q==",
              "rollingStartNumber": 5,
              "rollingPeriod": 2,
              "transmissionRisk": 3,
              "daysSinceOnsetOfSymptoms": 5
            }
            """
        )
    }

    @Test
    fun `converts to JSON without daysSinceOnsetOfSymptoms`() {
        expectThat(
            StoredTemporaryExposureKey(
                "W2zb3BeMWt6Xr2u0ABG32Q==",
                5,
                2,
                3
            )
        ).toJson().isEqualToJson(
            """
            {
              "key": "W2zb3BeMWt6Xr2u0ABG32Q==",
              "rollingStartNumber": 5,
              "rollingPeriod": 2,
              "transmissionRisk": 3
            }
            """
        )
    }
}
