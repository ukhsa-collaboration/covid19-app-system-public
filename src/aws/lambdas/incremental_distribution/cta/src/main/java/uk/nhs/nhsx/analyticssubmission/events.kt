package uk.nhs.nhsx.analyticssubmission

import uk.nhs.nhsx.core.events.Event
import uk.nhs.nhsx.core.events.EventCategory

data class AnalyticsSubmissionUploaded(
    val type: UploadType,
    val destinationName: String
) : Event(EventCategory.Metric)

data class AnalyticsSubmissionRejected(
    val startDate: String,
    val endDate: String,
    val deviceModel: String,
    val appVersion: String,
    val osVersion: String
) : Event(EventCategory.Info)

object AnalyticsSubmissionDisabled : Event(EventCategory.Info)

enum class UploadType {
    S3, Firehose
}

object QueueEntryProcessed : Event(EventCategory.Operational)
