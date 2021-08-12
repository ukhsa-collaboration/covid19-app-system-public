package uk.nhs.nhsx.keyfederation.upload

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import uk.nhs.nhsx.core.Json
import uk.nhs.nhsx.domain.ReportType.CONFIRMED_TEST
import uk.nhs.nhsx.domain.TestType.LAB_RESULT
import uk.nhs.nhsx.testhelper.assertions.isEqualToJson

class ExposureUploadTest {

    @Test
    fun `serializes to JSON`() {

        val json = Json.toJson(ExposureUpload(
            keyData = "xnGNbiVKd7xarkv9Gbdi5w==",
            rollingStartNumber = 1,
            transmissionRiskLevel = 2,
            rollingPeriod = 3,
            regions = listOf("GB"),
            testType = LAB_RESULT,
            reportType = CONFIRMED_TEST,
            daysSinceOnset = 4
        ))

        expectThat(json).isEqualToJson("""{"keyData":"xnGNbiVKd7xarkv9Gbdi5w==","rollingStartNumber":1,"transmissionRiskLevel":2,"rollingPeriod":3,"regions":["GB"],"testType":1,"reportType":1,"daysSinceOnset":4}""")
    }
}
