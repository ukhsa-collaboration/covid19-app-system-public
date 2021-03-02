package uk.nhs.nhsx.pubdash

import uk.nhs.nhsx.core.Environment
import java.util.function.Function

object TestEnvironments {

    @JvmField
    val TEST = Function { t: Map<String, String> -> Environment.fromName("test", Environment.Access.TEST.apply(t)) }

    fun environmentWith(vararg pairs: Pair<String, String>): Environment =
        TEST.apply(mapOf(*pairs.toList().toTypedArray()))
}
