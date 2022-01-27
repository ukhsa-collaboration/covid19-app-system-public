package uk.nhs.nhsx.keyfederation.upload

import com.amazonaws.services.lambda.runtime.Context
import uk.nhs.nhsx.core.Clock
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.InfoEvent
import uk.nhs.nhsx.diagnosiskeydist.Submission
import uk.nhs.nhsx.diagnosiskeydist.SubmissionRepository
import uk.nhs.nhsx.keyfederation.BatchTagService
import uk.nhs.nhsx.keyfederation.DiagnosisKeysUploadIncomplete
import uk.nhs.nhsx.keyfederation.InteropClient
import uk.nhs.nhsx.keyfederation.InvalidTemporaryExposureKey
import uk.nhs.nhsx.keyfederation.UploadedDiagnosisKeys
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.math.max

class DiagnosisKeysUploadService(
    private val clock: Clock,
    private val interopClient: InteropClient,
    private val submissionRepository: SubmissionRepository,
    private val batchTagService: BatchTagService,
    private val exposureUploadFactory: ExposureUploadFactory,
    private val uploadRiskLevelDefaultEnabled: Boolean,
    private val uploadRiskLevelDefault: Int,
    private val initialUploadHistoryDays: Int,
    private val maxUploadBatchSize: Int,
    private val maxSubsequentBatchUploadCount: Int,
    private val context: Context,
    private val events: Events
) {
    private val maxUploadBatchLimit: Int = maxUploadBatchSize - 4 // mobile submissions/sec

    fun loadKeysAndUploadToFederatedServer(): Int {
        var submissionCount = 0
        var iterationDuration = 0L
        var lastUploadedSubmissionTime = getLastUploadedTime()

        for (i in 1..maxSubsequentBatchUploadCount) {
            val startTime = clock().toEpochMilli()
            val result = loadKeysAndUploadOneBatchToFederatedServer(lastUploadedSubmissionTime, i)

            submissionCount += result.submissionCount

            if (maxUploadBatchSize == NO_BATCH_SIZE_LIMIT_LEGACY_BATCH_SIZE
                || lastUploadedSubmissionTime == result.lastUploadedSubmissionTime
                || result.submissionCount < maxUploadBatchLimit
            ) break

            lastUploadedSubmissionTime = result.lastUploadedSubmissionTime
            iterationDuration = max(iterationDuration, clock().toEpochMilli() - startTime)
            val remainingTimeInMillis = context.remainingTimeInMillis
            if (iterationDuration >= remainingTimeInMillis) break
        }

        return submissionCount
    }

    private fun loadKeysAndUploadOneBatchToFederatedServer(
        lastUploadedSubmissionTime: Instant,
        batchNumber: Int
    ): BatchUploadResult {
        events(InfoEvent("Begin: Upload diagnosis keys to the Nearform server - batch $batchNumber"))

        val newSubmissions = submissionRepository.loadAllSubmissions(
            lastUploadedSubmissionTime.toEpochMilli(),
            if (maxUploadBatchSize == NO_BATCH_SIZE_LIMIT_LEGACY_BATCH_SIZE) Int.MAX_VALUE else maxUploadBatchLimit,
            if (maxUploadBatchSize == NO_BATCH_SIZE_LIMIT_LEGACY_BATCH_SIZE) Int.MAX_VALUE else maxUploadBatchSize
        )

        val exposures = getUploadRequestRawPayload(newSubmissions)
            .filter { isKeyValid(it.keyData) }
            .map { updateRiskLevelIfDefaultEnabled(it) }

        events(
            InfoEvent("Loading and transforming keys from submissions finished (from ${if (newSubmissions.isEmpty()) null else newSubmissions[0].submissionDate} to ${if (newSubmissions.isEmpty()) null else newSubmissions[newSubmissions.size - 1].submissionDate}), keyCount=${exposures.size} (batch $batchNumber)")
        )

        if (exposures.isNotEmpty()) {
            val uploadResponse = interopClient.uploadKeys(exposures)

            emitStatistics(exposures, lastUploadedSubmissionTime, batchNumber)

            if (uploadResponse.insertedExposures != exposures.size) {
                events(
                    DiagnosisKeysUploadIncomplete(
                        exposures.size,
                        uploadResponse.insertedExposures,
                        lastUploadedSubmissionTime,
                        batchNumber
                    )
                )
            }

            val updatedLastUploadedSubmissionTime = newSubmissions
                .map { it.submissionDate.toEpochMilli() }
                .maxOrNull()
                ?.let(Instant::ofEpochMilli)
                ?: throw RuntimeException()

            batchTagService.updateLastUploadState(updatedLastUploadedSubmissionTime)

            return BatchUploadResult(
                updatedLastUploadedSubmissionTime,
                newSubmissions.size
            )
        } else {
            events(
                InfoEvent("No keys were available for uploading to federation server with submission date greater than $lastUploadedSubmissionTime -batch $batchNumber")
            )
        }

        return BatchUploadResult(
            lastUploadedSubmissionTime,
            newSubmissions.size
        )
    }

    private fun emitStatistics(
        exposures: List<ExposureUpload>,
        lastUploadedSubmissionTime: Instant,
        batchNumber: Int
    ) {
        exposures
            .groupBy(ExposureUpload::testType)
            .forEach { (testType, exposures) ->
                events(
                    UploadedDiagnosisKeys(
                        testType,
                        exposures.size,
                        lastUploadedSubmissionTime,
                        batchNumber
                    )
                )
            }
    }

    fun updateRiskLevelIfDefaultEnabled(upload: ExposureUpload): ExposureUpload = ExposureUpload(
        upload.keyData,
        upload.rollingStartNumber,
        if (uploadRiskLevelDefaultEnabled) uploadRiskLevelDefault else upload.transmissionRiskLevel,
        upload.rollingPeriod,
        upload.regions,
        upload.testType,
        upload.reportType,
        upload.daysSinceOnset
    )

    private fun isKeyValid(key: String?): Boolean {
        val isValid = key != null && isBase64EncodedAndEqualTo16Bytes(key)
        if (!isValid) events(InvalidTemporaryExposureKey(key))
        return isValid
    }

    private fun isBase64EncodedAndEqualTo16Bytes(value: String): Boolean = try {
        val decodedKeyData = Base64.getDecoder().decode(value)
        decodedKeyData.size == 16
    } catch (e: Exception) {
        false
    }

    // FIXME filter expired keys (rollingStartNumber & rollingPeriod)
    private fun getUploadRequestRawPayload(submissions: List<Submission>): List<ExposureUpload> = submissions
        .flatMap { exposureUploadFactory.create(it) }

    private fun getLastUploadedTime() = batchTagService
        .lastUploadState()
        ?.let {
            events(InfoEvent("Last uploaded timestamp from db ${it.lastUploadedTimeStamp}"))
            Instant.ofEpochSecond(it.lastUploadedTimeStamp)
        }
        ?: clock().minus(Duration.ofDays(initialUploadHistoryDays.toLong()))

    companion object {
        private const val NO_BATCH_SIZE_LIMIT_LEGACY_BATCH_SIZE = 0
    }
}
