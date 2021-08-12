package uk.nhs.nhsx.analyticssubmission.model

import uk.nhs.nhsx.analyticssubmission.PostDistrictLaReplacer.replacePostDistrictLA
import uk.nhs.nhsx.core.AppServicesJson.mapper
import uk.nhs.nhsx.core.events.Events

object StoredAnalyticsSubmissionPayload {

    @Suppress("UNCHECKED_CAST")
    fun convertFrom(clientPayload: ClientAnalyticsSubmissionPayload, events: Events): Map<String, Any?> {
        val (postalDistrict, localAuthority) = replacePostDistrictLA(
            clientPayload.metadata.postalDistrict,
            clientPayload.metadata.localAuthority,
            events
        )

        val payloadMap = mapper.convertValue(clientPayload, Map::class.java) as Map<String, Any?>
        return recFlatten(payloadMap)
            .toMutableMap()
            .also {
                it["postalDistrict"] = postalDistrict
                it["localAuthority"] = localAuthority
            }
    }

    @Suppress("UNCHECKED_CAST")
    private fun recFlatten(mapOfMaps: Map<String, *>): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()
        mapOfMaps.forEach {
            if (it.value is Map<*, *>)
                result.putAll(recFlatten(it.value as Map<String, Any?>))
            else
                result[it.key] = it.value
        }
        return result
    }

}
