package uk.nhs.nhsx.analyticslogs

import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.SystemClock.CLOCK
import uk.nhs.nhsx.core.events.PrintingJsonEvents

data class DiagnosisKeysSubmissionStats(val startOfHour: String, val platform: String, val version: String, val diagnosisKeysCount: Int)

class DiagnosisKeysSubmissionStatsConverter : Converter<DiagnosisKeysSubmissionStats>() {

    override fun convert(map: Map<String, String>): DiagnosisKeysSubmissionStats {
        return DiagnosisKeysSubmissionStats(
            startOfHour = map["start_of_hour"] ?: error("missing start_of_hour field from cloudwatch log insights"),
            platform = map["platform"] ?: error("missing platform field from cloudwatch log insights"),
            version = map["version"]?: error("missing version field from cloudwatch log insights"),
            diagnosisKeysCount = map["num_of_diagnosis_keys"]?.toInt() ?: 0,
        )
    }
}

class DiagnosisKeysSubmissionStatsAnalyticsHandler : LogInsightsAnalyticsHandler(
    logAnalyticsService(Environment.fromSystem(), CLOCK, PrintingJsonEvents(CLOCK), DiagnosisKeysSubmissionStatsQueryString, DiagnosisKeysSubmissionStatsConverter()),
    PrintingJsonEvents(CLOCK)
)

private const val DiagnosisKeysSubmissionStatsQueryString = """fields @timestamp, @message
| filter @message like /^\{/
| filter metadata.name = 'IncomingHttpRequest'
| filter event.status = 200
| filter event.userAgent.os = "iOS" or event.userAgent.os = "Android"
| stats count() as num_of_diagnosis_keys by bin(1h) as start_of_hour, event.userAgent.os as platform, event.userAgent.appVersion.semVer as version"""

