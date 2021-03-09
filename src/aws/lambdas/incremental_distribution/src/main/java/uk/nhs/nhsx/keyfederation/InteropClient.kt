package uk.nhs.nhsx.keyfederation

import uk.nhs.nhsx.core.Jackson.readOrNull
import uk.nhs.nhsx.core.Jackson.toJson
import uk.nhs.nhsx.core.UniqueId
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.OutgoingHttpRequest
import uk.nhs.nhsx.core.events.UnprocessableJson
import uk.nhs.nhsx.keyfederation.download.DiagnosisKeysDownloadResponse
import uk.nhs.nhsx.keyfederation.download.InteropDownloadResponse
import uk.nhs.nhsx.keyfederation.download.NoContent
import uk.nhs.nhsx.keyfederation.upload.DiagnosisKeysUploadRequest
import uk.nhs.nhsx.keyfederation.upload.ExposureUpload
import uk.nhs.nhsx.keyfederation.upload.InteropUploadResponse
import uk.nhs.nhsx.keyfederation.upload.JWS
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.function.Supplier

class InteropClient(
    private val interopBaseUrl: String,
    private val authToken: String,
    private val jws: JWS,
    private val events: Events,
    private val uniqueId: Supplier<UUID> = UniqueId.ID
) {
    private val client: HttpClient = HttpClient.newHttpClient()

    fun downloadKeys(date: LocalDate): InteropDownloadResponse =
        downloadKeys(URI.create("$interopBaseUrl/diagnosiskeys/download/${date.format(FORMATTER)}"))

    fun downloadKeys(date: LocalDate, batchTag: BatchTag): InteropDownloadResponse =
        downloadKeys(URI.create("$interopBaseUrl/diagnosiskeys/download/${date.format(FORMATTER)}?batchTag=${batchTag.value}"))

    private fun downloadKeys(downloadKeysUri: URI): InteropDownloadResponse {
        val request = HttpRequest.newBuilder()
            .header("Authorization", "Bearer $authToken")
            .uri(downloadKeysUri)
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        val statusCode = response.statusCode()

        events.emit(javaClass, OutgoingHttpRequest(request.uri().toString(), request.method(), statusCode))

        return when (statusCode) {
            200 -> {
                readOrNull<DiagnosisKeysDownloadResponse>(response.body()) { e ->
                    events.emit(javaClass, UnprocessableJson(e))
                } ?: throw RuntimeException()
            }
            204 -> NoContent
            else -> throw RuntimeException("""Request to download keys from federated key server with batch tag ${request.uri()} failed with status code $statusCode""")
        }
    }

    fun uploadKeys(keys: List<ExposureUpload>): InteropUploadResponse {
        return try {
            val payload = toJson(keys)
            val requestBody = DiagnosisKeysUploadRequest(uniqueId.get().toString(), jws.sign(payload))
            val uploadRequest = HttpRequest.newBuilder()
                .header("Authorization", "Bearer $authToken")
                .header("Content-Type", "application/json")
                .uri(URI.create("$interopBaseUrl/diagnosiskeys/upload"))
                .POST(BodyPublishers.ofString(toJson(requestBody)))
                .build()
            val httpResponse = client.send(uploadRequest, HttpResponse.BodyHandlers.ofString())
            when (val statusCode = httpResponse.statusCode()) {
                200 -> {
                    readOrNull<InteropUploadResponse>(httpResponse.body()) { e ->
                        events.emit(javaClass, UnprocessableJson(e))
                    } ?: throw RuntimeException()
                }
                else -> throw RuntimeException("Request to upload keys to federated key server failed with status code $statusCode")
            }
        } catch (e: InterruptedException) {
            throw RuntimeException("Request to upload keys to federated key server failed", e)
        } catch (e: IOException) {
            throw RuntimeException("Request to upload keys to federated key server failed", e)
        }
    }

    companion object {
        private val FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }

}
