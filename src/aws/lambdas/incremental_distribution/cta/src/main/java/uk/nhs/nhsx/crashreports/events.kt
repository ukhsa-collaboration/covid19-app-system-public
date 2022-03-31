package uk.nhs.nhsx.crashreports

import uk.nhs.nhsx.core.events.Event
import uk.nhs.nhsx.core.events.EventCategory.Info
import uk.nhs.nhsx.core.events.EventCategory.Warning

data class CrashReportStored(val crashReport: CrashReportRequest) : Event(Info)
data class CrashReportNotRecognised(val value: String) : Event(Warning)

