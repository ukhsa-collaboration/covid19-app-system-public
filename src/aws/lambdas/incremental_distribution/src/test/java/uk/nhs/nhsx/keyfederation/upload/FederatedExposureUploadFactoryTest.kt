package uk.nhs.nhsx.keyfederation.upload

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.diagnosiskeydist.Submission
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKey
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKeyPayload
import uk.nhs.nhsx.domain.ReportType
import uk.nhs.nhsx.domain.TestType
import java.time.Instant

class FederatedExposureUploadFactoryTest {

    @Test
    fun `create exposure payload from submission for LAB_RESULT`() {
        val fedExposureUploadFactory = FederatedExposureUploadFactory("GF")

        val submission = Submission(
            Instant.EPOCH,
            ObjectKey.of("mobile/LAB_RESULT/abc"),
            StoredTemporaryExposureKeyPayload(
                listOf(
                    StoredTemporaryExposureKey(
                        "W2zb3BeMWt6Xr2u0ABG32Q==",
                        5,
                        2,
                        3,
                        10
                    ))))

        assertThat(fedExposureUploadFactory.create(submission), equalTo(
            listOf(ExposureUpload(
                "W2zb3BeMWt6Xr2u0ABG32Q==",
                5,
                3,
                2,
                listOf("GF"),
                TestType.LAB_RESULT,
                ReportType.CONFIRMED_TEST,
                10))))
    }

    @Test
    fun `create exposure payload from submission for RAPID_RESULT`() {
        val fedExposureUploadFactory = FederatedExposureUploadFactory("GF")

        val submission = Submission(
            Instant.EPOCH,
            ObjectKey.of("mobile/RAPID_RESULT/abc.json"),
            StoredTemporaryExposureKeyPayload(
                listOf(
                    StoredTemporaryExposureKey(
                        "W2zb3BeMWt6Xr2u0ABG32Q==",
                        5,
                        2,
                        3,
                        10
                    ))))

        assertThat(fedExposureUploadFactory.create(submission), equalTo(
            listOf(ExposureUpload(
                "W2zb3BeMWt6Xr2u0ABG32Q==",
                5,
                3,
                2,
                listOf("GF"),
                TestType.RAPID_RESULT,
                ReportType.UNKNOWN,
                10))))
    }

    @Test
    fun `create exposure payload from submission for RAPID_SELF_REPORTED`() {
        val fedExposureUploadFactory = FederatedExposureUploadFactory("GF")

        val submission = Submission(
            Instant.EPOCH,
            ObjectKey.of("mobile/RAPID_SELF_REPORTED/abc"),
            StoredTemporaryExposureKeyPayload(
                listOf(
                    StoredTemporaryExposureKey(
                        "W2zb3BeMWt6Xr2u0ABG32Q==",
                        5,
                        2,
                        3,
                        10
                    ))))

        assertThat(fedExposureUploadFactory.create(submission), equalTo(
            listOf(ExposureUpload(
                "W2zb3BeMWt6Xr2u0ABG32Q==",
                5,
                3,
                2,
                listOf("GF"),
                TestType.RAPID_SELF_REPORTED,
                ReportType.UNKNOWN,
                10))))
    }

    @Test
    fun `create exposure payload from submission without DaysSinceOnsetOfSymptoms`() {
        val pcrExposureUploadFactory = FederatedExposureUploadFactory("GF")

        val submission = Submission(
            Instant.EPOCH,
            ObjectKey.of("mobile/LAB_RESULT/abc"),
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
                TestType.LAB_RESULT,
                ReportType.CONFIRMED_TEST,
                0))))
    }

    @Test
    fun `create exposure payload from empty submission`() {
        val fedExposureUploadFactory = FederatedExposureUploadFactory("GF")
        val submission = Submission(
            Instant.EPOCH,
            ObjectKey.of("mobile/LAB_RESULT/abc"),
            StoredTemporaryExposureKeyPayload(emptyList()))

        assertThat(fedExposureUploadFactory.create(submission), equalTo(emptyList()))
    }

}
