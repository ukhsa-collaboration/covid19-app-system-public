package uk.nhs.nhsx.analyticslogs

import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.SystemClock
import uk.nhs.nhsx.core.events.PrintingJsonEvents

data class KeyFederationUploadStats(val startOfHour: String, val testType: Int, val numberOfKeysUploaded: Int)

class KeyFederationUploadStatsConverter : Converter<KeyFederationUploadStats>() {

    override fun convert(map: Map<String, String>): KeyFederationUploadStats {
        return KeyFederationUploadStats(
            startOfHour = map["start_of_hour"] ?: error("missing start_of_hour field from cloudwatch log insights"),
            testType = map["test_type"]?.toInt() ?: 0,
            numberOfKeysUploaded = map["number_of_keys_uploaded"]?.toInt() ?: 0
        )
    }
}

class KeyFederationUploadAnalyticsHandler : LogInsightsAnalyticsHandler(
    logAnalyticsService(Environment.fromSystem(), SystemClock.CLOCK, PrintingJsonEvents(SystemClock.CLOCK), keyFederationUploadQueryString, KeyFederationUploadStatsConverter()),
    PrintingJsonEvents(SystemClock.CLOCK)
)
private const val keyFederationUploadQueryString = """fields @timestamp, @message
| filter @message like /^\{/
| filter metadata.name = 'UploadedDiagnosisKeys'
| stats sum(event.insertedExposures) as number_of_keys_uploaded by bin(1h) as start_of_hour, event.testType as test_type"""
