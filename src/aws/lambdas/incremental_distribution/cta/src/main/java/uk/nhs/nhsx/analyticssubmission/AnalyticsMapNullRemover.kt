@file:Suppress("UNCHECKED_CAST")

package uk.nhs.nhsx.analyticssubmission

object AnalyticsMapNullRemover {

    fun removeNullValues(map: Map<String, Any?>): Map<String, Any?> {
        val mutableFlattened = map.toMutableMap()
        mutableFlattened.values.removeAll(sequenceOf(null))

        val nonMutableMap = mutableFlattened.toMap()
        return nonMutableMap
    }
}
