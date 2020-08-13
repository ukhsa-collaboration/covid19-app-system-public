package uk.nhs.nhsx.highriskvenuesupload;

import uk.nhs.nhsx.core.aws.s3.BucketName;
import uk.nhs.nhsx.core.aws.s3.ObjectKey;
import uk.nhs.nhsx.core.aws.s3.S3Storage;

public class HighRiskVenuesUploadConfig {

    public final String cloudFrontDistId;
    public final String cloudFrontInvalidationPattern;
    public final S3Storage.Locator locator;

    public HighRiskVenuesUploadConfig(BucketName s3BucketName,
                                      ObjectKey s3ObjKey,
                                      String cloudFrontDistId,
                                      String cloudFrontInvalidationPattern) {

        this.locator = S3Storage.Locator.of(s3BucketName, s3ObjKey);
        this.cloudFrontDistId = cloudFrontDistId;
        this.cloudFrontInvalidationPattern = cloudFrontInvalidationPattern;
    }

}
