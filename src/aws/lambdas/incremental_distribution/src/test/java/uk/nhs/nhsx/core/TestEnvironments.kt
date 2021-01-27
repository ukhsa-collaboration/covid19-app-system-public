package uk.nhs.nhsx.core

import uk.nhs.nhsx.core.Environment.EnvironmentKey
import uk.nhs.nhsx.core.TestEnvironments
import java.util.*
import java.util.function.Function

object TestEnvironments {

    @JvmField
    val NOTHING: Environment.Access = object : Environment.Access {
        override fun <T> optional(key: EnvironmentKey<T>): Optional<T> = Optional.empty()
    }

    val EMPTY: Environment = Environment.fromName("empty", NOTHING)

    @JvmField
    val TEST = Function { t: Map<String, String> -> Environment.fromName("test", Environment.Access.TEST.apply(t)) }
}