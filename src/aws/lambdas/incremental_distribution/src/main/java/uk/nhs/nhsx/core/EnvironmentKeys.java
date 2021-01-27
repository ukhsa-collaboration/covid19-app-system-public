package uk.nhs.nhsx.core;

import uk.nhs.nhsx.core.Environment.EnvironmentKey;
import uk.nhs.nhsx.core.aws.s3.BucketName;

import static uk.nhs.nhsx.core.Environment.EnvironmentKey.*;

public class EnvironmentKeys {

    public static final EnvironmentKey<BucketName> SUBMISSION_BUCKET_NAME = value("SUBMISSION_BUCKET_NAME", BucketName::of);
    public static final EnvironmentKey<BucketName> BUCKET_NAME = value("BUCKET_NAME", BucketName::of);
    public static final EnvironmentKey<String> DISTRIBUTION_ID = string("DISTRIBUTION_ID");
    public static final EnvironmentKey<String> DISTRIBUTION_INVALIDATION_PATTERN = string("DISTRIBUTION_INVALIDATION_PATTERN");
    public static final EnvironmentKey<String> SUBMISSIONS_TOKENS_TABLE = string("submission_tokens_table");
    public static final EnvironmentKey<BucketName> SUBMISSION_STORE = value("SUBMISSION_STORE", BucketName::of);
    public static final EnvironmentKey<String> SSM_CIRCUIT_BREAKER_BASE_NAME = string("SSM_CIRCUIT_BREAKER_BASE_NAME");
}
