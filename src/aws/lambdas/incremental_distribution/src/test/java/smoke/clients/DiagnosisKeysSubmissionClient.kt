package smoke.clients

import org.http4k.client.JavaHttpClient
import org.http4k.core.ContentType
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.slf4j.LoggerFactory
import smoke.env.EnvConfig
import uk.nhs.nhsx.core.Jackson
import uk.nhs.nhsx.diagnosiskeyssubmission.model.ClientTemporaryExposureKey
import uk.nhs.nhsx.diagnosiskeyssubmission.model.ClientTemporaryExposureKeysPayload
import uk.nhs.nhsx.testkitorder.order.TestOrderResponse
import java.time.*
import java.util.*

class DiagnosisKeysSubmissionClient(private val client: JavaHttpClient,
                                    private val config: EnvConfig) {


    companion object {
        private val logger = LoggerFactory.getLogger(DiagnosisKeysSubmissionClient::class.java)

        fun baseUrlFrom(config: EnvConfig) = config.diagnosisKeysSubmissionEndpoint
    }

    fun sendTempExposureKeys(testOrderResponse: TestOrderResponse, encodedSubmissionKeys: List<String>) {
        logger.info("submitTemporaryExposureKeys: $encodedSubmissionKeys")

        val payload = createKeysPayload(testOrderResponse.diagnosisKeySubmissionToken, encodedSubmissionKeys)

        val request = Request(Method.POST, baseUrlFrom(config))
            .header("Authorization", config.authHeaders.mobile)
            .header("Content-Type", ContentType.APPLICATION_JSON.value)
            .body(Jackson.toJson(payload))

        client(request)
            .requireStatusCode(Status.OK)
            .requireSignatureHeaders()
            .requireNoPayload()
    }

    private fun createKeysPayload(diagnosisKeySubmissionToken: String,
                                  encodedKeyData: List<String>): ClientTemporaryExposureKeysPayload {
        return ClientTemporaryExposureKeysPayload(
            UUID.fromString(diagnosisKeySubmissionToken),
            encodedKeyData.map {
                ClientTemporaryExposureKey(it, rollingStartNumber(), 144)
            }
        )
    }

    private fun rollingStartNumber(): Int {
        val utcDateTime = utcDateTime().toInstant(ZoneOffset.UTC)
        val rollingStartNumber = utcDateTime.epochSecond / Duration.ofMinutes(10).toSeconds()
        return rollingStartNumber.toInt()
    }

    private fun utcDateTime() = LocalDateTime
        .ofInstant(Instant.now(), ZoneId.of("UTC"))

}