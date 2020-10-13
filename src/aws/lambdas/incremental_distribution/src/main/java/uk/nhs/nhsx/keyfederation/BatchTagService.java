package uk.nhs.nhsx.keyfederation;

import uk.nhs.nhsx.keyfederation.upload.lookup.UploadKeysResult;

import java.time.LocalDate;
import java.util.Optional;

public interface BatchTagService {

    Optional<UploadKeysResult> getLastUploadState();

    void updateLastUploadState(Long lastUploadTimestamp);

    Optional<FederationBatch> getLatestFederationBatch();

    void updateLatestFederationBatch(FederationBatch batch);

    class FederationBatch {
        public final BatchTag batchTag;
        public final LocalDate batchDate;

        public FederationBatch(BatchTag batchTag, LocalDate batchDate) {
            this.batchTag = batchTag;
            this.batchDate = batchDate;
        }
    }
}
