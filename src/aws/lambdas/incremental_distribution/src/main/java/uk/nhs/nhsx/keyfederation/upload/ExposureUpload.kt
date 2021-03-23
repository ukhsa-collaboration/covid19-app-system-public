package uk.nhs.nhsx.keyfederation.upload

import uk.nhs.nhsx.keyfederation.download.ReportType
import uk.nhs.nhsx.keyfederation.download.TestType

data class ExposureUpload(val keyData: String?,
                          val rollingStartNumber: Int,
                          val transmissionRiskLevel: Int,
                          val rollingPeriod: Int,
                          val regions: List<String>,
                          val testType: TestType,
                          val reportType: ReportType,
                          val daysSinceOnset: Int)
