package uk.nhs.nhsx.keyfederation.upload

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.diagnosiskeydist.Submission
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKey
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKeyPayload
import uk.nhs.nhsx.keyfederation.download.ReportType
import uk.nhs.nhsx.keyfederation.download.TestType
import java.time.Instant

class PcrExposureUploadFactoryTest {

    @Test
    fun `create exposure payload from submission with DaysSinceOnsetOfSymptoms`() {
        val pcrExposureUploadFactory = PcrExposureUploadFactory("GF")

        val submission = Submission(
            Instant.EPOCH,
            StoredTemporaryExposureKeyPayload(
                listOf(
                    StoredTemporaryExposureKey(
                        "W2zb3BeMWt6Xr2u0ABG32Q==",
                        5,
                        2,
                        3,
                        10
                    ))))

        assertThat(pcrExposureUploadFactory.create(submission), equalTo(
            listOf(ExposureUpload(
                "W2zb3BeMWt6Xr2u0ABG32Q==",
                5,
                3,
                2,
                listOf("GF"),
                TestType.PCR,
                ReportType.CONFIRMED_TEST,
                10))))
    }

    @Test
    fun `create exposure payload from submission without DaysSinceOnsetOfSymptoms`() {
        val pcrExposureUploadFactory = PcrExposureUploadFactory("GF")

        val submission = Submission(
            Instant.EPOCH,
            StoredTemporaryExposureKeyPayload(
                listOf(
                    StoredTemporaryExposureKey(
                        "W2zb3BeMWt6Xr2u0ABG32Q==",
                        5,
                        2,
                        3
                    ))))

        assertThat(pcrExposureUploadFactory.create(submission), equalTo(
            listOf(ExposureUpload(
                "W2zb3BeMWt6Xr2u0ABG32Q==",
                5,
                3,
                2,
                listOf("GF"),
                TestType.PCR,
                ReportType.CONFIRMED_TEST,
                0))))
    }

    @Test
    fun `create exposure payload from empty submission`() {
        val pcrExposureUploadFactory = PcrExposureUploadFactory("GF")
        val submission = Submission(
            Instant.EPOCH,
            StoredTemporaryExposureKeyPayload(emptyList()))

        assertThat(pcrExposureUploadFactory.create(submission), equalTo(emptyList()))
    }

}
