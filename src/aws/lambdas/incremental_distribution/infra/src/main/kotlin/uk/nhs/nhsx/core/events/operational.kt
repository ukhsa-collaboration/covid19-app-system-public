package uk.nhs.nhsx.core.events

import uk.nhs.nhsx.core.events.EventCategory.*
import uk.nhs.nhsx.core.headers.MobileAppVersion
import uk.nhs.nhsx.core.headers.MobileOS
import uk.nhs.nhsx.core.headers.UserAgent
import uk.nhs.nhsx.domain.Country
import uk.nhs.nhsx.domain.TestKit
import uk.nhs.nhsx.domain.TokenAgeRange

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

data class IncomingHttpResponse(
    val status: Int,
    val body: String
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
data class SuccessfulCtaExchange(val ctaToken: String, val country: Country, val testKit: TestKit, val mobileOS: MobileOS, val tokenAgeRange: TokenAgeRange, val appVersion: MobileAppVersion) : Event(Info)
