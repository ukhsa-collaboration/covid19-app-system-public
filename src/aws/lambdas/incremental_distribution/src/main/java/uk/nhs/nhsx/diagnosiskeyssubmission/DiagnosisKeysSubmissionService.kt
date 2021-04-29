package uk.nhs.nhsx.diagnosiskeyssubmission

import com.amazonaws.services.dynamodbv2.document.Item
import uk.nhs.nhsx.core.Clock
import uk.nhs.nhsx.core.ContentType.Companion.APPLICATION_JSON
import uk.nhs.nhsx.core.Json.toJson
import uk.nhs.nhsx.core.aws.dynamodb.AwsDynamoClient
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.ByteArraySource.Companion.fromUtf8String
import uk.nhs.nhsx.core.aws.s3.Locator.Companion.of
import uk.nhs.nhsx.core.aws.s3.ObjectKeyNameProvider
import uk.nhs.nhsx.core.aws.s3.S3Storage
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.diagnosiskeydist.agspec.RollingStartNumber.isRollingStartNumberValid
import uk.nhs.nhsx.diagnosiskeyssubmission.model.ClientTemporaryExposureKey
import uk.nhs.nhsx.diagnosiskeyssubmission.model.ClientTemporaryExposureKeysPayload
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKey
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKeyPayload
import uk.nhs.nhsx.diagnosiskeyssubmission.model.ValidatedTemporaryExposureKeysPayload
import uk.nhs.nhsx.keyfederation.InvalidRollingPeriod
import uk.nhs.nhsx.keyfederation.InvalidRollingStartNumber
import uk.nhs.nhsx.keyfederation.InvalidTemporaryExposureKey
import uk.nhs.nhsx.keyfederation.InvalidTransmissionRiskLevel
import uk.nhs.nhsx.domain.TestKit
import uk.nhs.nhsx.domain.TestKit.LAB_RESULT
import java.util.*

class DiagnosisKeysSubmissionService(
    private val s3Storage: S3Storage,
    private val awsDynamoClient: AwsDynamoClient,
    private val objectKeyNameProvider: ObjectKeyNameProvider,
    private val tableName: String,
    private val bucketName: BucketName,
    private val clock: Clock,
    private val events: Events
) {
    fun acceptTemporaryExposureKeys(payload: ClientTemporaryExposureKeysPayload) =
        allValidMaybe(payload).ifPresent(::acceptPayload)

    private fun allValidMaybe(payload: ClientTemporaryExposureKeysPayload) =
        when {
            payload.temporaryExposureKeys.size > MAX_KEYS -> {
                events(TemporaryExposureKeysSubmissionOverflow(payload.temporaryExposureKeys.size, MAX_KEYS))
                Optional.empty()
            }
            else -> {
                val validKeys = payload.temporaryExposureKeys.filterNotNull().filter(::isValidKey).map(::asStoredKey)

                val invalidKeysCount = payload.temporaryExposureKeys.size - validKeys.size
                events(DownloadedTemporaryExposureKeys(validKeys.size, invalidKeysCount))

                when {
                    validKeys.isNotEmpty() -> Optional.of(
                        ValidatedTemporaryExposureKeysPayload(
                            payload.diagnosisKeySubmissionToken,
                            validKeys
                        )
                    )
                    else -> {
                        events(EmptyTemporaryExposureKeys())
                        Optional.empty()
                    }
                }
            }
        }

    private fun isValidKey(temporaryExposureKey: ClientTemporaryExposureKey) =
        temporaryExposureKey
            .takeIf { isKeyValid(it.key) }
            ?.takeIf { isRollingStartNumberValid(it.rollingStartNumber.toLong(), it.rollingPeriod) }
            ?.takeIf { isRollingPeriodValid(it.rollingPeriod) }
            ?.takeIf { isTransmissionRiskLevelValid(it.transmissionRiskLevel) } != null

    private fun isKeyValid(key: String?): Boolean {
        val isValid = key != null && isBase64EncodedAndLessThan32Bytes(key)
        if (!isValid) events(InvalidTemporaryExposureKey(key))
        return isValid
    }

    private fun isRollingStartNumberValid(rollingStartNumber: Long, rollingPeriod: Int): Boolean {
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

    private fun isRollingPeriodValid(rollingPeriod: Int): Boolean {
        val isValid = rollingPeriod in 1..144
        if (!isValid) events(InvalidRollingPeriod(rollingPeriod))
        return isValid
    }

    private fun isBase64EncodedAndLessThan32Bytes(value: String) = try {
        Base64.getDecoder().decode(value).size < 32
    } catch (e: IllegalArgumentException) {
        false
    }

    private fun isTransmissionRiskLevelValid(transmissionRiskLevel: Int): Boolean {
        val isValid = transmissionRiskLevel in 0..7
        if (!isValid) events(InvalidTransmissionRiskLevel(transmissionRiskLevel))
        return isValid
    }

    private fun acceptPayload(payload: ValidatedTemporaryExposureKeysPayload) {
        matchDiagnosisToken(payload.diagnosisKeySubmissionToken)
            .map(::testkitFrom)
            .ifPresent { storeKeysAndDeleteToken(it, payload) }
    }

    private fun testkitFrom(item: Item): TestKit = Optional.ofNullable(item["testKit"])
        .map(Any::toString)
        .map { TestKit.valueOf(it) }
        .orElse(LAB_RESULT)

    private fun matchDiagnosisToken(token: UUID): Optional<Item> {
        val item = awsDynamoClient.getItem(
            tableName,
            SUBMISSION_TOKENS_HASH_KEY,
            token.toString()
        )
        if (item == null) {
            events(DiagnosisTokenNotFound(token))
        }
        return Optional.ofNullable(item)
    }

    private fun storeKeysAndDeleteToken(testKit: TestKit, payload: ValidatedTemporaryExposureKeysPayload) {
        uploadToS3(testKit, payload)
        deleteToken(payload.diagnosisKeySubmissionToken)
    }

    private fun uploadToS3(testKit: TestKit, payload: ValidatedTemporaryExposureKeysPayload) {
        val uploadPayload = StoredTemporaryExposureKeyPayload(payload.temporaryExposureKeys)
        val provider = TestKitAwareObjectKeyNameProvider(objectKeyNameProvider, testKit)
        val objectKey = provider.generateObjectKeyName().append(".json")
        s3Storage.upload(
            of(bucketName, objectKey),
            APPLICATION_JSON,
            fromUtf8String(toJson(uploadPayload))
        )
    }

    private fun asStoredKey(it: ClientTemporaryExposureKey) =
        StoredTemporaryExposureKey(
            it.key!!,
            it.rollingStartNumber,
            it.rollingPeriod,
            it.transmissionRiskLevel,
            it.daysSinceOnsetOfSymptoms
        )

    private fun deleteToken(diagnosisKeySubmissionToken: UUID) = awsDynamoClient.deleteItem(
        tableName,
        SUBMISSION_TOKENS_HASH_KEY,
        diagnosisKeySubmissionToken.toString()
    )

    companion object {
        private const val SUBMISSION_TOKENS_HASH_KEY = "diagnosisKeySubmissionToken"
        private const val MAX_KEYS = 14
    }
}
