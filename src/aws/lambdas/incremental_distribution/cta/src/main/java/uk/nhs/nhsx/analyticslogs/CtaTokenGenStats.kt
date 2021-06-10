package uk.nhs.nhsx.analyticslogs

import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.SystemClock
import uk.nhs.nhsx.core.events.PrintingJsonEvents
import uk.nhs.nhsx.domain.Country
import uk.nhs.nhsx.domain.TestKit
import uk.nhs.nhsx.domain.TestResult
import java.time.LocalDate
import java.time.format.DateTimeFormatter


data class CtaTokenGenStats(val startDate: LocalDate, val testType: TestKit, val source: Country, val resultType: TestResult, val total: Int)

class CtaTokenGenStatsConverter : Converter<CtaTokenGenStats>() {
    private val dateTimeFormatterPattern = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    override fun convert(map: Map<String, String>): CtaTokenGenStats {
        return CtaTokenGenStats(
            startDate = LocalDate.parse(map["start_date"] ?: error("missing start_date field from cloudwatch log insights"), dateTimeFormatterPattern),
            testType = TestKit.valueOf(map["test_type"] ?: error("missing test_type field from cloudwatch log insights")),
            source = Country.from(map["source"] ?: error("missing source field from cloudwatch log insights")),
            resultType = TestResult.from(map["result_type"] ?: error("missing result_type field from cloudwatch log insights")),
            total = map["total"]?.toInt() ?: 0
        )
    }
}

class CtaTokenGenAnalyticsHandler : LogInsightsAnalyticsHandler(
    logAnalyticsService(
        Environment.fromSystem(),
        SystemClock.CLOCK,
        PrintingJsonEvents(SystemClock.CLOCK),
        ctaTokenGenQueryString,
        CtaTokenGenStatsConverter()),
    PrintingJsonEvents(SystemClock.CLOCK)
)

private const val ctaTokenGenQueryString = """fields @timestamp, @message
| filter @message like /^\{/
| filter metadata.name = "CtaTokenGen"
| stats count() as total by bin(1d) as start_date, event.testKit as test_type, event.source as source, event.testResult as result_type"""
