@file:Suppress("UNCHECKED_CAST")

package uk.nhs.nhsx.analyticssubmission

import com.fasterxml.jackson.core.type.TypeReference
import uk.nhs.nhsx.core.AppServicesJson

object AnalyticsMapFlattener {

    private val mapReference = object : TypeReference<Map<String, Any?>>() {}

    fun flattenRecursively(input: Any) = AppServicesJson.mapper.convertValue(input, mapReference).flatten()

    private fun Map<String, Any?>.flatten(): Map<String, Any?> {
        val target = mutableMapOf<String, Any?>()

        for (entry in this) {
            when (entry.value) {
                is Map<*, *> -> (entry.value as Map<String, Any?>).flatten().let(target::putAll)
                else -> target[entry.key] = entry.value
            }
        }

        return target
    }
}
