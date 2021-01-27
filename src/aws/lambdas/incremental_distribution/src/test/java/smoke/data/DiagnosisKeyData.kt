package smoke.data

import smoke.actors.DiagnosisKeySubmissionToken
import uk.nhs.nhsx.diagnosiskeyssubmission.model.ClientTemporaryExposureKey
import uk.nhs.nhsx.diagnosiskeyssubmission.model.ClientTemporaryExposureKeysPayload
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID

object DiagnosisKeyData {

    fun createKeysPayload(diagnosisKeySubmissionToken: DiagnosisKeySubmissionToken,
                          encodedKeyData: List<String>,
                          clock: Clock,
                          rollingPeriod: Int = 144): ClientTemporaryExposureKeysPayload =
        ClientTemporaryExposureKeysPayload(
            UUID.fromString(diagnosisKeySubmissionToken.value),
            encodedKeyData.map {
                ClientTemporaryExposureKey(it, rollingStartNumber(clock), rollingPeriod)
            }
        )

    fun createKeysPayloadWithOnsetDays(diagnosisKeySubmissionToken: DiagnosisKeySubmissionToken,
                                       encodedKeyData: List<String>,
                                       clock: Clock): ClientTemporaryExposureKeysPayload {

        fun exposureKeys(): List<ClientTemporaryExposureKey> {
            val tek1 = ClientTemporaryExposureKey(encodedKeyData[0], rollingStartNumber(clock), 144)
            tek1.daysSinceOnsetOfSymptoms = 0
            val tek2 = ClientTemporaryExposureKey(encodedKeyData[1], rollingStartNumber(clock), 144)
            tek2.daysSinceOnsetOfSymptoms = 3
            val tek3 = ClientTemporaryExposureKey(encodedKeyData[2], rollingStartNumber(clock), 144)
            return listOf(tek1, tek2, tek3)
        }

        return ClientTemporaryExposureKeysPayload(
            UUID.fromString(diagnosisKeySubmissionToken.value),
            exposureKeys()
        )
    }

    private fun rollingStartNumber(clock: Clock): Int {
        val utcDateTime = utcDateTime(clock).minusHours(1).toInstant(ZoneOffset.UTC)
        val rollingStartNumber = utcDateTime.epochSecond / Duration.ofMinutes(10).toSeconds()
        return rollingStartNumber.toInt()
    }

    private fun utcDateTime(clock: Clock) = LocalDateTime.ofInstant(Instant.now(clock), ZoneId.of("UTC"))

}
