package uk.nhs.nhsx.keyfederation.upload;

import uk.nhs.nhsx.core.Environment;
import uk.nhs.nhsx.core.Environment.EnvironmentKey;
import uk.nhs.nhsx.core.FeatureFlag;
import uk.nhs.nhsx.core.WorkspaceFeatureFlag;
import uk.nhs.nhsx.core.StandardSigning;
import uk.nhs.nhsx.core.aws.s3.BucketName;
import uk.nhs.nhsx.core.aws.secretsmanager.SecretName;
import uk.nhs.nhsx.core.aws.ssm.ParameterName;

import java.util.List;

import static uk.nhs.nhsx.core.Environment.EnvironmentKey.*;

public class KeyFederationUploadConfig {

    public final int maxSubsequentBatchUploadCount;
    public final int initialUploadHistoryDays;
    public final int maxUploadBatchSize;
    public final FeatureFlag uploadFeatureFlag;
    public final boolean uploadRiskLevelDefaultEnabled;
    public final int uploadRiskLevelDefault;
    public final BucketName submissionBucketName;
    public final String interopBaseUrl;
    public final SecretName interopAuthTokenSecretName;
    public final ParameterName signingKeyParameterName;
    public final String stateTableName;
    public final String region;
    public final List<String> federatedKeyUploadPrefixes;

    public KeyFederationUploadConfig(
        int maxSubsequentBatchUploadCount,
        int initialUploadHistoryDays,
        int maxUploadBatchSize,
        FeatureFlag uploadFeatureFlag,
        boolean uploadRiskLevelDefaultEnabled,
        int uploadRiskLevelDefault,
        BucketName submissionBucketName,
        String interopBaseUrl,
        SecretName interopAuthTokenSecretName,
        ParameterName signingKeyParameterName,
        String stateTableName,
        String region,
        List<String> federatedKeyUploadPrefixes) {
        this.maxSubsequentBatchUploadCount = maxSubsequentBatchUploadCount;
        this.maxUploadBatchSize = maxUploadBatchSize;
        this.initialUploadHistoryDays = initialUploadHistoryDays;
        this.uploadFeatureFlag = uploadFeatureFlag;
        this.uploadRiskLevelDefault = uploadRiskLevelDefault;
        this.uploadRiskLevelDefaultEnabled = uploadRiskLevelDefaultEnabled;
        this.submissionBucketName = submissionBucketName;
        this.interopBaseUrl = interopBaseUrl;
        this.interopAuthTokenSecretName = interopAuthTokenSecretName;
        this.signingKeyParameterName = signingKeyParameterName;
        this.stateTableName = stateTableName;
        this.region = region;
        this.federatedKeyUploadPrefixes = federatedKeyUploadPrefixes;
    }

    private static final EnvironmentKey<Integer> MAX_SUBSEQUENT_BATCH_UPLOAD_COUNT = integer("MAX_SUBSEQUENT_BATCH_UPLOAD_COUNT");
    private static final EnvironmentKey<Integer> INITIAL_UPLOAD_HISTORY_DAYS = integer("INITIAL_UPLOAD_HISTORY_DAYS");
    private static final EnvironmentKey<Integer> MAX_UPLOAD_BATCH_SIZE = integer("MAX_UPLOAD_BATCH_SIZE");
    private static final EnvironmentKey<List<String>> UPLOAD_ENABLED_WORKSPACES = strings("UPLOAD_ENABLED_WORKSPACES");
    private static final EnvironmentKey<Boolean> UPLOAD_RISK_LEVEL_DEFAULT_ENABLED = bool("UPLOAD_RISK_LEVEL_DEFAULT_ENABLED");
    private static final EnvironmentKey<Integer> UPLOAD_RISK_LEVEL_DEFAULT = integer("UPLOAD_RISK_LEVEL_DEFAULT");
    private static final EnvironmentKey<BucketName> SUBMISSION_BUCKET_NAME = value("SUBMISSION_BUCKET_NAME", BucketName.class);
    private static final EnvironmentKey<String> INTEROP_BASE_URL = string("INTEROP_BASE_URL");
    private static final EnvironmentKey<SecretName> INTEROP_AUTH_TOKEN_SECRET_NAME = value("INTEROP_AUTH_TOKEN_SECRET_NAME", SecretName.class);
    private static final EnvironmentKey<String> PROCESSOR_STATE_TABLE = string("PROCESSOR_STATE_TABLE");
    private static final EnvironmentKey<String> REGION = string("REGION");
    private static final EnvironmentKey<List<String>> FEDERATED_KEY_UPLOAD_PREFIXES = strings("FEDERATED_KEY_UPLOAD_PREFIXES");
    private static final EnvironmentKey<String> WORKSPACE = string("WORKSPACE");

    public static KeyFederationUploadConfig fromEnvironment(Environment e) {
        return new KeyFederationUploadConfig(
            e.access.required(MAX_SUBSEQUENT_BATCH_UPLOAD_COUNT),
            e.access.required(INITIAL_UPLOAD_HISTORY_DAYS),
            e.access.required(MAX_UPLOAD_BATCH_SIZE),
            new WorkspaceFeatureFlag(
                e.access.required(WORKSPACE),
                e.access.required(UPLOAD_ENABLED_WORKSPACES)
            ),
            e.access.required(UPLOAD_RISK_LEVEL_DEFAULT_ENABLED),
            e.access.required(UPLOAD_RISK_LEVEL_DEFAULT),
            e.access.required(SUBMISSION_BUCKET_NAME),
            e.access.required(INTEROP_BASE_URL),
            e.access.required(INTEROP_AUTH_TOKEN_SECRET_NAME),
            e.access.required(StandardSigning.SSM_KEY_ID_PARAMETER_NAME),
            e.access.required(PROCESSOR_STATE_TABLE),
            e.access.required(REGION),
            e.access.required(FEDERATED_KEY_UPLOAD_PREFIXES)
        );
    }
}
