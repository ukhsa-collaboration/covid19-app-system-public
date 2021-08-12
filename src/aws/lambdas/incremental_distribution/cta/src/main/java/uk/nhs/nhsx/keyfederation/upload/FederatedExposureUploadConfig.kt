package uk.nhs.nhsx.keyfederation.upload

import uk.nhs.nhsx.core.ObjectKeyFilters
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.diagnosiskeydist.Submission
import uk.nhs.nhsx.domain.ReportType.CONFIRMED_TEST
import uk.nhs.nhsx.domain.ReportType.UNKNOWN
import uk.nhs.nhsx.domain.TestType
import uk.nhs.nhsx.domain.TestType.LAB_RESULT

fun interface ExposureUploadFactory {
    fun create(submission: Submission): List<ExposureUpload>
}

class FederatedExposureUploadFactory(private val region: String) : ExposureUploadFactory {
    override fun create(submission: Submission) = when (val testType = submission.objectKey.testType()) {
        null -> emptyList()
        else -> submission.payload.temporaryExposureKeys.map {
            ExposureUpload(
                keyData = it.key,
                rollingStartNumber = it.rollingStartNumber,
                transmissionRiskLevel = it.transmissionRisk,
                rollingPeriod = it.rollingPeriod,
                regions = listOf(region),
                testType = testType,
                reportType = when (testType) {
                    LAB_RESULT -> CONFIRMED_TEST
                    else -> UNKNOWN
                },
                daysSinceOnset = it.daysSinceOnsetOfSymptoms ?: 0
            )
        }
    }

    private fun ObjectKey.testType(): TestType? =
        try {
            value
                .split("/")
                .drop(1)
                .first()
                .let(TestType::valueOf)
        } catch (e: Exception) {
            null
        }
}

object FederatedExposureUploadConfig {
    operator fun invoke(
        region: String,
        prefixes: List<String>
    ) = ObjectKeyFilters.federated().withPrefixes(prefixes) to FederatedExposureUploadFactory(region)
}
