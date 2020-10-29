package uk.nhs.nhsx.core;

import uk.nhs.nhsx.core.aws.s3.BucketName;

import static uk.nhs.nhsx.core.Environment.EnvironmentKey.string;
import static uk.nhs.nhsx.core.Environment.EnvironmentKey.value;

public class EnvironmentKeys {
    
    public static final Environment.EnvironmentKey<BucketName> BUCKET_NAME = value("BUCKET_NAME", BucketName.class);
    public static final Environment.EnvironmentKey<String> DISTRIBUTION_ID = string("DISTRIBUTION_ID");
    public static final Environment.EnvironmentKey<String> DISTRIBUTION_INVALIDATION_PATTERN = string("DISTRIBUTION_INVALIDATION_PATTERN");
    public static final Environment.EnvironmentKey<String> SUBMISSIONS_TOKENS_TABLE = string("submission_tokens_table");
    public static final Environment.EnvironmentKey<BucketName> SUBMISSION_STORE = value("SUBMISSION_STORE", BucketName.class);
    public static final Environment.EnvironmentKey<String> SSM_CIRCUIT_BREAKER_BASE_NAME = string("SSM_CIRCUIT_BREAKER_BASE_NAME");
}
