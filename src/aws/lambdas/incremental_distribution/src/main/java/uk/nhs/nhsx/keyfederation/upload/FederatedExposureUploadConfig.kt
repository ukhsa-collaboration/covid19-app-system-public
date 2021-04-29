package uk.nhs.nhsx.keyfederation.upload

import uk.nhs.nhsx.core.ObjectKeyFilters
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.diagnosiskeydist.Submission
import uk.nhs.nhsx.domain.ReportType
import uk.nhs.nhsx.domain.TestType
import java.util.function.Predicate

fun interface ExposureUploadFactory {
    fun create(submission: Submission): List<ExposureUpload>
}

class FederatedExposureUploadFactory(private val region: String) : ExposureUploadFactory {
    override fun create(submission: Submission): List<ExposureUpload> {
        val testType = testType(submission.objectKey.value)
        if (testType != null) {
            return submission.payload.temporaryExposureKeys.map {
                ExposureUpload(
                    keyData = it.key,
                    rollingStartNumber = it.rollingStartNumber,
                    transmissionRiskLevel = it.transmissionRisk,
                    rollingPeriod = it.rollingPeriod,
                    regions = listOf(region),
                    testType = testType,
                    reportType = if (testType == TestType.LAB_RESULT) ReportType.CONFIRMED_TEST else ReportType.UNKNOWN,
                    daysSinceOnset = it.daysSinceOnsetOfSymptoms ?: 0
                )
            }
        }
        return emptyList()
    }

    private fun testType(objectKey:String): TestType? =
        try {
            objectKey.split("/").drop(1).first().let(TestType::valueOf)
        } catch (e: Exception) {
            null
        }
}

object FederatedExposureUploadConfig {
    fun create(region: String, prefixes: List<String>): Pair<Predicate<ObjectKey>, ExposureUploadFactory> = Pair(
        ObjectKeyFilters.federated().withPrefixes(prefixes),
        FederatedExposureUploadFactory(region)
    )
}
