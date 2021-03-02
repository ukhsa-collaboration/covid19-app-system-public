package uk.nhs.nhsx.diagnosiskeyssubmission

import uk.nhs.nhsx.core.events.Event
import uk.nhs.nhsx.core.events.EventCategory.Info
import uk.nhs.nhsx.core.events.EventCategory.Warning
import java.util.*

data class TemporaryExposureKeysSubmissionOverflow(
    val actual: Int,
    val max: Int
) : Event(Warning)

class EmptyTemporaryExposureKeys : Event(Warning)

data class DiagnosisTokenNotFound(val token: UUID) : Event(Warning)

data class DownloadedTemporaryExposureKeys(
    val validKeys: Int,
    val invalidKeys: Int
) : Event(Info)
