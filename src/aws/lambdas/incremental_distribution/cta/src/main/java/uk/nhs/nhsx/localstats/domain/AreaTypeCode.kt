@file:Suppress("SpellCheckingInspection")

package uk.nhs.nhsx.localstats.domain

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class AreaTypeCode private constructor(value: String) : StringValue(value.lowercase()) {
    companion object : NonBlankStringValueFactory<AreaTypeCode>(::AreaTypeCode) {
        val LTLA = AreaTypeCode.of("ltla")
        val NATION = AreaTypeCode.of("nation")

        fun from(areaType: AreaType) = when (areaType) {
            is LowerTierLocalAuthority -> LTLA
            is Nation -> NATION
        }
    }
}
