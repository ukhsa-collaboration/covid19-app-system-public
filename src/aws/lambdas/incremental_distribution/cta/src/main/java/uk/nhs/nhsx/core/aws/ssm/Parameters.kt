package uk.nhs.nhsx.core.aws.ssm

import java.lang.Enum.valueOf
import java.util.*
import java.util.function.Function

interface Parameters {
    fun <T> parameter(name: ParameterName, convert: Function<String, T>): Parameter<T>

    companion object {
        val positive = setOf("yes", "true", "enabled")
    }
}

fun <T : Enum<T>> Parameters.ofEnum(name: ParameterName, type: Class<T>, whenError: T): Parameter<T> =
    parameter(name) { v: String -> convertEnum(type, v, whenError) }

fun <T : Enum<T>> convertEnum(type: Class<T>, value: String, whenError: T): T = try {
    valueOf(type, value.uppercase(Locale.getDefault()))
} catch (noMatchingEnumValue: IllegalArgumentException) {
    whenError
}

fun Parameters.ofBoolean(name: ParameterName): Parameter<Boolean> =
    parameter(name) { v: String? -> v != null && Parameters.positive.contains(v.lowercase(Locale.getDefault())) }
