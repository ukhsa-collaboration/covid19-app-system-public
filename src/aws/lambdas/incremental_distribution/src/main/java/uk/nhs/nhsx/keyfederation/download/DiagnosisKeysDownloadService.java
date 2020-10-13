package uk.nhs.nhsx.keyfederation.download;

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
import java.util.function.Supplier;

public class DiagnosisKeysDownloadService {

    private static final Logger logger = LogManager.getLogger(DiagnosisKeysDownloadService.class);

    private static final int MAX_AGE = 14;

    private final Supplier<Instant> clock;
    private final InteropClient interopClient;
    private final FederatedKeyUploader keyUploader;
    private final BatchTagService batchTagService;

    public DiagnosisKeysDownloadService(Supplier<Instant> clock, InteropClient interopClient, FederatedKeyUploader keyUploader, BatchTagService batchTagService) {
        this.clock = clock;
        this.interopClient = interopClient;
        this.keyUploader = keyUploader;
        this.batchTagService = batchTagService;
    }

    public void downloadAndSave() {
        batchTagService.getLatestFederationBatch().ifPresentOrElse(
            it -> processBatches(interopClient.downloadKeys(it.batchDate, it.batchTag)),
            () -> processBatches(interopClient.downloadKeys(dateNow().minusDays(MAX_AGE)))
        );
    }

    private LocalDate dateNow() {
        return LocalDate.ofInstant(clock.get(), ZoneId.of("UTC"));
    }

    private void processBatches(List<DiagnosisKeysDownloadResponse> batches) {
        if (!batches.isEmpty()) {
            batches.forEach(this::convertAndSaveKeys);
        } else {
            logger.info("No batches were found from federation server download");
        }
    }

    private void convertAndSaveKeys(DiagnosisKeysDownloadResponse diagnosisKeysDownloadResponse) {
        keyUploader.acceptKeysFromFederatedServer(diagnosisKeysDownloadResponse);
        batchTagService.updateLatestFederationBatch(
            new BatchTagService.FederationBatch(
                BatchTag.of(diagnosisKeysDownloadResponse.batchTag),
                dateNow()
            )
        );
    }

}
