package uk.nhs.nhsx.core

import uk.nhs.nhsx.core.Environment.EnvironmentKey
import java.util.*
import java.util.function.Function

object TestEnvironments {

    val NOTHING: Environment.Access = object : Environment.Access {
        override fun <T> optional(key: EnvironmentKey<T>): Optional<T> = Optional.empty()
    }

    val TEST = Function { t: Map<String, String> -> Environment.fromName("test", Environment.Access.TEST.apply(t)) }

    fun environmentWith(vararg pairs: Pair<String, String>): Environment = TEST.apply(mapOf(*pairs.toList().toTypedArray()))
}
