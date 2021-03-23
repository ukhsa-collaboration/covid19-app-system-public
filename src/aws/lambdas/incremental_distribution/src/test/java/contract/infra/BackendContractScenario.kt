package contract.infra

import org.http4k.client.JavaHttpClient
import org.http4k.core.Filter
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters.SetHostFrom
import org.http4k.servirtium.ServirtiumServer
import smoke.env.EnvConfig

interface BackendContractScenario {
    val envConfig: EnvConfig

    val uri get() = Uri.of("http://localhost:${control.port()}")

    fun http() = JavaHttpClient()

    fun mitmHttpClient() = SetHostFrom(uri)
        .then(TidyJsonBody())
        .then(http())

    /**
     * These calls are performed for recording tests but ignored for replays
     */
    fun outOfBandAction(call: () -> Unit)

    val control: ServirtiumServer
}

fun TidyJsonBody() = Filter { next ->
    {
        next(it.tidyJsonBody()).tidyJsonBody().also { b -> println(b.bodyString()) }
    }
}
