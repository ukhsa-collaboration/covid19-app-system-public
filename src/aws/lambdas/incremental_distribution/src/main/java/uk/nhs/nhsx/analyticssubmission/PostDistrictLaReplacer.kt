package uk.nhs.nhsx.analyticssubmission

import uk.nhs.nhsx.analyticssubmission.model.PostDistrictPair
import uk.nhs.nhsx.analyticssubmission.model.PostDistrictPair.Companion.UNKNOWN
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.InfoEvent

object PostDistrictLaReplacer {

    private val postDistrictLAMapping =
        javaClass.classLoader.getResource("analyticssubmission/PD_LA_to_MergedPD_LA.csv")
            ?.readText()
            ?.let { PostDistrictLaReplacerCsvParser.parse(it) }
            ?: error("Failed to read csv file for postcode/LA tuple replacement")

    @JvmStatic
    fun replacePostDistrictLA(
        postDistrict: String?,
        localAuthority: String?,
        events: Events
    ): PostDistrictPair {
        val key1 = PostDistrictPair(postDistrict, localAuthority ?: UNKNOWN)
        val key2 = PostDistrictPair(postDistrict, UNKNOWN)
        val key3 = PostDistrictPair(UNKNOWN, UNKNOWN)
        val result = postDistrictLAMapping[key1] ?: postDistrictLAMapping[key2] ?: key3
        if (result == key3) {
            events(
                PostDistrictLaReplacer::class.java,
                InfoEvent("Post district LA tuple not found in mapping. Persisting post district and localAuthority as $UNKNOWN")
            )
        }
        return result
    }
}
