package uk.nhs.nhsx.localstats.data.actions

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVRecord
import uk.nhs.nhsx.localstats.domain.AreaCode
import uk.nhs.nhsx.localstats.domain.AreaName
import uk.nhs.nhsx.localstats.domain.AreaTypeCode
import uk.nhs.nhsx.localstats.domain.CoronavirusMetric
import uk.nhs.nhsx.localstats.domain.Metric
import java.io.StringReader
import java.time.LocalDate

class CoronavirusDataCSVParser(private val metrics: Set<CoronavirusMetric>) {
    fun parse(input: String) = StringReader(input).use { text ->
        CSVFormat.DEFAULT
            .withFirstRecordAsHeader()
            .withIgnoreEmptyLines()
            .withIgnoreSurroundingSpaces()
            .withTrim()
            .parse(text)
            .use { parser -> parser.flatMap(::toMetric) }
    }

    private fun toMetric(csvRecord: CSVRecord): List<Metric> {
        val areaCode = csvRecord["areaCode"].let(AreaCode::of)
        val areaName = csvRecord["areaName"].let(AreaName::of)
        val areaType = csvRecord["areaType"].let(AreaTypeCode::of)
        val date = csvRecord["date"].let(LocalDate::parse)

        return metrics.toSortedSet().mapNotNull { metric ->
            if (csvRecord.isSet(metric.name)) {
                Metric(
                    areaType = areaType,
                    areaCode = areaCode,
                    areaName = areaName,
                    date = date,
                    value = metric to csvRecord[metric.name].ifBlank { null }
                )
            } else null
        }
    }
}
