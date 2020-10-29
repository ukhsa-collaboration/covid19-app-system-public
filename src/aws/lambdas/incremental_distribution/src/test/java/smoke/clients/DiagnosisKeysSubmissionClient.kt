package smoke.clients

import org.http4k.client.JavaHttpClient
import org.http4k.core.ContentType
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import smoke.env.EnvConfig
import uk.nhs.nhsx.core.Jackson
import uk.nhs.nhsx.diagnosiskeyssubmission.model.ClientTemporaryExposureKey
import uk.nhs.nhsx.diagnosiskeyssubmission.model.ClientTemporaryExposureKeysPayload
import java.time.*
import java.util.*

class DiagnosisKeysSubmissionClient(private val client: JavaHttpClient,
                                    private val config: EnvConfig) {


    companion object {
        fun baseUrlFrom(config: EnvConfig) = config.diagnosisKeysSubmissionEndpoint
    }

    fun sendTempExposureKeys(payload: ClientTemporaryExposureKeysPayload) {
        val request = Request(Method.POST, baseUrlFrom(config))
            .header("Authorization", config.authHeaders.mobile)
            .header("Content-Type", ContentType.APPLICATION_JSON.value)
            .body(Jackson.toJson(payload))

        client(request)
            .requireStatusCode(Status.OK)
            .requireSignatureHeaders()
            .requireNoPayload()
    }

    fun createKeysPayload(diagnosisKeySubmissionToken: String,
                          encodedKeyData: List<String>,
                          rollingPeriod: Int = 144): ClientTemporaryExposureKeysPayload =
        ClientTemporaryExposureKeysPayload(
            UUID.fromString(diagnosisKeySubmissionToken),
            encodedKeyData.map {
                ClientTemporaryExposureKey(it, rollingStartNumber(), rollingPeriod)
            }
        )

    fun createKeysPayloadWithOnsetDays(diagnosisKeySubmissionToken: String,
                                       encodedKeyData: List<String>): ClientTemporaryExposureKeysPayload {

        fun exposureKeys(): List<ClientTemporaryExposureKey> {
            val tek1 = ClientTemporaryExposureKey(encodedKeyData[0], rollingStartNumber(), 144)
            tek1.daysSinceOnsetOfSymptoms = 0
            val tek2 = ClientTemporaryExposureKey(encodedKeyData[1], rollingStartNumber(), 144)
            tek2.daysSinceOnsetOfSymptoms = 3
            val tek3 = ClientTemporaryExposureKey(encodedKeyData[2], rollingStartNumber(), 144)
            return listOf(tek1, tek2, tek3)
        }

        return ClientTemporaryExposureKeysPayload(
            UUID.fromString(diagnosisKeySubmissionToken),
            exposureKeys()
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