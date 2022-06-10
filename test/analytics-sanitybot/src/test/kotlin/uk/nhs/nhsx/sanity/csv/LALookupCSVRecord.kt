package uk.nhs.nhsx.sanity.csv

import dev.forkhandles.result4k.resultFrom
import org.apache.commons.csv.CSVRecord

object LALookupCSVRecordHeader {
    const val local_authority = "local_authority"
    const val region = "region"
    const val country = "country"
    const val local_authority_population = "local_authority_population"
    const val latitude = "latitude"
    const val longitude = "longitude"
    const val lad20cd = "lad20cd"
    const val gor10cd = "gor10cd"
    const val ctry12cd = "ctry12cd"
    const val upper_tier_local_authority = "upper_tier_local_authority"
    const val local_authority_welsh = "local_authority_welsh"
    const val upper_tier_local_authority_welsh = "upper_tier_local_authority_welsh"
    const val region_welsh = "region_welsh"
    const val country_welsh = "country_welsh"
    fun years_age_population(year: Int) = "$year" + "_years_age_population"
    fun plus_years_age_population(year: Int) = "$year" + "_plus_years_age_population"
    fun minus_years_age_population(year: Int) = "$year" + "_minus_years_age_population"
}

data class LALookupCSVRecord(
    val local_authority: String,
    val region: String,
    val country: String,
    val local_authority_population: String,
    val latitude: String,
    val longitude: String,
    val lad20cd: String,
    val gor10cd: String,
    val ctry12cd: String,
    val upper_tier_local_authority: String,
    val local_authority_welsh: String,
    val upper_tier_local_authority_welsh: String,
    val region_welsh: String,
    val country_welsh: String,
    val years_age_population_0 : String,
    val years_age_population_1 : String,
    val years_age_population_2 : String,
    val years_age_population_3 : String,
    val years_age_population_4 : String,
    val years_age_population_5 : String,
    val years_age_population_6 : String,
    val years_age_population_7 : String,
    val years_age_population_8 : String,
    val years_age_population_9 : String,
    val years_age_population_10 : String,
    val years_age_population_11 : String,
    val years_age_population_12 : String,
    val years_age_population_13 : String,
    val years_age_population_14 : String,
    val years_age_population_15 : String,
    val years_age_population_16 : String,
    val years_age_population_17 : String,
    val years_age_population_18 : String,
    val years_age_population_19 : String,
    val years_age_population_20 : String,
    val years_age_population_21 : String,
    val years_age_population_22 : String,
    val years_age_population_23 : String,
    val years_age_population_24 : String,
    val years_age_population_25 : String,
    val years_age_population_26 : String,
    val years_age_population_27 : String,
    val years_age_population_28 : String,
    val years_age_population_29 : String,
    val years_age_population_30 : String,
    val years_age_population_31 : String,
    val years_age_population_32 : String,
    val years_age_population_33 : String,
    val years_age_population_34 : String,
    val years_age_population_35 : String,
    val years_age_population_36 : String,
    val years_age_population_37 : String,
    val years_age_population_38 : String,
    val years_age_population_39 : String,
    val years_age_population_40 : String,
    val years_age_population_41 : String,
    val years_age_population_42 : String,
    val years_age_population_43 : String,
    val years_age_population_44 : String,
    val years_age_population_45 : String,
    val years_age_population_46 : String,
    val years_age_population_47 : String,
    val years_age_population_48 : String,
    val years_age_population_49 : String,
    val years_age_population_50 : String,
    val years_age_population_51 : String,
    val years_age_population_52 : String,
    val years_age_population_53 : String,
    val years_age_population_54 : String,
    val years_age_population_55 : String,
    val years_age_population_56 : String,
    val years_age_population_57 : String,
    val years_age_population_58 : String,
    val years_age_population_59 : String,
    val years_age_population_60 : String,
    val years_age_population_61 : String,
    val years_age_population_62 : String,
    val years_age_population_63 : String,
    val years_age_population_64 : String,
    val years_age_population_65 : String,
    val years_age_population_66 : String,
    val years_age_population_67 : String,
    val years_age_population_68 : String,
    val years_age_population_69 : String,
    val years_age_population_70 : String,
    val years_age_population_71 : String,
    val years_age_population_72 : String,
    val years_age_population_73 : String,
    val years_age_population_74 : String,
    val years_age_population_75 : String,
    val years_age_population_76 : String,
    val years_age_population_77 : String,
    val years_age_population_78 : String,
    val years_age_population_79 : String,
    val years_age_population_80 : String,
    val years_age_population_81 : String,
    val years_age_population_82 : String,
    val years_age_population_83 : String,
    val years_age_population_84 : String,
    val years_age_population_85 : String,
    val years_age_population_86 : String,
    val years_age_population_87 : String,
    val years_age_population_88 : String,
    val years_age_population_89 : String,
    val plus_years_age_population_90 : String,
    val plus_years_age_population_16 : String,
    val minus_years_age_population_16 : String,
) {
    companion object
}

