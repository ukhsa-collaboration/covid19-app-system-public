package uk.nhs.nhsx.analyticslogs

import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.SystemClock.CLOCK
import uk.nhs.nhsx.core.events.PrintingJsonEvents

data class DiagnosisKeysSubmissionStats(
    val startOfHour: String,
    val platform: String,
    val version: String,
    val diagnosisKeysCount: Int
)

class DiagnosisKeysSubmissionStatsConverter : Converter<DiagnosisKeysSubmissionStats>() {

    override fun convert(map: Map<String, String>) = DiagnosisKeysSubmissionStats(
        startOfHour = map.getValue("start_of_hour"),
        platform = map.getValue("platform"),
        version = map.getValue("version"),
        diagnosisKeysCount = map.getOrDefault("num_of_diagnosis_keys", "0").toInt()
    )
}

class DiagnosisKeysSubmissionStatsAnalyticsHandler : LogInsightsAnalyticsHandler(
    service = logAnalyticsService(
        environment = Environment.fromSystem(),
        clock = CLOCK,
        events = PrintingJsonEvents(CLOCK),
        queryString = DiagnosisKeysSubmissionStatsQueryString,
        converter = DiagnosisKeysSubmissionStatsConverter()
    ),
    events = PrintingJsonEvents(CLOCK)
)

private const val DiagnosisKeysSubmissionStatsQueryString = """fields @timestamp, @message
| filter @message like /^\{/
| filter metadata.name = 'IncomingHttpRequest'
| filter event.status = 200
| filter event.userAgent.os = "iOS" or event.userAgent.os = "Android"
| stats count() as num_of_diagnosis_keys by bin(1h) as start_of_hour, event.userAgent.os as platform, event.userAgent.appVersion.semVer as version"""

