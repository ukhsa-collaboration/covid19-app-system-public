package uk.nhs.nhsx.keyfederation.download;

import uk.nhs.nhsx.diagnosiskeyssubmission.model.ClientTemporaryExposureKey;
import uk.nhs.nhsx.diagnosiskeyssubmission.model.ClientTemporaryExposureKeysPayload;
import uk.nhs.nhsx.keyfederation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static java.util.stream.Collectors.toList;

public class DiagnosisKeysDownloadService {

    private final InteropClient interopClient;
    private final KeyUploader keyUploader;
    private final BatchTagService batchTagService;

    public DiagnosisKeysDownloadService(InteropClient interopClient, KeyUploader keyUploader, BatchTagService batchTagService) {
        this.interopClient = interopClient;
        this.keyUploader = keyUploader;
        this.batchTagService = batchTagService;
    }

    public void downloadAndSave(LocalDate date) throws Exception {
        interopClient.downloadKeys(date, batchTagService.getLatestBatchTag())
            .forEach(this::convertAndSaveKeys);
    }

    private void convertAndSaveKeys(DiagnosisKeysDownloadResponse diagnosisKeysDownloadResponse) {
        keyUploader.acceptKeysFromFederatedServer(convert(diagnosisKeysDownloadResponse), diagnosisKeysDownloadResponse.batchTag);
        batchTagService.updateLatestBatchTag(BatchTag.of(diagnosisKeysDownloadResponse.batchTag));
    }

    public ClientTemporaryExposureKeysPayload convert(DiagnosisKeysDownloadResponse diagnosisKeysDownloadResponse) {
        List<ClientTemporaryExposureKey> temporaryExposureKeys = diagnosisKeysDownloadResponse.exposures.stream()
            .map(this::convert)
            .collect(toList());
        return new ClientTemporaryExposureKeysPayload(UUID.randomUUID(), temporaryExposureKeys);
    }

    private ClientTemporaryExposureKey convert(Exposure exposure) {
        return new ClientTemporaryExposureKey(exposure.keyData, exposure.rollingStartNumber, exposure.rollingPeriod);
    }

}
