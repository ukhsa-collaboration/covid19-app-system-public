package uk.nhs.nhsx.diagnosiskeydist;

import uk.nhs.nhsx.core.Environment;
import uk.nhs.nhsx.core.aws.s3.BucketName;
import uk.nhs.nhsx.core.aws.ssm.ParameterName;

public class BatchProcessingConfig {

    public final boolean shouldAbortOutsideTimeWindow;
    public final BucketName zipBucketName;
    public final String cloudFrontDistributionId;
    public final String distributionPatternDaily;
    public final String distributionPattern2Hourly;
    public final ParameterName ssmAGSigningKeyParameterName;
    public final ParameterName ssmMetaDataSigningKeyParameterName;

    public BatchProcessingConfig(boolean shouldAbortOutsideTimeWindow,
                                 BucketName zipBucketName,
                                 String cloudFrontDistributionId,
                                 String distributionPatternDaily,
                                 String distributionPattern2Hourly,
                                 ParameterName ssmAGSigningKeyParameterName,
                                 ParameterName ssmMetaDataSigningKeyParameterName) {
        this.shouldAbortOutsideTimeWindow = shouldAbortOutsideTimeWindow;
        this.zipBucketName = zipBucketName;
        this.cloudFrontDistributionId = cloudFrontDistributionId;
        this.distributionPatternDaily = distributionPatternDaily;
        this.distributionPattern2Hourly = distributionPattern2Hourly;
        this.ssmAGSigningKeyParameterName = ssmAGSigningKeyParameterName;
        this.ssmMetaDataSigningKeyParameterName = ssmMetaDataSigningKeyParameterName;
    }

    public static BatchProcessingConfig fromEnvironment(Environment e) {
        return new BatchProcessingConfig(
            Boolean.parseBoolean(e.access.required("ABORT_OUTSIDE_TIME_WINDOW")),
            BucketName.of(e.access.required("DISTRIBUTION_BUCKET_NAME")),
            e.access.required("DISTRIBUTION_ID"),
            e.access.required("DISTRIBUTION_PATTERN_DAILY"),
            e.access.required("DISTRIBUTION_PATTERN_2HOURLY"),
            ParameterName.of(e.access.required("SSM_AG_SIGNING_KEY_ID_PARAMETER_NAME")),
            ParameterName.of(e.access.required("SSM_METADATA_SIGNING_KEY_ID_PARAMETER_NAME")
            )
        );
    }
}
