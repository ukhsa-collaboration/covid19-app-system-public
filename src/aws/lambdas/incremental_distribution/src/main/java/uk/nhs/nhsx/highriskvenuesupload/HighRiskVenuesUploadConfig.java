package uk.nhs.nhsx.highriskvenuesupload;

import uk.nhs.nhsx.core.aws.s3.BucketName;
import uk.nhs.nhsx.core.aws.s3.Locator;
import uk.nhs.nhsx.core.aws.s3.ObjectKey;

public class HighRiskVenuesUploadConfig {

    public final String cloudFrontDistId;
    public final String cloudFrontInvalidationPattern;
    public final Locator locator;

    public HighRiskVenuesUploadConfig(BucketName s3BucketName,
                                      ObjectKey s3ObjKey,
                                      String cloudFrontDistId,
                                      String cloudFrontInvalidationPattern) {

        this.locator = Locator.of(s3BucketName, s3ObjKey);
        this.cloudFrontDistId = cloudFrontDistId;
        this.cloudFrontInvalidationPattern = cloudFrontInvalidationPattern;
    }

}
