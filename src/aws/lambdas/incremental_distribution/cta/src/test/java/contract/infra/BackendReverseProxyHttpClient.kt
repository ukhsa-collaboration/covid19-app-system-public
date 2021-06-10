package contract.infra

import org.http4k.client.JavaHttpClient
import org.http4k.core.Filter
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ServerFilters.CatchAll
import org.http4k.format.Jackson
import smoke.env.EnvConfig

/**
 * This Client rewrites the URI of a particular call with the underlying base URL for the destination Lambda, based on the
 * path and the environmental config which is passed in.
 *
 * This logic is quite complicated because we're looking for a matching base path. So we start with the longest path
 * and work our way backward by dropping each section at a time. eg.
 */
fun BackendReverseProxyHttpClient(envConfig: EnvConfig) =
    CatchAll()
        .then(LookupMatchingBackendUrlFrom(envConfig))
        .then(JavaHttpClient())

fun LookupMatchingBackendUrlFrom(envConfig: EnvConfig): Filter {
    val urlPatterns = Jackson.asJsonObject(envConfig).fields().asSequence().toList().filter { it.key.endsWith("_endpoint") }
        .map { it.value!!.textValue() }
        .sortedBy { it }

    fun Request.lookupMatchingBaseUri(): Uri {
        var partsToDrop = 0
        val uriSplitBySlash = uri.path.split("/")
        while (partsToDrop < uriSplitBySlash.size) {
            val pathToCheck = uriSplitBySlash.dropLast(partsToDrop).joinToString("/")
            val matchedUrl = urlPatterns.firstOrNull { it.endsWith(pathToCheck) }?.let { Uri.of(it) }
            when {
                matchedUrl != null -> return uri.authority(matchedUrl.authority).scheme(matchedUrl.scheme)
                else -> partsToDrop++
            }
        }

        error("no matching endpoint found for uri $uri")
    }
    return Filter { next ->
        { req -> next(req.uri(req.lookupMatchingBaseUri())) }
    }
}
