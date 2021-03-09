package uk.nhs.nhsx.keyfederation.download

import com.amazonaws.services.lambda.runtime.Context
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.InfoEvent
import uk.nhs.nhsx.keyfederation.*
import uk.nhs.nhsx.keyfederation.BatchTag.Companion.of
import uk.nhs.nhsx.keyfederation.FederationBatch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.function.Supplier
import java.util.stream.Collectors

class DiagnosisKeysDownloadService(private val clock: Supplier<Instant>,
                                   private val interopClient: InteropClient,
                                   private val keyUploader: FederatedKeyUploader,
                                   private val batchTagService: BatchTagService,
                                   private val downloadRiskLevelDefaultEnabled: Boolean,
                                   private val downloadRiskLevelDefault: Int,
                                   private val initialDownloadHistoryDays: Int,
                                   private val maxSubsequentBatchDownloadCount: Int,
                                   private val context: Context,
                                   private val events: Events) {

    fun downloadFromFederatedServerAndStoreKeys(): Int = batchTagService.latestFederationBatch()
        .map { downloadKeysAndProcess(it.batchDate, it.batchTag, maxSubsequentBatchDownloadCount, context) }
        .orElseGet { downloadKeysAndProcess(dateNow().minusDays(initialDownloadHistoryDays.toLong()), null, maxSubsequentBatchDownloadCount, context) }

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
            val startTime = clock.get().toEpochMilli()
            val diagnosisKeysDownloadResponse = exposureKeysNextBatch
            convertAndSaveKeys(diagnosisKeysDownloadResponse)
            events.emit(javaClass, DownloadedExposures(diagnosisKeysDownloadResponse.exposures.size,
                diagnosisKeysDownloadResponse.batchTag,
                i))
            processedBatches++
            iterationDuration = Math.max(iterationDuration, clock.get().toEpochMilli() - startTime)
            if (iterationDuration >= context.remainingTimeInMillis) {
                break
            }
            exposureKeysNextBatch = interopClient.downloadKeys(date, BatchTag.of(diagnosisKeysDownloadResponse.batchTag))
            i++
        }
        events.emit(javaClass, InfoEvent("Downloaded keys from federated server finished, batchCount=$processedBatches"))
        return processedBatches
    }

    private fun dateNow(): LocalDate = LocalDate.ofInstant(clock.get(), ZoneId.of("UTC"))

    private fun convertAndSaveKeys(diagnosisKeysDownloadResponse: DiagnosisKeysDownloadResponse) {
        val transformedResponse = DiagnosisKeysDownloadResponse(
            diagnosisKeysDownloadResponse.batchTag,
            diagnosisKeysDownloadResponse.exposures.stream().map { downloaded: ExposureDownload -> postDownloadTransformations(downloaded) }.collect(Collectors.toList())
        )
        keyUploader.acceptKeysFromFederatedServer(transformedResponse)
        batchTagService.updateLatestFederationBatch(
            FederationBatch(
                of(transformedResponse.batchTag),
                dateNow()
            )
        )
    }

    fun postDownloadTransformations(downloaded: ExposureDownload): ExposureDownload =
        ExposureDownload(downloaded.keyData,
            downloaded.rollingStartNumber,
            if (downloadRiskLevelDefaultEnabled) downloadRiskLevelDefault else downloaded.transmissionRiskLevel,
            downloaded.rollingPeriod,
            downloaded.origin,
            downloaded.regions,
            downloaded.testType,
            downloaded.reportType,
            downloaded.daysSinceOnset)
}
