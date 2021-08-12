package smoke.actors

import org.http4k.client.JavaHttpClient
import org.http4k.cloudnative.env.Environment
import org.http4k.cloudnative.env.EnvironmentKey
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.NoOp
import org.http4k.core.then
import org.http4k.filter.DebuggingFilters
import org.http4k.lens.boolean

val DEBUG = EnvironmentKey.boolean().defaulted("DEBUG", false)

fun createHandler(env: Environment): HttpHandler = when {
    DEBUG(env) -> DebuggingFilters.PrintRequestAndResponse().then(JavaHttpClient())
    else -> Filter.NoOp.then(JavaHttpClient())
}