fun CSVRecord.asLALookupCSVRecord() = resultFrom {
    with(LALookupCSVRecordHeader) {
        LALookupCSVRecord(
            local_authority = get(local_authority),
            region = get(region),
            country = get(country),
            local_authority_population = get(local_authority_population),
            latitude = get(latitude),
            longitude = get(longitude),
            lad20cd = get(lad20cd),
            gor10cd = get(gor10cd),
            ctry12cd = get(ctry12cd),
            upper_tier_local_authority = get(upper_tier_local_authority),
            local_authority_welsh = get(local_authority_welsh),
            upper_tier_local_authority_welsh = get(upper_tier_local_authority_welsh),
            region_welsh = get(region_welsh),
            country_welsh = get(country_welsh),
            years_age_population_0 = getYearsAgePopulation(0),
            years_age_population_1 = getYearsAgePopulation(1),
            years_age_population_2 = getYearsAgePopulation(2),
            years_age_population_3 = getYearsAgePopulation(3),
            years_age_population_4 = getYearsAgePopulation(4),
            years_age_population_5 = getYearsAgePopulation(5),
            years_age_population_6 = getYearsAgePopulation(6),
            years_age_population_7 = getYearsAgePopulation(7),
            years_age_population_8 = getYearsAgePopulation(8),
            years_age_population_9 = getYearsAgePopulation(9),
            years_age_population_10 = getYearsAgePopulation(10),
            years_age_population_11 = getYearsAgePopulation(11),
            years_age_population_12 = getYearsAgePopulation(12),
            years_age_population_13 = getYearsAgePopulation(13),
            years_age_population_14 = getYearsAgePopulation(14),
            years_age_population_15 = getYearsAgePopulation(15),
            years_age_population_16 = getYearsAgePopulation(16),
            years_age_population_17 = getYearsAgePopulation(17),
            years_age_population_18 = getYearsAgePopulation(18),
            years_age_population_19 = getYearsAgePopulation(19),
            years_age_population_20 = getYearsAgePopulation(20),
            years_age_population_21 = getYearsAgePopulation(21),
            years_age_population_22 = getYearsAgePopulation(22),
            years_age_population_23 = getYearsAgePopulation(23),
            years_age_population_24 = getYearsAgePopulation(24),
            years_age_population_25 = getYearsAgePopulation(25),
            years_age_population_26 = getYearsAgePopulation(26),
            years_age_population_27 = getYearsAgePopulation(27),
            years_age_population_28 = getYearsAgePopulation(28),
            years_age_population_29 = getYearsAgePopulation(29),
            years_age_population_30 = getYearsAgePopulation(30),
            years_age_population_31 = getYearsAgePopulation(31),
            years_age_population_32 = getYearsAgePopulation(32),
            years_age_population_33 = getYearsAgePopulation(33),
            years_age_population_34 = getYearsAgePopulation(34),
            years_age_population_35 = getYearsAgePopulation(35),
            years_age_population_36 = getYearsAgePopulation(36),
            years_age_population_37 = getYearsAgePopulation(37),
            years_age_population_38 = getYearsAgePopulation(38),
            years_age_population_39 = getYearsAgePopulation(39),
            years_age_population_40 = getYearsAgePopulation(40),
            years_age_population_41 = getYearsAgePopulation(41),
            years_age_population_42 = getYearsAgePopulation(42),
            years_age_population_43 = getYearsAgePopulation(43),
            years_age_population_44 = getYearsAgePopulation(44),
            years_age_population_45 = getYearsAgePopulation(45),
            years_age_population_46 = getYearsAgePopulation(46),
            years_age_population_47 = getYearsAgePopulation(47),
            years_age_population_48 = getYearsAgePopulation(48),
            years_age_population_49 = getYearsAgePopulation(49),
            years_age_population_50 = getYearsAgePopulation(50),
            years_age_population_51 = getYearsAgePopulation(51),
            years_age_population_52 = getYearsAgePopulation(52),
            years_age_population_53 = getYearsAgePopulation(53),
            years_age_population_54 = getYearsAgePopulation(54),
            years_age_population_55 = getYearsAgePopulation(55),
            years_age_population_56 = getYearsAgePopulation(56),
            years_age_population_57 = getYearsAgePopulation(57),
            years_age_population_58 = getYearsAgePopulation(58),
            years_age_population_59 = getYearsAgePopulation(59),
            years_age_population_60 = getYearsAgePopulation(60),
            years_age_population_61 = getYearsAgePopulation(61),
            years_age_population_62 = getYearsAgePopulation(62),
            years_age_population_63 = getYearsAgePopulation(63),
            years_age_population_64 = getYearsAgePopulation(64),
            years_age_population_65 = getYearsAgePopulation(65),
            years_age_population_66 = getYearsAgePopulation(66),
            years_age_population_67 = getYearsAgePopulation(67),
            years_age_population_68 = getYearsAgePopulation(68),
            years_age_population_69 = getYearsAgePopulation(69),
            years_age_population_70 = getYearsAgePopulation(70),
            years_age_population_71 = getYearsAgePopulation(71),
            years_age_population_72 = getYearsAgePopulation(72),
            years_age_population_73 = getYearsAgePopulation(73),
            years_age_population_74 = getYearsAgePopulation(74),
            years_age_population_75 = getYearsAgePopulation(75),
            years_age_population_76 = getYearsAgePopulation(76),
            years_age_population_77 = getYearsAgePopulation(77),
            years_age_population_78 = getYearsAgePopulation(78),
            years_age_population_79 = getYearsAgePopulation(79),
            years_age_population_80 = getYearsAgePopulation(80),
            years_age_population_81 = getYearsAgePopulation(81),
            years_age_population_82 = getYearsAgePopulation(82),
            years_age_population_83 = getYearsAgePopulation(83),
            years_age_population_84 = getYearsAgePopulation(84),
            years_age_population_85 = getYearsAgePopulation(85),
            years_age_population_86 = getYearsAgePopulation(86),
            years_age_population_87 = getYearsAgePopulation(87),
            years_age_population_88 = getYearsAgePopulation(88),
            years_age_population_89 = getYearsAgePopulation(89),
            plus_years_age_population_90 = getPlusYearsAgePopulation(90),
            plus_years_age_population_16 = getPlusYearsAgePopulation(16),
            minus_years_age_population_16 = getMinusYearsAgePopulation(16),
        )
    }
}

private fun CSVRecord.getYearsAgePopulation(year: Int) = get(LALookupCSVRecordHeader.years_age_population(year))
private fun CSVRecord.getPlusYearsAgePopulation(year: Int) = get(LALookupCSVRecordHeader.plus_years_age_population(year))
private fun CSVRecord.getMinusYearsAgePopulation(year: Int) = get(LALookupCSVRecordHeader.minus_years_age_population(year))
