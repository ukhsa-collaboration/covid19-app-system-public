package uk.nhs.nhsx.keyfederation.upload

import uk.nhs.nhsx.core.ObjectKeyFilters
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.diagnosiskeydist.Submission
import uk.nhs.nhsx.keyfederation.download.ReportType
import uk.nhs.nhsx.keyfederation.download.TestType
import java.util.function.Predicate

fun interface ExposureUploadFactory {
    fun create(submission: Submission): List<ExposureUpload>
}

class PcrExposureUploadFactory(val region: String) : ExposureUploadFactory {
    override fun create(submission: Submission): List<ExposureUpload> = submission.payload.temporaryExposureKeys.map {
        ExposureUpload(
            keyData = it.key,
            rollingStartNumber = it.rollingStartNumber,
            transmissionRiskLevel = it.transmissionRisk,
            rollingPeriod = it.rollingPeriod,
            regions = listOf(region),
            testType = TestType.PCR,
            reportType = ReportType.CONFIRMED_TEST,
            daysSinceOnset = it.daysSinceOnsetOfSymptoms ?: 0
        )
    }

}

object FederatedExposureUploadConfig {
    @JvmStatic
    fun create(region: String, prefixes: List<String>): Pair<Predicate<ObjectKey>, ExposureUploadFactory> = Pair(
        ObjectKeyFilters.federated().withPrefixes(prefixes),
        PcrExposureUploadFactory(region)
    )
}
