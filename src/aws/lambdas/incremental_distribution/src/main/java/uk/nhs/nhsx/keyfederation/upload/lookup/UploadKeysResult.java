package uk.nhs.nhsx.keyfederation.upload.lookup;

public class UploadKeysResult {
    public final Long lastUploadedTimeStamp;

    public UploadKeysResult(Long lastUploadedTimeStamp) {
        this.lastUploadedTimeStamp = lastUploadedTimeStamp;
    }
}
