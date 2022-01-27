package uk.nhs.nhsx.localstats.domain

import java.math.BigDecimal
import java.time.LocalDate

interface MetricTable {
    fun areaCodes(): List<ProjectedMetricTable>
    fun areaCode(code: AreaCode): ProjectedMetricTable
    fun lastUpdatedOnOrThrow(metric: CoronavirusMetric): LocalDate
}

interface ProjectedMetricTable {
    fun areName(): AreaName
    fun areaCode(): AreaCode
    fun lastUpdatedOnOrThrow(metric: CoronavirusMetric): LocalDate
    fun latestOrThrow(metric: CoronavirusMetric): Metric
    fun latest(metric: CoronavirusMetric): Metric?
}

sealed class MetricValue {
    abstract fun toBigDecimal(): BigDecimal?
    abstract fun toBigDecimalOrThrow(): BigDecimal
    abstract fun toDirection(): Direction?
    abstract fun toDirectionOrThrow(): Direction
}

class SingleMetricValue(private val value: String) : MetricValue() {
    override fun toBigDecimal() = value.toBigDecimal()
    override fun toDirection() = Direction.valueOf(value)
    override fun toBigDecimalOrThrow() = toBigDecimal()
    override fun toDirectionOrThrow() = toDirection()
}

object NullMetricValue : MetricValue() {
    override fun toBigDecimal(): BigDecimal? = null
    override fun toDirection(): Direction? = null
    override fun toBigDecimalOrThrow() = throw NullPointerException()
    override fun toDirectionOrThrow() = throw NullPointerException()
}

fun Metric?.toMetricValue(): MetricValue {
    if (this == null) return NullMetricValue
    return metricValue?.let(::SingleMetricValue) ?: NullMetricValue
}

class InMemoryMetricTable(
    metrics: List<Metric>,
    private val lastUpdated: Map<MetricName, LocalDate>
) : MetricTable {

    private val metricsByAreaCode = metrics.groupBy(Metric::areaCode)

    override fun areaCodes() = metricsByAreaCode
        .map(Map.Entry<AreaCode, List<Metric>>::key)
        .sorted()
        .map(::areaCode)
        .toList()

    override fun areaCode(code: AreaCode) =
        InMemoryProjectedMetricTable(
            areaCode = code,
            metrics = metricsByAreaCode[code].orEmpty(),
            lastUpdated = lastUpdated
        )

    override fun lastUpdatedOnOrThrow(metric: CoronavirusMetric) =
        lastUpdated[MetricName.of(metric.name)] ?: error("metric not found: $metric")
}

class InMemoryProjectedMetricTable(
    private val areaCode: AreaCode,
    metrics: List<Metric>,
    private val lastUpdated: Map<MetricName, LocalDate>
) : ProjectedMetricTable {

    private val areaName = metrics.first().areaName
    private val metricsByDate = metrics.groupBy(Metric::date)

    override fun areName() = areaName
    override fun areaCode() = areaCode

    override fun lastUpdatedOnOrThrow(metric: CoronavirusMetric) =
        lastUpdatedOn(metric) ?: error("metric not found: $metric")

    override fun latestOrThrow(metric: CoronavirusMetric) =
        latest(metric) ?: error("metric not found: $metric")

    override fun latest(metric: CoronavirusMetric): Metric? {
        val date = lastUpdatedOn(metric) ?: LocalDate.EPOCH
        return metricsByDate[date].orEmpty().firstOrNull { it.metricName == metric }
    }

    private fun lastUpdatedOn(metric: CoronavirusMetric) = lastUpdated[MetricName.of(metric.name)]
}
