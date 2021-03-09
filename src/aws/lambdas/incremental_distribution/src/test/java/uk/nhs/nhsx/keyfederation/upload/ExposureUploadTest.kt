package uk.nhs.nhsx.keyfederation.upload

import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert.assertEquals
import org.skyscreamer.jsonassert.JSONCompareMode.STRICT
import uk.nhs.nhsx.core.Jackson
import uk.nhs.nhsx.keyfederation.download.ReportType
import uk.nhs.nhsx.keyfederation.download.TestType

class ExposureUploadTest {

    @Test
    fun `serializes to JSON`() {
        val json = Jackson.toJson(ExposureUpload(
            keyData = "xnGNbiVKd7xarkv9Gbdi5w==",
            rollingStartNumber = 1,
            transmissionRiskLevel = 2,
            rollingPeriod = 3,
            regions = listOf("GB"),
            testType = TestType.PCR,
            reportType = ReportType.CONFIRMED_TEST,
            daysSinceOnset = 4
        ))

        assertEquals("""{"keyData":"xnGNbiVKd7xarkv9Gbdi5w==","rollingStartNumber":1,"transmissionRiskLevel":2,"rollingPeriod":3,"regions":["GB"],"testType":1,"reportType":1,"daysSinceOnset":4}""", json, STRICT)
    }
}
