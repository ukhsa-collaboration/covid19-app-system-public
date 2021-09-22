package contract.infra

import org.http4k.core.ContentType
import org.http4k.core.HttpMessage
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.format.Jackson
import org.http4k.servirtium.InteractionOptions
import java.util.*

class MobileAppInteractionOptions : InteractionOptions {

    /**
     * this method modifies the requests from the mobile before they are recorded to disk.
     */
    override fun modify(request: Request): Request {
        val keepOnlyHeaders = super.modify(request)
            .keepOnlyHeaders("Content-Type")
            .tidyJsonBody()

        return request.header("Authorization")
            ?.let { keepOnlyHeaders.replaceHeader("Authorization", "Bearer TOKEN") } ?: keepOnlyHeaders
    }

    /**
     * this method modifies the responses from the backend before they are recorded to disk.
     */
    override fun modify(response: Response): Response = super.modify(response)
        .keepOnlyHeaders("Content-Type", "Content-Length", "x-amz-meta-signature", "x-amz-meta-signature-date").tidyJsonBody()

    /**
     * Diagnosis key distribution ZIP files
     */
    override fun isBinary(contentType: ContentType?) = contentType?.value?.contains("application/zip") == true
}

@Suppress("UNCHECKED_CAST")
fun <T : HttpMessage> T.tidyJsonBody() = when {
    header("Content-Type")?.contains("json") == true -> {
        body(Jackson.prettify(bodyString()))
    }
    else -> this
} as T

@Suppress("UNCHECKED_CAST")
private fun <T : HttpMessage> T.keepOnlyHeaders(vararg toKeep: String): T {
    val filteredHeaders = headers.filter { it.first.lowercase(Locale.getDefault()) in toKeep.map { it.lowercase(Locale.getDefault()) } }
    return headers.fold(this) { acc, next -> acc.removeHeader(next.first) as T }.headers(filteredHeaders) as T
}
