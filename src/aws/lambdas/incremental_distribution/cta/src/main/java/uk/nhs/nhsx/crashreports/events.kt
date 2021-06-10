package uk.nhs.nhsx.crashreports

import uk.nhs.nhsx.core.events.Event
import uk.nhs.nhsx.core.events.EventCategory

data class CrashReportStored(val crashReport: CrashReportRequest) : Event(EventCategory.Info)
data class CrashReportNotRecognised(val value: String) : Event(EventCategory.Warning)

