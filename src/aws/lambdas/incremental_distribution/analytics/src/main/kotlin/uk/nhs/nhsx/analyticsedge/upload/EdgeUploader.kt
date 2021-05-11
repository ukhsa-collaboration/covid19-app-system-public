package uk.nhs.nhsx.analyticsedge.upload

import org.apache.http.client.utils.URIBuilder
import uk.nhs.nhsx.analyticsexporter.ExportDestinationUploader
import uk.nhs.nhsx.core.aws.secretsmanager.SecretManager
import uk.nhs.nhsx.core.aws.secretsmanager.SecretName
import uk.nhs.nhsx.core.aws.secretsmanager.SecretValue
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.OutgoingHttpRequest
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class EdgeUploader(
    config: EdgeUploaderConfig,
    secretManager: SecretManager,
    private val events: Events
) : ExportDestinationUploader {

    private val targetUrl = config.targetUrl
        .let { if (it.endsWith("/")) it else "$it/" }

    private val sasToken = secretManager
        .getSecret(SecretName.of(config.sasTokenSecretName))
        .map(SecretValue::value)
        .orElseThrow { RuntimeException("sas token does not exits in secret manager") }

    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(HTTP_PUT_TIME_OUT)
        .build()

    override fun uploadFile(filename: String, content: ByteArray, contentType: String) {
        val uploadRequest = HttpRequest.newBuilder()
            .header("Content-Type", contentType)
            .header("x-ms-version", "2018-03-28")
            .header("x-ms-blob-type", "BlockBlob")
            .uri(URI.create("$targetUrl$filename?$sasToken"))
            .PUT(HttpRequest.BodyPublishers.ofByteArray(content))
            .build()
        val httpResponse = httpClient.send(uploadRequest, HttpResponse.BodyHandlers.ofString())
        events(
            OutgoingHttpRequest(
                uploadRequest.uri().let { uri -> URIBuilder(uri).removeQuery().build() }.toString(),
                uploadRequest.method(),
                httpResponse.statusCode()
            )
        )
        if (httpResponse.statusCode() != EDGE_UPLOAD_SUCCESS_HTTP_RESPONSE_CODE) {
            throw RuntimeException("Unexpected HTTP response code " + httpResponse.statusCode())
        }
    }

    companion object {
        private val HTTP_PUT_TIME_OUT: Duration = Duration.ofSeconds(10)
        const val EDGE_UPLOAD_SUCCESS_HTTP_RESPONSE_CODE = 201
    }
}
