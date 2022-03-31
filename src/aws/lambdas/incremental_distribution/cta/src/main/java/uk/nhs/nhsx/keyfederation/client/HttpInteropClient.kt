package uk.nhs.nhsx.keyfederation.client

import uk.nhs.nhsx.core.Json
import uk.nhs.nhsx.core.Json.toJson
import uk.nhs.nhsx.core.RandomUUID
import uk.nhs.nhsx.core.UniqueId
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.IncomingHttpResponse
import uk.nhs.nhsx.core.events.OutgoingHttpRequest
import uk.nhs.nhsx.core.events.UnprocessableJson
import uk.nhs.nhsx.core.readJsonOrNull
import uk.nhs.nhsx.domain.BatchTag
import uk.nhs.nhsx.keyfederation.upload.JWS
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandlers
import java.time.LocalDate

class HttpInteropClient(
    private val interopBaseUrl: String,
    private val authToken: String,
    private val jws: JWS,
    private val events: Events,
    private val uniqueId: UniqueId = RandomUUID
): InteropClient {
    private val client: HttpClient = HttpClient.newHttpClient()

    override fun downloadKeys(date: LocalDate, batchTag: BatchTag?): InteropDownloadResponse {
        val base = "$interopBaseUrl/diagnosiskeys/download/${date}"
        val query = batchTag?.let { "?batchTag=${batchTag}" }.orEmpty()
        return downloadKeys(URI.create(base + query))
    }

    private fun downloadKeys(downloadKeysUri: URI): InteropDownloadResponse {
        val request = HttpRequest.newBuilder()
            .header("Authorization", "Bearer $authToken")
            .uri(downloadKeysUri)
            .build()

        val response = client.send(request, BodyHandlers.ofString())
        val statusCode = response.statusCode()

        events(OutgoingHttpRequest(request.uri().toString(), request.method(), statusCode))

        return when (statusCode) {
            200 -> response.parse<DiagnosisKeysDownloadResponse>()
            204 -> NoContent
            else -> error("""Request to download keys from federated key server with batch tag ${request.uri()} failed with status code $statusCode""")
        }
    }

    override fun uploadKeys(keys: List<ExposureUpload>): InteropUploadResponse {
        val payload = toJson(keys)
        val requestBody = DiagnosisKeysUploadRequest(BatchTag.of(uniqueId().toString()), jws.sign(payload))

        val request = HttpRequest.newBuilder()
            .header("Authorization", "Bearer $authToken")
            .header("Content-Type", "application/json")
            .uri(URI.create("$interopBaseUrl/diagnosiskeys/upload"))
            .POST(BodyPublishers.ofString(toJson(requestBody)))
            .build()

        val response = client.send(request, BodyHandlers.ofString())
        val statusCode = response.statusCode()

        events(IncomingHttpResponse(statusCode, response.body()))

        return when (statusCode) {
            200 -> response.parse()
            else -> error("Request to upload keys to federated key server failed with status code $statusCode")
        }
    }

    private inline fun <reified T> HttpResponse<String>.parse(): T =
        Json.readJsonOrNull(body()) { e -> events(UnprocessableJson(e)) }
            ?: error("Unable to parse response from key federation server")
}
