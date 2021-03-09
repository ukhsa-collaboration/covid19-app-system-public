package uk.nhs.nhsx.keyfederation

import uk.nhs.nhsx.core.events.Event
import uk.nhs.nhsx.core.events.EventCategory.Info
import uk.nhs.nhsx.core.events.EventCategory.Warning
import uk.nhs.nhsx.keyfederation.download.TestType
import java.time.Instant

data class UploadedDiagnosisKeys(
    val testType: TestType,
    val insertedExposures: Int,
    val latestSubmissionTime: Instant,
    val batchNumber: Int
) : Event(Info)

data class DiagnosisKeysUploadIncomplete(
    val totalExposures: Int,
    val insertedExposures: Int,
    val latestSubmissionTime: Instant,
    val batchNumber: Int
) : Event(Warning)

data class DownloadedExposures(
    val downloadedExposures: Int,
    val batchTag: String,
    val batchNumber: Int
) : Event(Info)

data class DownloadedFederatedDiagnosisKeys(
    val testType: TestType,
    val validKeys: Int,
    val invalidKeys: Int,
    val origin: String
) : Event(Info)

data class InvalidOriginKeys(
    val origin: String,
    val batchTag: String
) : Event(Warning)

data class InvalidTemporaryExposureKey(val key: String?) : Event(Info)
data class InvalidRollingPeriod(val isRollingPeriod: Int) : Event(Info)
data class InvalidTransmissionRiskLevel(val transmissionRiskLevel: Int) : Event(Info)

data class InvalidRollingStartNumber(
    val now: Instant,
    val rollingStartNumber: Long,
    val rollingPeriod: Int
) : Event(Info)
