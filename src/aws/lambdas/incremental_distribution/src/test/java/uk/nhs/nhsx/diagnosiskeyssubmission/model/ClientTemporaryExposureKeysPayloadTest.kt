package uk.nhs.nhsx.diagnosiskeyssubmission.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.Jackson

class ClientTemporaryExposureKeysPayloadTest {

    @Test
    fun `can parse null temporaryExposureKeys`() {
        val json = """
        {
            "diagnosisKeySubmissionToken": "dd3aa1bf-4c91-43bb-afb6-12d0b5dcad43",
            "temporaryExposureKeys": [
                null, 
                {
                    "key": "kzQt9Lf3xjtAlMtm7jkSqw==",
                    "rollingStartNumber": 2664864,
                    "rollingPeriod": 144,
                    "transmissionRiskLevel": 4
                }
            ]
        }
        """.trimIndent()

        val payload = Jackson.readOrNull<ClientTemporaryExposureKeysPayload>(json)
        assertThat(payload?.temporaryExposureKeys).hasSize(2)
    }
}
