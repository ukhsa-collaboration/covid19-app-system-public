package smoke.clients

import org.http4k.client.JavaHttpClient
import org.http4k.core.ContentType
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.slf4j.LoggerFactory
import smoke.env.EnvConfig

class AnalyticsEventsSubmissionClient(private val client: JavaHttpClient, private val config: EnvConfig) {


    companion object {
        private val logger = LoggerFactory.getLogger(AnalyticsEventsSubmissionClient::class.java)

        fun baseUrlFrom(config: EnvConfig) = config.analyticsEventsSubmissionEndpoint
    }

    fun upload(json: String): Response {
        logger.info("uploadResult")

        val uri = baseUrlFrom(config)

        val request = Request(Method.POST, uri)
            .header("Authorization", config.authHeaders.mobile)
            .header("Content-Type", ContentType("text/json").value)
            .body(json)

        return client(request)
    }

}