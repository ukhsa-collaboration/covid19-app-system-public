package uk.nhs.nhsx.virology.tokengen;

import org.apache.http.Consts;
import org.apache.http.entity.ContentType;
import uk.nhs.nhsx.core.aws.s3.*;

public class VirologyProcessorStore {

    private final S3Storage s3Client;
    private final BucketName bucketName;

    public VirologyProcessorStore(S3Storage s3Client, BucketName bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    public void storeCsv(CtaTokensCsv ctaTokensCsv) {
        s3Client.upload(
            Locator.of(bucketName, ObjectKey.of(ctaTokensCsv.filename)),
            ContentType.create("text/csv", Consts.UTF_8),
            ByteArraySource.fromUtf8String(ctaTokensCsv.content)
        );
    }

    public void storeZip(CtaTokensZip ctaTokensZip) {
        s3Client.upload(
            Locator.of(bucketName, ObjectKey.of(ctaTokensZip.filename)),
            ContentType.create("application/zip"),
            ByteArraySource.fromFile(ctaTokensZip.content)
        );
    }
}
