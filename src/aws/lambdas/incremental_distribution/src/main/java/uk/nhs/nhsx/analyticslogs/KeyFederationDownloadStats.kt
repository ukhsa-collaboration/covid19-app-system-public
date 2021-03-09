package uk.nhs.nhsx.analyticslogs

import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.SystemClock.CLOCK
import uk.nhs.nhsx.core.events.PrintingJsonEvents

data class KeyFederationDownloadStats(val startOfHour: String, val origin: String, val testType: Int, val numberOfKeys: Int)

class KeyFederationDownloadStatsConverter : Converter<KeyFederationDownloadStats>() {

    override fun convert(map: Map<String, String>): KeyFederationDownloadStats {
        return KeyFederationDownloadStats(
            startOfHour = map["start_of_hour"] ?: error("missing start_of_hour field from cloudwatch log insights"),
            origin = map["origin"] ?: error("missing origin field from cloudwatch log insights"),
            testType = map["test_type"]?.toInt() ?: 0,
            numberOfKeys = map["number_of_keys"]?.toInt() ?: 0
        )
    }
}

class KeyFederationDownloadAnalyticsHandler : LogInsightsAnalyticsHandler(
    logAnalyticsService(Environment.fromSystem(), CLOCK, PrintingJsonEvents(CLOCK), keyFederationDownloadQueryString, KeyFederationDownloadStatsConverter()),
    PrintingJsonEvents(CLOCK)
)

private const val keyFederationDownloadQueryString = """fields @timestamp, @message
| filter @message like /^\{/
| filter metadata.name = 'DownloadedFederatedDiagnosisKeys'
| stats sum(event.validKeys) as number_of_keys by bin(1h) as start_of_hour, event.origin as origin, event.testType as test_type"""

