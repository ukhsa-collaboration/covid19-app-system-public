package uk.nhs.nhsx.localstats.domain

import java.time.LocalDate

data class Metric(
    val areaType: AreaTypeCode,
    val areaCode: AreaCode,
    val areaName: AreaName,
    val date: LocalDate,
    val value: Pair<CoronavirusMetric, String?>
) {
    val metricName get() = value.first
    val metricValue get() = value.second
}
