package uk.nhs.nhsx.analyticslogs

import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.SystemClock.CLOCK
import uk.nhs.nhsx.core.events.PrintingJsonEvents
import uk.nhs.nhsx.domain.Country
import uk.nhs.nhsx.domain.TestKit
import uk.nhs.nhsx.domain.TestResult
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class CtaTokenGenStats(
    val startDate: LocalDate,
    val testType: TestKit,
    val source: Country,
    val resultType: TestResult,
    val total: Int
)

class CtaTokenGenStatsConverter : Converter<CtaTokenGenStats>() {
    private val dateTimeFormatterPattern = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    override fun convert(map: Map<String, String>) = CtaTokenGenStats(
        startDate = LocalDate.parse(map.getValue("start_date"), dateTimeFormatterPattern),
        testType = TestKit.valueOf(map.getValue("test_type")),
        source = Country.from(map.getValue("source")),
        resultType = TestResult.from(map.getValue("result_type")),
        total = map.getOrDefault("total", "0").toInt()
    )
}

class CtaTokenGenAnalyticsHandler : LogInsightsAnalyticsHandler(
    service = logAnalyticsService(
        environment = Environment.fromSystem(),
        clock = CLOCK,
        events = PrintingJsonEvents(CLOCK),
        queryString = ctaTokenGenQueryString,
        converter = CtaTokenGenStatsConverter()
    ),
    events = PrintingJsonEvents(CLOCK)
)

private const val ctaTokenGenQueryString = """fields @timestamp, @message
| filter @message like /^\{/
| filter metadata.name = "CtaTokenGen"
| stats count() as total by bin(1d) as start_date, event.testKit as test_type, event.source as source, event.testResult as result_type"""
