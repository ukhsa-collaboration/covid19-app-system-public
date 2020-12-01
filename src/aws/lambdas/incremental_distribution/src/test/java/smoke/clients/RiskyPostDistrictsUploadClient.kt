package smoke.clients

import org.apache.logging.log4j.LogManager
import org.http4k.client.JavaHttpClient
import org.http4k.core.ContentType
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import smoke.env.EnvConfig

class RiskyPostDistrictsUploadClient(private val client: JavaHttpClient,
                                     private val config: EnvConfig) {

    companion object {
        private val logger = LogManager.getLogger(RiskyPostDistrictsUploadClient::class.java)

        fun baseUrlFrom(config: EnvConfig) = config.riskyPostDistrictsUploadEndpoint
    }

    fun upload(csv: String) {
        logger.info("upload")

        val uri = baseUrlFrom(config)

        val request = Request(Method.POST, uri)
            .header("Authorization", config.authHeaders.highRiskPostCodeUpload)
            .header("Content-Type", ContentType("text/csv").value)
            .body(csv)

        val response = client(request)

        response
            .requireStatusCode(Status.ACCEPTED)
            .requireBodyText("successfully uploaded")
    }
}