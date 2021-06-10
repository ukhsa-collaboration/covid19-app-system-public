package uk.nhs.nhsx.analyticslogs

import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.SystemClock
import uk.nhs.nhsx.core.events.PrintingJsonEvents
import uk.nhs.nhsx.domain.Country
import uk.nhs.nhsx.domain.TestKit
import java.time.LocalDate
import java.time.format.DateTimeFormatter


data class CtaTokenStatusStats(val startDate: LocalDate, val testType: TestKit, val source: Country, val total: Int)

class CtaTokenStatusStatsConverter : Converter<CtaTokenStatusStats>() {
    private val dateTimeFormatterPattern = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    override fun convert(map: Map<String, String>): CtaTokenStatusStats {
        return CtaTokenStatusStats(
            startDate = LocalDate.parse(map["start_date"] ?: error("missing start_date field from cloudwatch log insights"), dateTimeFormatterPattern),
            testType = TestKit.valueOf(map["test_type"] ?: error("missing test_type field from cloudwatch log insights")),
            source = Country.from(map["source"] ?: error("missing source field from cloudwatch log insights")),
            total = map["total"]?.toInt() ?: 0
        )
    }
}

class CtaTokenStatusAnalyticsHandler : LogInsightsAnalyticsHandler(
    logAnalyticsService(
        Environment.fromSystem(),
        SystemClock.CLOCK,
        PrintingJsonEvents(SystemClock.CLOCK),
        ctaTokenStatusQueryString,
        CtaTokenStatusStatsConverter()),
    PrintingJsonEvents(SystemClock.CLOCK)
)

private const val ctaTokenStatusQueryString = """fields @timestamp, @message
| filter @message like /^\{/
| filter metadata.name = "ConsumableTokenStatusCheck"
| stats count() as total by bin(1d) as start_date, event.testKit as test_type, event.source as source"""
