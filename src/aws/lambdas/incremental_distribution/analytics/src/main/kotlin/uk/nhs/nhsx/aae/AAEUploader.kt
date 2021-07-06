package uk.nhs.nhsx.aae

import org.apache.http.ssl.SSLContexts
import uk.nhs.nhsx.core.aws.secretsmanager.SecretManager
import uk.nhs.nhsx.core.aws.secretsmanager.SecretName
import uk.nhs.nhsx.core.aws.secretsmanager.SecretValue
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.OutgoingHttpRequest
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.KeyStore
import java.time.Duration

class AAEUploader(private val config: AAEUploadConfig, secretManager: SecretManager, private val events: Events) {
    private val subscription = secretManager
        .getSecret(SecretName.of(config.subscriptionKeySecretName))
        .map(SecretValue::value)
        .orElseThrow { RuntimeException("subscriptionKey does not exits in secret manager") }

    private val httpClient = createMutualAuthHttpClient(secretManager, config)

    fun uploadFile(filename: String, content: ByteArray, contentType: String) {
        val uploadRequest = HttpRequest.newBuilder()
            .header(SUBSCRIPTION_HEADER_NAME, subscription)
            .header("Content-Type", contentType)
            .uri(URI.create(config.aaeUrlPrefix + filename + config.aaeUrlSuffix))
            .PUT(HttpRequest.BodyPublishers.ofByteArray(content))
            .build()
        val httpResponse = httpClient.send(uploadRequest, HttpResponse.BodyHandlers.ofString())

        events(
            OutgoingHttpRequest(uploadRequest.uri().toString(), uploadRequest.method(), httpResponse.statusCode())
        )
        if (httpResponse.statusCode() != AAE_UPLOAD_SUCCESS_HTTP_RESPONSE_CODE) {
            throw RuntimeException("Unexpected HTTP response code " + httpResponse.statusCode())
        }
    }

    companion object {
        private val HTTP_PUT_TIME_OUT: Duration = Duration.ofSeconds(10)
        const val AAE_UPLOAD_SUCCESS_HTTP_RESPONSE_CODE = 201
        const val SUBSCRIPTION_HEADER_NAME = "Ocp-Apim-Subscription-Key"

        private fun createMutualAuthHttpClient(secretManager: SecretManager, config: AAEUploadConfig): HttpClient {
            val p12Certificate = secretManager
                .getSecretBinary(SecretName.of(config.p12CertificateSecretName))
            val p12CertificatePassword = secretManager
                .getSecret(SecretName.of(config.p12CertificatePasswordSecretName))
                .map { it.value.toCharArray() }
                .orElseThrow { RuntimeException("p12CertificatePassword does not exits in secret manager") }
            return createMutualAuthHttpClient(
                ByteArrayInputStream(p12Certificate),
                p12CertificatePassword
            )
        }

        private fun createMutualAuthHttpClient(
            p12CertInputStream: InputStream,
            p12CertPassword: CharArray
        ): HttpClient {
            val tks = KeyStore.getInstance("PKCS12")
            tks.load(p12CertInputStream, p12CertPassword)
            val sslContext = SSLContexts.custom()
                .loadKeyMaterial(tks, p12CertPassword)
                .build()
            return HttpClient.newBuilder()
                .connectTimeout(HTTP_PUT_TIME_OUT)
                .sslContext(sslContext)
                .build()
        }
    }
}
