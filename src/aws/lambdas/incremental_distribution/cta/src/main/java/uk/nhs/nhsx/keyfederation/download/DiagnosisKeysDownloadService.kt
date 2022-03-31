package uk.nhs.nhsx.keyfederation.download

import com.amazonaws.services.lambda.runtime.Context
import uk.nhs.nhsx.core.Clock
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.InfoEvent
import uk.nhs.nhsx.domain.BatchTag
import uk.nhs.nhsx.keyfederation.DownloadedExposures
import uk.nhs.nhsx.keyfederation.FederatedKeyUploader
import uk.nhs.nhsx.keyfederation.client.DiagnosisKeysDownloadResponse
import uk.nhs.nhsx.keyfederation.client.InteropClient
import uk.nhs.nhsx.keyfederation.client.InteropDownloadResponse
import uk.nhs.nhsx.keyfederation.domain.FederationBatch
import uk.nhs.nhsx.keyfederation.domain.RemainingTimeScheduler
import uk.nhs.nhsx.keyfederation.storage.BatchTagService
import java.time.LocalDate
import java.time.ZoneOffset

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

    fun downloadFromFederatedServerAndStoreKeys(): Int {
        val latestFederationBatch = batchTagService.latestFederationBatch()
        val (date, batchTag) = when (latestFederationBatch) {
            null -> dateNow().minusDays(initialDownloadHistoryDays.toLong()) to null
            else -> latestFederationBatch.batchDate to latestFederationBatch.batchTag
        }
        return downloadKeysAndProcess(date, batchTag)
    }

    private fun downloadKeysAndProcess(
        date: LocalDate,
        batchTag: BatchTag?
    ): Int {

        val scheduler = RemainingTimeScheduler<InteropDownloadResponse>(context, clock)
        var next: InteropDownloadResponse? = interopClient.downloadKeys(date, batchTag)
        var processedBatches = 0

        for (idx in 1..maxSubsequentBatchDownloadCount) {
            if (next !is DiagnosisKeysDownloadResponse) break
            val nextBatchTag = next.batchTag

            convertAndSaveKeys(next)
            events(DownloadedExposures(next.exposures.size, nextBatchTag, idx))
            processedBatches++

            next = scheduler.runMaybe { interopClient.downloadKeys(date, nextBatchTag) }
        }

        events(InfoEvent("Downloaded keys from federated server finished, batchCount=$processedBatches"))

        return processedBatches
    }

    private fun dateNow() = LocalDate.ofInstant(clock(), ZoneOffset.UTC)

    private fun convertAndSaveKeys(diagnosisKeysDownloadResponse: DiagnosisKeysDownloadResponse) {
        val transformedResponse = DiagnosisKeysDownloadResponse(
            diagnosisKeysDownloadResponse.batchTag,
            diagnosisKeysDownloadResponse.exposures.map(::postDownloadTransformations)
        )
        keyUploader.acceptKeysFromFederatedServer(transformedResponse)
        batchTagService.updateLatestFederationBatch(FederationBatch(transformedResponse.batchTag, dateNow()))
    }

    fun postDownloadTransformations(downloaded: ExposureDownload) = ExposureDownload(
        keyData = downloaded.keyData,
        rollingStartNumber = downloaded.rollingStartNumber,
        transmissionRiskLevel = if (downloadRiskLevelDefaultEnabled) downloadRiskLevelDefault else downloaded.transmissionRiskLevel,
        rollingPeriod = downloaded.rollingPeriod,
        origin = downloaded.origin,
        regions = downloaded.regions,
        testType = downloaded.testType,
        reportType = downloaded.reportType,
        daysSinceOnset = downloaded.daysSinceOnset
    )
}
