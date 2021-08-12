package uk.nhs.nhsx.diagnosiskeyssubmission.model

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.hasSize
import uk.nhs.nhsx.testhelper.assertions.readJsonOrThrow

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

        expectThat(json)
            .readJsonOrThrow<ClientTemporaryExposureKeysPayload>()
            .get(ClientTemporaryExposureKeysPayload::temporaryExposureKeys)
            .hasSize(2)
    }
}
