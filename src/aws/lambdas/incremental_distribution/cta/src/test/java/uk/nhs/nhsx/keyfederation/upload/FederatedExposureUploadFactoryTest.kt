package uk.nhs.nhsx.keyfederation.upload

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEmpty
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.diagnosiskeydist.Submission
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKey
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKeyPayload
import uk.nhs.nhsx.domain.ReportType.CONFIRMED_TEST
import uk.nhs.nhsx.domain.ReportType.UNKNOWN
import uk.nhs.nhsx.domain.TestType.LAB_RESULT
import uk.nhs.nhsx.domain.TestType.RAPID_RESULT
import uk.nhs.nhsx.domain.TestType.RAPID_SELF_REPORTED
import uk.nhs.nhsx.keyfederation.client.ExposureUpload
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
                        key = "W2zb3BeMWt6Xr2u0ABG32Q==",
                        rollingStartNumber = 5,
                        rollingPeriod = 2,
                        transmissionRisk = 3,
                        daysSinceOnsetOfSymptoms = 10
                    )
                )
            )
        )

        expectThat(fedExposureUploadFactory.create(submission)).containsExactly(
            ExposureUpload(
                keyData = "W2zb3BeMWt6Xr2u0ABG32Q==",
                rollingStartNumber = 5,
                transmissionRiskLevel = 3,
                rollingPeriod = 2,
                regions = listOf("GF"),
                testType = LAB_RESULT,
                reportType = CONFIRMED_TEST,
                daysSinceOnset = 10
            )
        )
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
                        key = "W2zb3BeMWt6Xr2u0ABG32Q==",
                        rollingStartNumber = 5,
                        rollingPeriod = 2,
                        transmissionRisk = 3,
                        daysSinceOnsetOfSymptoms = 10
                    )
                )
            )
        )

        expectThat(fedExposureUploadFactory.create(submission)).containsExactly(
            ExposureUpload(
                keyData = "W2zb3BeMWt6Xr2u0ABG32Q==",
                rollingStartNumber = 5,
                transmissionRiskLevel = 3,
                rollingPeriod = 2,
                regions = listOf("GF"),
                testType = RAPID_RESULT,
                reportType = UNKNOWN,
                daysSinceOnset = 10
            )
        )

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
                        key = "W2zb3BeMWt6Xr2u0ABG32Q==",
                        rollingStartNumber = 5,
                        rollingPeriod = 2,
                        transmissionRisk = 3,
                        daysSinceOnsetOfSymptoms = 10
                    )
                )
            )
        )

        expectThat(fedExposureUploadFactory.create(submission)).containsExactly(
            ExposureUpload(
                keyData = "W2zb3BeMWt6Xr2u0ABG32Q==",
                rollingStartNumber = 5,
                transmissionRiskLevel = 3,
                rollingPeriod = 2,
                regions = listOf("GF"),
                testType = RAPID_SELF_REPORTED,
                reportType = UNKNOWN,
                daysSinceOnset = 10
            )
        )
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
                        key = "W2zb3BeMWt6Xr2u0ABG32Q==",
                        rollingStartNumber = 5,
                        rollingPeriod = 2,
                        transmissionRisk = 3
                    )
                )
            )
        )

        expectThat(pcrExposureUploadFactory.create(submission)).containsExactly(
            ExposureUpload(
                keyData = "W2zb3BeMWt6Xr2u0ABG32Q==",
                rollingStartNumber = 5,
                transmissionRiskLevel = 3,
                rollingPeriod = 2,
                regions = listOf("GF"),
                testType = LAB_RESULT,
                reportType = CONFIRMED_TEST,
                daysSinceOnset = 0
            )
        )
    }

    @Test
    fun `create exposure payload from empty submission`() {
        val fedExposureUploadFactory = FederatedExposureUploadFactory("GF")
        val submission = Submission(
            Instant.EPOCH,
            ObjectKey.of("mobile/LAB_RESULT/abc"),
            StoredTemporaryExposureKeyPayload(emptyList())
        )

        expectThat(fedExposureUploadFactory.create(submission)).isEmpty()
    }

}
