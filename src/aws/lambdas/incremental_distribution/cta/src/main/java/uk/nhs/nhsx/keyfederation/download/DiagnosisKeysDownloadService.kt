package uk.nhs.nhsx.keyfederation.download

import com.amazonaws.services.lambda.runtime.Context
import uk.nhs.nhsx.core.Clock
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.InfoEvent
import uk.nhs.nhsx.domain.BatchTag
import uk.nhs.nhsx.keyfederation.BatchTagService
import uk.nhs.nhsx.keyfederation.DownloadedExposures
import uk.nhs.nhsx.keyfederation.FederatedKeyUploader
import uk.nhs.nhsx.keyfederation.FederationBatch
import uk.nhs.nhsx.keyfederation.InteropClient
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.max

class DiagnosisKeysDownloadService(
    private val clock: Clock,
    private val interopClient: InteropClient,
    private val keyUploader: FederatedKeyUploader,
    private val batchTagService: BatchTagService,
    private val downloadRiskLevelDefaultEnabled: Boolean,
    private val downloadRiskLevelDefault: Int,
    private val initialDownloadHistoryDays: Int,
    private val maxSubsequentBatchDownloadCount: Int,
    private val context: Context,
    private val events: Events
) {

    fun downloadFromFederatedServerAndStoreKeys() = batchTagService
        .latestFederationBatch()
        ?.let {
            downloadKeysAndProcess(
                it.batchDate,
                it.batchTag,
                maxSubsequentBatchDownloadCount,
                context
            )
        }
        ?: downloadKeysAndProcess(
            dateNow().minusDays(initialDownloadHistoryDays.toLong()),
            null,
            maxSubsequentBatchDownloadCount,
            context
        )

    private fun downloadKeysAndProcess(
        date: LocalDate,
        batchTag: BatchTag?,
        maxBatchDownloadCount: Int,
        context: Context
    ): Int {
        var processedBatches = 0

        var exposureKeysNextBatch = if (batchTag == null) {
            interopClient.downloadKeys(date)
        } else {
            interopClient.downloadKeys(date, batchTag)
        }

        var iterationDuration = 0L
        var i = 1
        while (i <= maxBatchDownloadCount && exposureKeysNextBatch is DiagnosisKeysDownloadResponse) {
            val startTime = clock().toEpochMilli()
            val diagnosisKeysDownloadResponse = exposureKeysNextBatch
            convertAndSaveKeys(diagnosisKeysDownloadResponse)
            events(
                DownloadedExposures(
                    diagnosisKeysDownloadResponse.exposures.size,
                    diagnosisKeysDownloadResponse.batchTag,
                    i
                )
            )
            processedBatches++
            iterationDuration = max(iterationDuration, clock().toEpochMilli() - startTime)
            if (iterationDuration >= context.remainingTimeInMillis) {
                break
            }
            exposureKeysNextBatch = interopClient.downloadKeys(date, diagnosisKeysDownloadResponse.batchTag)
            i++
        }
        events(
            InfoEvent("Downloaded keys from federated server finished, batchCount=$processedBatches")
        )
        return processedBatches
    }

    private fun dateNow(): LocalDate = LocalDate.ofInstant(clock(), ZoneId.of("UTC"))

    private fun convertAndSaveKeys(diagnosisKeysDownloadResponse: DiagnosisKeysDownloadResponse) {
        val transformedResponse = DiagnosisKeysDownloadResponse(
            diagnosisKeysDownloadResponse.batchTag,
            diagnosisKeysDownloadResponse.exposures.map(::postDownloadTransformations)
        )
        keyUploader.acceptKeysFromFederatedServer(transformedResponse)
        batchTagService.updateLatestFederationBatch(
            FederationBatch(
                transformedResponse.batchTag,
                dateNow()
            )
        )
    }

    fun postDownloadTransformations(downloaded: ExposureDownload): ExposureDownload =
        ExposureDownload(
            downloaded.keyData,
            downloaded.rollingStartNumber,
            if (downloadRiskLevelDefaultEnabled) downloadRiskLevelDefault else downloaded.transmissionRiskLevel,
            downloaded.rollingPeriod,
            downloaded.origin,
            downloaded.regions,
            downloaded.testType,
            downloaded.reportType,
            downloaded.daysSinceOnset
        )
}
