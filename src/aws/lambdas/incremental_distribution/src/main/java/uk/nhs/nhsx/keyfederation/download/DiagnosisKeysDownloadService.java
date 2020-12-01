package uk.nhs.nhsx.keyfederation.download;

import com.amazonaws.services.lambda.runtime.Context;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.nhs.nhsx.keyfederation.BatchTag;
import uk.nhs.nhsx.keyfederation.BatchTagService;
import uk.nhs.nhsx.keyfederation.FederatedKeyUploader;
import uk.nhs.nhsx.keyfederation.InteropClient;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class DiagnosisKeysDownloadService {
    private static final Logger logger = LogManager.getLogger(DiagnosisKeysDownloadService.class);

    private final Supplier<Instant> clock;
    private final InteropClient interopClient;
    private final FederatedKeyUploader keyUploader;
    private final BatchTagService batchTagService;
    private final boolean downloadRiskLevelDefaultEnabled;
    private final int downloadRiskLevelDefault;
    private final int initialDownloadHistoryDays;
    private final int maxSubsequentBatchDownloadCount;
    private final Context context;

    public DiagnosisKeysDownloadService(Supplier<Instant> clock,
                                        InteropClient interopClient,
                                        FederatedKeyUploader keyUploader,
                                        BatchTagService batchTagService,
                                        boolean downloadRiskLevelDefaultEnabled,
                                        int downloadRiskLevelDefault,
                                        int initialDownloadHistoryDays,
                                        int maxSubsequentBatchDownloadCount,
                                        Context context) {
        this.clock = clock;
        this.interopClient = interopClient;
        this.keyUploader = keyUploader;
        this.batchTagService = batchTagService;
        this.downloadRiskLevelDefaultEnabled = downloadRiskLevelDefaultEnabled;
        this.downloadRiskLevelDefault = downloadRiskLevelDefault;
        this.initialDownloadHistoryDays = initialDownloadHistoryDays;
        this.maxSubsequentBatchDownloadCount = maxSubsequentBatchDownloadCount;
        this.context = context;
    }

    public int downloadFromFederatedServerAndStoreKeys() {
        AtomicInteger processedBatches = new AtomicInteger();
        batchTagService
            .getLatestFederationBatch()
            .ifPresentOrElse(
                it -> processedBatches.set(processBatches(interopClient.downloadKeys(it.batchDate, it.batchTag, maxSubsequentBatchDownloadCount,context))),
                () -> processedBatches.set(processBatches(interopClient.downloadKeys(dateNow().minusDays(initialDownloadHistoryDays), null, maxSubsequentBatchDownloadCount,context)))
            );
        return processedBatches.get();
    }

    private LocalDate dateNow() {
        return LocalDate.ofInstant(clock.get(), ZoneId.of("UTC"));
    }

    private int processBatches(List<DiagnosisKeysDownloadResponse> batches) {
        if (!batches.isEmpty()) {
            batches.forEach(this::convertAndSaveKeys);
        } else {
            logger.info("No batches were found from federation server download");
        }

        return batches.size();
    }

    private void convertAndSaveKeys(DiagnosisKeysDownloadResponse diagnosisKeysDownloadResponse) {
        var transformedResponse = new DiagnosisKeysDownloadResponse(
            diagnosisKeysDownloadResponse.batchTag,
            diagnosisKeysDownloadResponse.exposures.stream().map(this::postDownloadTransformations).collect(Collectors.toList())
        );

        keyUploader.acceptKeysFromFederatedServer(transformedResponse);

        batchTagService.updateLatestFederationBatch(
            new BatchTagService.FederationBatch(
                BatchTag.of(transformedResponse.batchTag),
                dateNow()
            )
        );
    }

    public ExposureDownload postDownloadTransformations(ExposureDownload downloaded) {
        return new ExposureDownload(downloaded.keyData,
            downloaded.rollingStartNumber,
            downloadRiskLevelDefaultEnabled ? downloadRiskLevelDefault : downloaded.transmissionRiskLevel,
            downloaded.rollingPeriod,
            downloaded.origin,
            downloaded.regions);
    }
}
