package uk.nhs.nhsx.analyticslogs

import com.amazonaws.services.logs.model.ResultField

abstract class Converter<T> {
    fun from(logRows: List<List<ResultField>>): List<T> {
        return logRows.map {
            val map: MutableMap<String, String> = HashMap()
            for (resultField in it) {
                check(map.put(resultField.field, resultField.value) == null) { "Duplicate key" }
            }
            convert(map)
        }
    }

    abstract fun convert(map: Map<String, String>): T

}




