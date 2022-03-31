package uk.nhs.nhsx.keyfederation

import uk.nhs.nhsx.core.Clock
import uk.nhs.nhsx.core.ContentType.Companion.APPLICATION_JSON
import uk.nhs.nhsx.core.Json.toJson
import uk.nhs.nhsx.core.aws.s3.AwsS3
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.ByteArraySource.Companion.fromUtf8String
import uk.nhs.nhsx.core.aws.s3.Locator
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.InfoEvent
import uk.nhs.nhsx.diagnosiskeydist.agspec.RollingStartNumber.isRollingStartNumberValid
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKey
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKeyPayload
import uk.nhs.nhsx.domain.BatchTag
import uk.nhs.nhsx.domain.ReportType
import uk.nhs.nhsx.domain.TestType
import uk.nhs.nhsx.keyfederation.client.DiagnosisKeysDownloadResponse
import uk.nhs.nhsx.keyfederation.download.ExposureDownload
import uk.nhs.nhsx.keyfederation.download.ExposureKeysPayload
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

class FederatedKeyUploader(
    private val awsS3: AwsS3,
    private val bucketName: BucketName,
    private val federatedKeySourcePrefix: String,
    private val clock: Clock,
    private val validOrigins: List<String>,
    private val events: Events
) {

    private val dateStringProvider = { DATE_TIME_FORMATTER.format(clock()) }

    fun acceptKeysFromFederatedServer(payload: DiagnosisKeysDownloadResponse) =
        groupByOrigin(payload).forEach { (origin, keys) -> handleOriginKeys(payload.batchTag, origin, keys) }

    fun groupByOrigin(payload: DiagnosisKeysDownloadResponse): Map<String, List<ExposureDownload>> =
        payload.exposures
            .filter { it.testType === TestType.LAB_RESULT && it.reportType === ReportType.CONFIRMED_TEST }
            .groupBy(ExposureDownload::origin)

    private fun handleOriginKeys(batchTag: BatchTag, origin: String, exposureDownloads: List<ExposureDownload>) {
        emitStatistics(exposureDownloads, origin)

        val validKeys = exposureDownloads.filter(::isValidExposure)

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
                        InfoEvent("Skip store to s3 because no valid keys were found or all keys were invalid, origin=$origin, batchTag=${batchTag}")
                    )
                }
            }
            else -> events(InvalidOriginKeys(origin, batchTag))
        }
    }

    private fun emitStatistics(exposureDownloads: List<ExposureDownload>, origin: String) {
        exposureDownloads
            .groupBy { it.testType }
            .forEach { (testType: TestType, exposures: List<ExposureDownload>) ->
                val validKeys = exposures.filter { isValidExposure(it) }
                val invalidKeys = exposures.size - validKeys.size
                events(
                    DownloadedFederatedDiagnosisKeys(
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

    private fun isRollingStartNumberValid(
        clock: Clock,
        rollingStartNumber: Long,
        rollingPeriod: Int,
        events: Events
    ): Boolean {
        val now = clock()
        val isValid = isRollingStartNumberValid(
            { now },
            rollingStartNumber,
            rollingPeriod
        )
        if (!isValid) {
            events(InvalidRollingStartNumber(now, rollingStartNumber, rollingPeriod))
        }
        return isValid
    }

    private fun isKeyValid(key: String?): Boolean {
        val isValid = key != null && isBase64EncodedAndLessThan32Bytes(key)
        if (!isValid) events(InvalidTemporaryExposureKey(key))
        return isValid
    }

    private fun isRollingPeriodValid(isRollingPeriod: Int): Boolean {
        val isValid = isRollingPeriod in 1..144
        if (!isValid) events(InvalidRollingPeriod(isRollingPeriod))
        return isValid
    }

    private fun isBase64EncodedAndLessThan32Bytes(value: String): Boolean = try {
        Base64.getDecoder().decode(value).size < 32
    } catch (e: Exception) {
        false
    }

    private fun isTransmissionRiskLevelValid(transmissionRiskLevel: Int): Boolean {
        val isValid = transmissionRiskLevel in 0..7
        if (!isValid) events(InvalidTransmissionRiskLevel(transmissionRiskLevel))
        return isValid
    }

    private fun uploadOriginKeysToS3(exposureKeysPayload: ExposureKeysPayload) {
        val payload = StoredTemporaryExposureKeyPayload(exposureKeysPayload.temporaryExposureKeys)
        val objectKey =
            ObjectKey.of("""$federatedKeySourcePrefix/${exposureKeysPayload.origin}/${dateStringProvider()}/${exposureKeysPayload.batchTag}.json""")

        awsS3.upload(
            Locator.of(bucketName, objectKey),
            APPLICATION_JSON,
            fromUtf8String(toJson(payload))
        )
    }

    companion object {
        private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC)
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
