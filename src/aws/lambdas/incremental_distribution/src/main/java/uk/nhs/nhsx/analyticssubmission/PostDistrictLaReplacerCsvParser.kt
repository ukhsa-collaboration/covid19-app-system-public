package uk.nhs.nhsx.analyticssubmission

import org.apache.commons.csv.CSVFormat
import uk.nhs.nhsx.analyticssubmission.model.PostDistrictPair
import java.io.StringReader

object PostDistrictLaReplacerCsvParser {

    private object Header {
        const val PostcodeDistrictWithLADId = "Postcode_District_LAD_ID"
        const val LAD20CD = "LAD20CD"
        const val MergedPostcodeDistrict = "Merged_Postcode_District"
        val headerNames = listOf(PostcodeDistrictWithLADId, LAD20CD, MergedPostcodeDistrict)
    }

    fun parse(csv: String): Map<PostDistrictPair, PostDistrictPair> {
        if (csv.isBlank()) {
            throw error("Empty csv")
        }

        return CSVFormat.RFC4180
            .withFirstRecordAsHeader()
            .withIgnoreHeaderCase()
            .withIgnoreSurroundingSpaces()
            .parse(StringReader(csv))
            .use { p ->
                if (Header.headerNames != p.headerNames) {
                    error("Invalid header. Expected ${Header.headerNames}")
                }

                p.records.map {
                    if (!it.isConsistent) {
                        error("Invalid data in row ${it.parser.recordNumber}")
                    }

                    val districtWithLadId = it.get(Header.PostcodeDistrictWithLADId)
                    val lad20cd = it.get(Header.LAD20CD)
                    val mergedDistrict = it.get(Header.MergedPostcodeDistrict)

                    if (!districtWithLadId.contains("_")) {
                        error("Invalid data in row ${it.parser.recordNumber}")
                    }

                    val split = districtWithLadId.splitIgnoreEmpty("_", limit = 2)
                    when (split.size) {
                        0 -> PostDistrictPair("", "") to PostDistrictPair(mergedDistrict, null)
                        2 -> PostDistrictPair(split[0], split[1]) to PostDistrictPair(mergedDistrict, lad20cd)
                        else -> PostDistrictPair(split[0], "UNKNOWN") to PostDistrictPair(mergedDistrict, null)
                    }
                }.toMap()
            }
    }

    private fun CharSequence.splitIgnoreEmpty(vararg delimiters: String, limit: Int = 0): List<String> =
        this.split(*delimiters, limit = limit).filter { it.isNotEmpty() }

}
