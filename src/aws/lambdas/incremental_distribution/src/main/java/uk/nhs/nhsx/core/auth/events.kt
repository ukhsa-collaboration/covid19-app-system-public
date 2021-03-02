package uk.nhs.nhsx.core.auth

import uk.nhs.nhsx.core.events.Event
import uk.nhs.nhsx.core.events.EventCategory.Warning

data class BCryptKeyVerificationFailure(val keyName: String) : Event(Warning)
