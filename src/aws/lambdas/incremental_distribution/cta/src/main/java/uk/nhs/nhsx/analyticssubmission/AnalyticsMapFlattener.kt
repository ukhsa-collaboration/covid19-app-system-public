package uk.nhs.nhsx.analyticssubmission

import uk.nhs.nhsx.core.AppServicesJson

object AnalyticsMapFlattener {

    @Suppress("UNCHECKED_CAST")
    fun recFlatten(input: Any): Map<String, Any?> {
        val payloadMap = AppServicesJson.mapper.convertValue(input, Map::class.java) as Map<String, Any?>
        return recFlattenMap(payloadMap)
    }

    @Suppress("UNCHECKED_CAST")
    private fun recFlattenMap(mapOfMaps: Map<String, *>): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()
        mapOfMaps.forEach {
            if (it.value is Map<*, *>)
                result.putAll(recFlattenMap(it.value as Map<String, Any?>))
            else
                result[it.key] = it.value
        }
        return result
    }

}
