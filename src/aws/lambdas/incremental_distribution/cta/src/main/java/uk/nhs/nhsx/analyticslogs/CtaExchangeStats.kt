package uk.nhs.nhsx.analyticslogs

import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.SystemClock
import uk.nhs.nhsx.core.events.PrintingJsonEvents
import uk.nhs.nhsx.core.headers.MobileOS
import uk.nhs.nhsx.domain.Country
import uk.nhs.nhsx.domain.TestKit
import uk.nhs.nhsx.domain.TokenAgeRange
import java.time.LocalDate
import java.time.format.DateTimeFormatter


data class CtaExchangeStats(val startDate: LocalDate, val testType: TestKit, val platform: MobileOS?, val tokenAgeRange : TokenAgeRange, val source: Country, val total: Int,  val appVersion: String)

class CtaExchangeStatsConverter : Converter<CtaExchangeStats>() {
    private val dateTimeFormatterPattern = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    override fun convert(map: Map<String, String>): CtaExchangeStats {
        return CtaExchangeStats(
            startDate = LocalDate.parse(map["start_date"] ?: error("missing start_date field from cloudwatch log insights"),dateTimeFormatterPattern),
            testType = TestKit.valueOf(map["test_type"] ?: error("missing test_type field from cloudwatch log insights")),
            platform = MobileOS.valueOf(map["platform"] ?: error("missing platform field from cloudwatch log insights")),
            tokenAgeRange = TokenAgeRange.valueOf(map["token_age_range"] ?: error("missing token_age_range field from cloudwatch log insights")),
            source = Country.from(map["source"] ?: error("missing source field from cloudwatch log insights")),
            total = map["total"]?.toInt() ?: 0,
            appVersion = map["app_version"] ?: "UNKNOWN"
        )
    }
}

class CtaExchangeAnalyticsHandler : LogInsightsAnalyticsHandler(
    logAnalyticsService(
        Environment.fromSystem(),
        SystemClock.CLOCK,
        PrintingJsonEvents(SystemClock.CLOCK),
        ctaExchangeQueryString,
        CtaExchangeStatsConverter()),
    PrintingJsonEvents(SystemClock.CLOCK)
)

private const val ctaExchangeQueryString = """fields @timestamp, @message
| filter @message like /^\{/
| filter metadata.name = "SuccessfulCtaExchange"
| sort @timestamp desc
| stats count() as total by bin(1d) as start_date, event.testKit as test_type, event.country as source, event.mobileOS as platform, event.tokenAgeRange as token_age_range, event.appVersion.semVer as app_version"""
