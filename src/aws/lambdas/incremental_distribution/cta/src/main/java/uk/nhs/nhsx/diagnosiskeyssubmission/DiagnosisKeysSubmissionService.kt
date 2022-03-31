package uk.nhs.nhsx.diagnosiskeyssubmission

import com.amazonaws.services.dynamodbv2.document.Item
import uk.nhs.nhsx.core.Clock
import uk.nhs.nhsx.core.ContentType.Companion.APPLICATION_JSON
import uk.nhs.nhsx.core.Json.toJson
import uk.nhs.nhsx.core.aws.dynamodb.AwsDynamoClient
import uk.nhs.nhsx.core.aws.dynamodb.TableName
import uk.nhs.nhsx.core.aws.s3.AwsS3
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.ByteArraySource.Companion.fromUtf8String
import uk.nhs.nhsx.core.aws.s3.Locator
import uk.nhs.nhsx.core.aws.s3.ObjectKeyNameProvider
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.diagnosiskeydist.agspec.RollingStartNumber.isRollingStartNumberValid
import uk.nhs.nhsx.diagnosiskeyssubmission.model.ClientTemporaryExposureKey
import uk.nhs.nhsx.diagnosiskeyssubmission.model.ClientTemporaryExposureKeysPayload
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKey
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKeyPayload
import uk.nhs.nhsx.diagnosiskeyssubmission.model.ValidatedTemporaryExposureKeysPayload
import uk.nhs.nhsx.domain.TestKit
import uk.nhs.nhsx.domain.TestKit.LAB_RESULT
import uk.nhs.nhsx.keyfederation.InvalidRollingPeriod
import uk.nhs.nhsx.keyfederation.InvalidRollingStartNumber
import uk.nhs.nhsx.keyfederation.InvalidTemporaryExposureKey
import uk.nhs.nhsx.keyfederation.InvalidTransmissionRiskLevel
import java.util.*

class DiagnosisKeysSubmissionService(
    private val awsS3: AwsS3,
    private val awsDynamoClient: AwsDynamoClient,
    private val objectKeyNameProvider: ObjectKeyNameProvider,
    private val tableName: TableName,
    private val bucketName: BucketName,
    private val clock: Clock,
    private val events: Events
) {
    fun acceptTemporaryExposureKeys(payload: ClientTemporaryExposureKeysPayload) =
        allValidMaybe(payload)?.also(::acceptPayload)

    private fun allValidMaybe(payload: ClientTemporaryExposureKeysPayload): ValidatedTemporaryExposureKeysPayload? {
        with(payload) { if (temporaryExposureKeys.size > MAX_KEYS) return null }

        val validKeys = payload.temporaryExposureKeys
            .filterNotNull()
            .filter(::isValidKey)
            .map(::asStoredKey)

        val invalidKeysCount = payload.temporaryExposureKeys.size - validKeys.size
        events(DownloadedTemporaryExposureKeys(validKeys.size, invalidKeysCount))

        return when {
            validKeys.isNotEmpty() -> ValidatedTemporaryExposureKeysPayload(
                diagnosisKeySubmissionToken = payload.diagnosisKeySubmissionToken,
                temporaryExposureKeys = validKeys
            )
            else -> {
                events(EmptyTemporaryExposureKeys())
                null
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
            clock = { now },
            rollingStartNumber = rollingStartNumber,
            rollingPeriod = rollingPeriod
        )
        if (!isValid) events(InvalidRollingStartNumber(now, rollingStartNumber, rollingPeriod))
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
            ?.let(::testkitFrom)
            ?.also { storeKeysAndDeleteToken(it, payload) }
    }

    private fun testkitFrom(item: Item) = item.getString("testKit")?.let { TestKit.valueOf(it) } ?: LAB_RESULT

    private fun matchDiagnosisToken(token: UUID): Item? {
        val item = awsDynamoClient.getItem(
            tableName = tableName,
            hashKeyName = SUBMISSION_TOKENS_HASH_KEY,
            hashKeyValue = token.toString()
        )
        if (item == null) events(DiagnosisTokenNotFound(token))
        return item
    }

    private fun storeKeysAndDeleteToken(
        testKit: TestKit,
        payload: ValidatedTemporaryExposureKeysPayload
    ) {
        uploadToS3(testKit, payload)
        deleteToken(payload.diagnosisKeySubmissionToken)
    }

    private fun uploadToS3(
        testKit: TestKit,
        payload: ValidatedTemporaryExposureKeysPayload
    ) {
        val uploadPayload = StoredTemporaryExposureKeyPayload(payload.temporaryExposureKeys)
        val provider = TestKitAwareObjectKeyNameProvider(objectKeyNameProvider, testKit)
        val objectKey = provider.generateObjectKeyName().append(".json")
        awsS3.upload(
            locator = Locator.of(bucketName, objectKey),
            contentType = APPLICATION_JSON,
            bytes = fromUtf8String(toJson(uploadPayload))
        )
    }

    private fun asStoredKey(it: ClientTemporaryExposureKey) =
        StoredTemporaryExposureKey(
            key = it.key!!,
            rollingStartNumber = it.rollingStartNumber,
            rollingPeriod = it.rollingPeriod,
            transmissionRisk = it.transmissionRiskLevel,
            daysSinceOnsetOfSymptoms = it.daysSinceOnsetOfSymptoms
        )

    private fun deleteToken(diagnosisKeySubmissionToken: UUID) =
        awsDynamoClient.deleteItem(
            tableName = tableName,
            hashKeyName = SUBMISSION_TOKENS_HASH_KEY,
            hashKeyValue = diagnosisKeySubmissionToken.toString()
        )

    companion object {
        private const val SUBMISSION_TOKENS_HASH_KEY = "diagnosisKeySubmissionToken"
        private const val MAX_KEYS = 14
    }
}
