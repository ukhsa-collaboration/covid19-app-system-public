package uk.nhs.nhsx.analyticslogs

import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.SystemClock.CLOCK
import uk.nhs.nhsx.core.events.PrintingJsonEvents

data class KeyFederationDownloadStats(
    val startOfHour: String,
    val origin: String,
    val testType: Int,
    val numberOfKeysDownloaded: Int,
    val numberOfKeysImported: Int
)

class KeyFederationDownloadStatsConverter : Converter<KeyFederationDownloadStats>() {
    override fun convert(map: Map<String, String>) = KeyFederationDownloadStats(
        startOfHour = map.getValue("start_of_hour"),
        origin = map.getValue("origin"),
        testType = map.getOrDefault("test_type", "0").toInt(),
        numberOfKeysDownloaded = map.getOrDefault("number_of_keys_downloaded", "0").toInt(),
        numberOfKeysImported = map.getOrDefault("number_of_keys_imported", "0").toInt()
    )
}

class KeyFederationDownloadAnalyticsHandler : LogInsightsAnalyticsHandler(
    service = logAnalyticsService(
        environment = Environment.fromSystem(),
        clock = CLOCK,
        events = PrintingJsonEvents(CLOCK),
        queryString = keyFederationDownloadQueryString,
        converter = KeyFederationDownloadStatsConverter()
    ),
    events = PrintingJsonEvents(CLOCK)
)

private const val keyFederationDownloadQueryString = """fields @timestamp, @message
| filter @message like /^\{/
| filter metadata.name = 'DownloadedFederatedDiagnosisKeys'
| stats sum(event.validKeys+event.invalidKeys) as number_of_keys_downloaded, sum(event.validKeys) as number_of_keys_imported by bin(1h) as start_of_hour, event.origin as origin, event.testType as test_type"""

