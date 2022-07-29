package uk.nhs.nhsx.core

import dev.forkhandles.values.Value
import dev.forkhandles.values.ValueFactory
import uk.nhs.nhsx.core.EnvironmentName.Companion.of
import java.lang.Boolean.parseBoolean
import java.time.Duration
import java.time.LocalDate
import java.util.Optional
import java.util.function.Function
import java.util.function.Supplier

class Environment private constructor(val name: EnvironmentName, val type: EnvironmentType, val access: Access) {
    interface Access {
        fun <T> optional(key: EnvironmentKey<T>): Optional<T>

        fun <T> required(key: EnvironmentKey<T>): T = optional(key)
            .orElseThrow { IllegalArgumentException("Environment variable ${key.name} is missing") }

        fun <T> defaulted(key: EnvironmentKey<T>, value: Supplier<T>): T = optional(key).orElseGet(value)

        companion object {
            val SYSTEM: Access = object : Access {
                override fun <T> optional(key: EnvironmentKey<T>) =
                    Optional.ofNullable(System.getenv(key.name)).map(key.conversion)
            }
            val TEST = Function<Map<String, String>, Access> { m: Map<String, String> ->
                object : Access {
                    override fun <T> optional(key: EnvironmentKey<T>) =
                        Optional.ofNullable(m[key.name]).map(key.conversion)
                }
            }
        }
    }

    class EnvironmentKey<T>(val name: String, val conversion: (String) -> T) {
        companion object {
            private fun <T> define(name: String, conversion: (String) -> T): EnvironmentKey<T> =
                EnvironmentKey(name, conversion)

            fun string(name: String) = define(name) { it }

            fun bool(name: String) = define(name, ::parseBoolean)

            fun duration(name: String) = define(name, Duration::parse)

            fun integer(name: String) = define(name, String::toInt)

            fun long(name: String) = define(name, String::toLong)

            fun strings(name: String) = define(name) { it.split(",").map(String::trim).filter(String::isNotBlank) }

            fun localDate(name: String) = define(name, LocalDate::parse)

            fun <X : Any, T : Value<X>> value(name: String, fn: ValueFactory<T, X>) = define(name, fn::parse)
        }
    }

    companion object {
        val WORKSPACE = EnvironmentKey.string("WORKSPACE")

        private const val UNKNOWN = "unknown"
        fun unknown(): Environment = fromName(UNKNOWN, Access.SYSTEM)

        fun fromSystem(): Environment = fromEnvironment(Access.SYSTEM)

        fun fromEnvironment(access: Access): Environment = fromName(access.required(WORKSPACE), access)

        fun fromName(name: String, access: Access): Environment =
            Environment(of(name), environmentTypeFrom(name), access)

        private fun environmentTypeFrom(name: String) =
            when {
                name == UNKNOWN || name.startsWith("te-prod") -> EnvironmentType.Production
                else -> EnvironmentType.NonProduction
            }
    }
}
