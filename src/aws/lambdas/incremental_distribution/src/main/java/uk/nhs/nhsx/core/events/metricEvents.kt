package uk.nhs.nhsx.core.events

import uk.nhs.nhsx.core.events.EventCategory.Metric

/**
 * These are interesting business events that we might want to track
 */
class VirologyOrder : Event(Metric)
class VirologyRegister : Event(Metric) // to be removed in v2 API
class VirologyResults : Event(Metric)
class VirologyTokenGen : Event(Metric)
class VirologyCtaExchange : Event(Metric)
class DiagnosisKeySubmission : Event(Metric)
class MobileAnalyticsSubmission : Event(Metric)
class CircuitBreakerVenueResolution : Event(Metric)
class CircuitBreakerVenueRequest : Event(Metric)
class CircuitBreakerExposureResolution : Event(Metric)
class CircuitBreakerExposureRequest : Event(Metric)
class RiskyPostDistrictUpload : Event(Metric)
class RiskyVenuesUpload : Event(Metric)
class CtaTokensGenerated(val event: Map<String, String>) : Event(Metric)
