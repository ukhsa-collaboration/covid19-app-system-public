package uk.nhs.nhsx.keyfederation

import org.apache.http.entity.ContentType
import uk.nhs.nhsx.core.Jackson.toJson
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.ByteArraySource.Companion.fromUtf8String
import uk.nhs.nhsx.core.aws.s3.Locator
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.core.aws.s3.S3Storage
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.InfoEvent
import uk.nhs.nhsx.diagnosiskeydist.agspec.ENIntervalNumber.enIntervalNumberFromTimestamp
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKey
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKeyPayload
import uk.nhs.nhsx.keyfederation.download.DiagnosisKeysDownloadResponse
import uk.nhs.nhsx.keyfederation.download.ExposureDownload
import uk.nhs.nhsx.keyfederation.download.ExposureKeysPayload
import uk.nhs.nhsx.keyfederation.download.ReportType
import uk.nhs.nhsx.keyfederation.download.TestType
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.function.Supplier

class FederatedKeyUploader(
    private val s3Storage: S3Storage,
    private val bucketName: BucketName,
    private val federatedKeySourcePrefix: String,
    private val clock: Supplier<Instant>,
    private val validOrigins: List<String>,
    private val events: Events
) {

    private val dateStringProvider = { DATE_TIME_FORMATTER.format(clock.get()) }

    fun acceptKeysFromFederatedServer(payload: DiagnosisKeysDownloadResponse) =
        groupByOrigin(payload).forEach { (origin, keys) -> handleOriginKeys(payload.batchTag, origin, keys) }

    fun groupByOrigin(payload: DiagnosisKeysDownloadResponse): Map<String, List<ExposureDownload>> =
        payload.exposures
            .filter { it.testType === TestType.PCR && it.reportType === ReportType.CONFIRMED_TEST }
            .groupBy(ExposureDownload::origin)

    private fun handleOriginKeys(batchTag: String, origin: String, exposureDownloads: List<ExposureDownload>) {
        emitStatistics(exposureDownloads, origin)

        val validKeys = exposureDownloads.filter { isValidExposure(it) }
        when {
            validOrigins.contains(origin) -> {
                when {
                    validKeys.isNotEmpty() -> uploadOriginKeysToS3(
                        ExposureKeysPayload(
                            origin,
                            batchTag,
                            validKeys.map { StoredTemporaryExposureKeyTransform(it) }
                        )
                    )
                    else -> events(
                        javaClass,
                        InfoEvent("Skip store to s3 because no valid keys were found or all keys were invalid, origin=$origin, batchTag=${batchTag}")
                    )
                }
            }
            else -> events(javaClass, InvalidOriginKeys(origin, batchTag))
        }
    }

    private fun emitStatistics(exposureDownloads: List<ExposureDownload>, origin: String) {
        exposureDownloads
            .groupBy { it.testType }
            .forEach { (testType: TestType, exposures: List<ExposureDownload>) ->
                val validKeys = exposures.filter { isValidExposure(it) }
                val invalidKeys = exposures.size - validKeys.size
                events(
                    javaClass, DownloadedFederatedDiagnosisKeys(
                        testType,
                        validKeys.size,
                        invalidKeys,
                        origin
                    )
                )
            }
    }

    private fun isValidExposure(exposureDownload: ExposureDownload): Boolean = exposureDownload
        .takeIf { isKeyValid(it.keyData) }
        ?.takeIf { isRollingStartNumberValid(clock, it.rollingStartNumber.toLong(), it.rollingPeriod, events) }
        ?.takeIf { isRollingPeriodValid(it.rollingPeriod) }
        ?.takeIf { isTransmissionRiskLevelValid(it.transmissionRiskLevel) } != null

    private fun isKeyValid(key: String?): Boolean {
        val isValid = key != null && isBase64EncodedAndLessThan32Bytes(key)
        if (!isValid) events.emit(javaClass, InvalidTemporaryExposureKey(key))
        return isValid
    }

    private fun isRollingPeriodValid(isRollingPeriod: Int): Boolean {
        val isValid = isRollingPeriod in 1..144
        if (!isValid) events.emit(javaClass, InvalidRollingPeriod(isRollingPeriod))
        return isValid
    }

    private fun isBase64EncodedAndLessThan32Bytes(value: String): Boolean = try {
        Base64.getDecoder().decode(value).size < 32
    } catch (e: Exception) {
        false
    }

    private fun isTransmissionRiskLevelValid(transmissionRiskLevel: Int): Boolean {
        val isValid = transmissionRiskLevel in 0..7
        if (!isValid) events.emit(javaClass, InvalidTransmissionRiskLevel(transmissionRiskLevel))
        return isValid
    }

    private fun uploadOriginKeysToS3(exposureKeysPayload: ExposureKeysPayload) {
        val payload = StoredTemporaryExposureKeyPayload(exposureKeysPayload.temporaryExposureKeys)
        val objectKey =
            ObjectKey.of("""$federatedKeySourcePrefix/${exposureKeysPayload.origin}/${dateStringProvider()}/${exposureKeysPayload.batchTag}.json""")

        s3Storage.upload(
            Locator.of(bucketName, objectKey),
            ContentType.APPLICATION_JSON,
            fromUtf8String(toJson(payload))
        )
    }

    companion object {
        private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC)

        fun isRollingStartNumberValid(
            clock: Supplier<Instant>,
            rollingStartNumber: Long,
            rollingPeriod: Int,
            events: Events
        ): Boolean {
            val now = clock.get()
            val currentInstant = enIntervalNumberFromTimestamp(now).enIntervalNumber
            val expiryPeriod = enIntervalNumberFromTimestamp(now.minus(Duration.ofDays(14))).enIntervalNumber
            val isValid = rollingStartNumber + rollingPeriod >= expiryPeriod && rollingStartNumber <= currentInstant
            if (!isValid) {
                events(
                    FederatedKeyUploader::class.java,
                    InvalidRollingStartNumber(now, rollingStartNumber, rollingPeriod)
                )
            }
            return isValid
        }
    }
}

object StoredTemporaryExposureKeyTransform {
    operator fun invoke(input: ExposureDownload): StoredTemporaryExposureKey {
        return StoredTemporaryExposureKey(
            input.keyData,
            input.rollingStartNumber,
            input.rollingPeriod,
            input.transmissionRiskLevel,
            input.daysSinceOnset
        )
    }
}
