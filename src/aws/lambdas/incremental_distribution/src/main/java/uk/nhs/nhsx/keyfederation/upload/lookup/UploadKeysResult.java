package uk.nhs.nhsx.keyfederation.upload.lookup;

public class UploadKeysResult {
    public final String lastUploadedTimeStamp;

    public UploadKeysResult(String lastUploadedTimeStamp) {
        this.lastUploadedTimeStamp = lastUploadedTimeStamp;
    }
}
