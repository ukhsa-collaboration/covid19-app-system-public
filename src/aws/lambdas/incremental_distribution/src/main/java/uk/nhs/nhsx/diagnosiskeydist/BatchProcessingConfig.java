package uk.nhs.nhsx.diagnosiskeydist;

import uk.nhs.nhsx.core.Environment;
import uk.nhs.nhsx.core.aws.s3.BucketName;
import uk.nhs.nhsx.core.aws.ssm.ParameterName;

import static uk.nhs.nhsx.core.Environment.EnvironmentKey.bool;
import static uk.nhs.nhsx.core.Environment.EnvironmentKey.string;
import static uk.nhs.nhsx.core.Environment.EnvironmentKey.value;

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

    private static final Environment.EnvironmentKey<Boolean> ABORT_OUTSIDE_TIME_WINDOW = bool("ABORT_OUTSIDE_TIME_WINDOW");
    private static final Environment.EnvironmentKey<BucketName> DISTRIBUTION_BUCKET_NAME = value("DISTRIBUTION_BUCKET_NAME", BucketName::of);
    private static final Environment.EnvironmentKey<String> DISTRIBUTION_ID = string("DISTRIBUTION_ID");
    private static final Environment.EnvironmentKey<String> DISTRIBUTION_PATTERN_DAILY = string("DISTRIBUTION_PATTERN_DAILY");
    private static final Environment.EnvironmentKey<String> DISTRIBUTION_PATTERN_2HOURLY = string("DISTRIBUTION_PATTERN_2HOURLY");
    private static final Environment.EnvironmentKey<ParameterName> SSM_AG_SIGNING_KEY_ID_PARAMETER_NAME = value("SSM_AG_SIGNING_KEY_ID_PARAMETER_NAME", ParameterName::of);
    private static final Environment.EnvironmentKey<ParameterName> SSM_METADATA_SIGNING_KEY_ID_PARAMETER_NAME = value("SSM_METADATA_SIGNING_KEY_ID_PARAMETER_NAME", ParameterName::of);

    public static BatchProcessingConfig fromEnvironment(Environment e) {
        return new BatchProcessingConfig(
            e.access.required(ABORT_OUTSIDE_TIME_WINDOW),
            e.access.required(DISTRIBUTION_BUCKET_NAME),
            e.access.required(DISTRIBUTION_ID),
            e.access.required(DISTRIBUTION_PATTERN_DAILY),
            e.access.required(DISTRIBUTION_PATTERN_2HOURLY),
            e.access.required(SSM_AG_SIGNING_KEY_ID_PARAMETER_NAME),
            e.access.required(SSM_METADATA_SIGNING_KEY_ID_PARAMETER_NAME)
        );
    }
}
