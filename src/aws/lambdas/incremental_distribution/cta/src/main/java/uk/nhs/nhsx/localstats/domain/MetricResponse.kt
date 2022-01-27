package uk.nhs.nhsx.localstats.domain

data class MetricResponse(
    val areaType: AreaType,
    val metrics: List<Metric>
) {
    operator fun plus(other: MetricResponse) = MetricResponse(areaType, metrics + other.metrics)
}
