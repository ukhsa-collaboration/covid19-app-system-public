package uk.nhs.nhsx.keyfederation;

import uk.nhs.nhsx.keyfederation.upload.lookup.UploadKeysResult;

import java.util.Optional;

public interface BatchTagService {

    BatchTag getLatestBatchTag();

    void updateLatestBatchTag(BatchTag batchTag);

    Optional<UploadKeysResult> getLastUploadState();

    void updateLastUploadState(String lastUploadState);

}
