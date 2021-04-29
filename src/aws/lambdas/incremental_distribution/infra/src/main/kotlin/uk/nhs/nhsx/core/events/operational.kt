package uk.nhs.nhsx.core.events

import uk.nhs.nhsx.core.events.EventCategory.Error
import uk.nhs.nhsx.core.events.EventCategory.Info
import uk.nhs.nhsx.core.events.EventCategory.Operational
import uk.nhs.nhsx.core.events.EventCategory.Warning
import uk.nhs.nhsx.core.headers.UserAgent

data class IncomingHttpRequest(
    val uri: String,
    val method: String,
    val status: Int,
    val latency: Long,
    val userAgent: UserAgent,
    val requestId: String,
    val apiKey: String,
    val message: String // deprecated - waiting to migrate to stats from the user agent
) : Event(Operational)

data class OutgoingHttpRequest(
    val uri: String,
    val method: String,
    val status: Int
) : Event(
    Operational
)

data class ExceptionThrown<T : Throwable>(
    val exception: T,
    val message: String = exception.localizedMessage ?: "<missing>"
) : Event(Error)

data class ApiHandleFailed constructor(
    val statusCode: Int,
    val message: String?
) : Event(Info)

data class NoRequestIdFound(val uri: String) : Event(Warning)
data class OAINotSet(val method: String?, val path: String?) : Event(Warning)
data class UnprocessableJson(val e: Exception) : Event(Warning)

/**
 * this is a generic info event - it really should not be here but be replaced with
 * more specific events for wanted scenarios
 */
data class InfoEvent(val message: String) : Event(Info)

data class RequestRejected(val reason: String) : Event(Info)
