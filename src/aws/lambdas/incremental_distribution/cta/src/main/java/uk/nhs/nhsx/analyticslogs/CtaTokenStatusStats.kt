package uk.nhs.nhsx.analyticslogs

import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.SystemClock
import uk.nhs.nhsx.core.events.PrintingJsonEvents
import uk.nhs.nhsx.domain.Country
import uk.nhs.nhsx.domain.TestKit
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class CtaTokenStatusStats(
    val startDate: LocalDate,
    val testType: TestKit,
    val source: Country,
    val total: Int
)

class CtaTokenStatusStatsConverter : Converter<CtaTokenStatusStats>() {
    private val dateTimeFormatterPattern = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    override fun convert(map: Map<String, String>) = CtaTokenStatusStats(
        startDate = LocalDate.parse(map.getValue("start_date"), dateTimeFormatterPattern),
        testType = TestKit.valueOf(map.getValue("test_type")),
        source = Country.from(map.getValue("source")),
        total = map.getOrDefault("total", "0").toInt()
    )
}

class CtaTokenStatusAnalyticsHandler : LogInsightsAnalyticsHandler(
    service = logAnalyticsService(
        environment = Environment.fromSystem(),
        clock = SystemClock.CLOCK,
        events = PrintingJsonEvents(SystemClock.CLOCK),
        queryString = ctaTokenStatusQueryString,
        converter = CtaTokenStatusStatsConverter()
    ),
    events = PrintingJsonEvents(SystemClock.CLOCK)
)

private const val ctaTokenStatusQueryString = """fields @timestamp, @message
| filter @message like /^\{/
| filter metadata.name = "ConsumableTokenStatusCheck"
| stats count() as total by bin(1d) as start_date, event.testKit as test_type, event.source as source"""
