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

data class CtaExchangeStats(
    val startDate: LocalDate,
    val testType: TestKit,
    val platform: MobileOS?,
    val tokenAgeRange: TokenAgeRange,
    val source: Country,
    val total: Int,
    val appVersion: String
)

class CtaExchangeStatsConverter : Converter<CtaExchangeStats>() {
    private val dateTimeFormatterPattern = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    override fun convert(map: Map<String, String>) = CtaExchangeStats(
        startDate = LocalDate.parse(map.getValue("start_date"), dateTimeFormatterPattern),
        testType = TestKit.valueOf(map.getValue("test_type")),
        platform = MobileOS.valueOf(map.getValue("platform")),
        tokenAgeRange = TokenAgeRange.valueOf(map.getValue("token_age_range")),
        source = Country.from(map.getValue("source")),
        total = map.getOrDefault("total", "0").toInt(),
        appVersion = map.getOrDefault("app_version", "UNKNOWN")
    )
}

class CtaExchangeAnalyticsHandler : LogInsightsAnalyticsHandler(
    service = logAnalyticsService(
        environment = Environment.fromSystem(),
        clock = SystemClock.CLOCK,
        events = PrintingJsonEvents(SystemClock.CLOCK),
        queryString = ctaExchangeQueryString,
        converter = CtaExchangeStatsConverter()
    ),
    events = PrintingJsonEvents(SystemClock.CLOCK)
)

private const val ctaExchangeQueryString = """fields @timestamp, @message
| filter @message like /^\{/
| filter metadata.name = "SuccessfulCtaExchange"
| sort @timestamp desc
| stats count() as total by bin(1d) as start_date, event.testKit as test_type, event.country as source, event.mobileOS as platform, event.tokenAgeRange as token_age_range, event.appVersion.semVer as app_version"""
