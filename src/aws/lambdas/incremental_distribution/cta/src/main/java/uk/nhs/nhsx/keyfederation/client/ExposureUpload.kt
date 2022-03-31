package uk.nhs.nhsx.keyfederation.client

import uk.nhs.nhsx.domain.ReportType
import uk.nhs.nhsx.domain.TestType

data class ExposureUpload(
    val keyData: String?,
    val rollingStartNumber: Int,
    val transmissionRiskLevel: Int,
    val rollingPeriod: Int,
    val regions: List<String>,
    val testType: TestType,
    val reportType: ReportType,
    val daysSinceOnset: Int
)
