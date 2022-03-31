package uk.nhs.nhsx.analyticslogs

import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.SystemClock.CLOCK
import uk.nhs.nhsx.core.events.PrintingJsonEvents

data class KeyFederationUploadStats(
    val startOfHour: String,
    val testType: Int,
    val numberOfKeysUploaded: Int
)

class KeyFederationUploadStatsConverter : Converter<KeyFederationUploadStats>() {
    override fun convert(map: Map<String, String>) = KeyFederationUploadStats(
        startOfHour = map.getValue("start_of_hour"),
        testType = map.getOrDefault("test_type", "0").toInt(),
        numberOfKeysUploaded = map.getOrDefault("number_of_keys_uploaded", "0").toInt()
    )
}

class KeyFederationUploadAnalyticsHandler : LogInsightsAnalyticsHandler(
    service = logAnalyticsService(
        environment = Environment.fromSystem(),
        clock = CLOCK,
        events = PrintingJsonEvents(CLOCK),
        queryString = keyFederationUploadQueryString,
        converter = KeyFederationUploadStatsConverter()
    ),
    events = PrintingJsonEvents(CLOCK)
)

private const val keyFederationUploadQueryString = """fields @timestamp, @message
| filter @message like /^\{/
| filter metadata.name = 'UploadedDiagnosisKeys'
| stats sum(event.insertedExposures) as number_of_keys_uploaded by bin(1h) as start_of_hour, event.testType as test_type"""
