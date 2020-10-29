package uk.nhs.nhsx.keyfederation;

import uk.nhs.nhsx.core.Environment;
import uk.nhs.nhsx.core.Environment.EnvironmentKey;
import uk.nhs.nhsx.core.aws.s3.BucketName;
import uk.nhs.nhsx.core.aws.secretsmanager.SecretName;

import java.util.List;

import static uk.nhs.nhsx.core.Environment.EnvironmentKey.*;

public class KeyFederationConfig {

    public final boolean downloadEnabled;
    public final boolean uploadEnabled;
    public final BucketName submissionBucketName;
    public final String interopBaseUrl;
    public final SecretName interopAuthTokenSecretName;
    public final SecretName interopPrivateKeySecretName;
    public final String federatedKeyPrefix;
    public final String stateTableName;
    public final List<String> validRegions;
    public final String region;

    public KeyFederationConfig(
        boolean downloadEnabled,
        boolean uploadEnabled,
        BucketName submissionBucketName,
        String interopBaseUrl,
        SecretName interopAuthTokenSecretName,
        SecretName interopPrivateKeySecretName,
        String federatedKeyPrefix,
        String stateTableName,
        List<String> validRegions,
        String region) {
        this.downloadEnabled = downloadEnabled;
        this.uploadEnabled = uploadEnabled;
        this.submissionBucketName = submissionBucketName;
        this.interopBaseUrl = interopBaseUrl;
        this.interopAuthTokenSecretName = interopAuthTokenSecretName;
        this.interopPrivateKeySecretName = interopPrivateKeySecretName;
        this.federatedKeyPrefix = federatedKeyPrefix;
        this.stateTableName = stateTableName;
        this.validRegions = validRegions;
        this.region = region;
    }

    private static final EnvironmentKey<Boolean> DOWNLOAD_ENABLED = bool("DOWNLOAD_ENABLED");
    private static final EnvironmentKey<Boolean> UPLOAD_ENABLED = bool("UPLOAD_ENABLED");
    private static final EnvironmentKey<BucketName> SUBMISSION_BUCKET_NAME = value("SUBMISSION_BUCKET_NAME", BucketName.class);
    private static final EnvironmentKey<String> INTEROP_BASE_URL = string("INTEROP_BASE_URL");
    private static final EnvironmentKey<SecretName> INTEROP_AUTH_TOKEN_SECRET_NAME = value("INTEROP_AUTH_TOKEN_SECRET_NAME", SecretName.class);
    private static final EnvironmentKey<SecretName> INTEROP_PRIVATE_KEY_SECRET_NAME = value("INTEROP_PRIVATE_KEY_SECRET_NAME", SecretName.class);
    private static final EnvironmentKey<String> FEDERATED_KEY_PREFIX = string("FEDERATED_KEY_PREFIX");
    private static final EnvironmentKey<String> PROCESSOR_STATE_TABLE = string("PROCESSOR_STATE_TABLE");
    private static final EnvironmentKey<List<String>> VALID_REGIONS = strings("VALID_REGIONS");
    private static final EnvironmentKey<String> REGION = string("REGION");
    
    public static KeyFederationConfig fromEnvironment(Environment e) {
        return new KeyFederationConfig(
            e.access.required(DOWNLOAD_ENABLED),
            e.access.required(UPLOAD_ENABLED),
            e.access.required(SUBMISSION_BUCKET_NAME),
            e.access.required(INTEROP_BASE_URL),
            e.access.required(INTEROP_AUTH_TOKEN_SECRET_NAME),
            e.access.required(INTEROP_PRIVATE_KEY_SECRET_NAME),
            e.access.required(FEDERATED_KEY_PREFIX),
            e.access.required(PROCESSOR_STATE_TABLE),
            e.access.required(VALID_REGIONS),
            e.access.required(REGION));
    }
}
