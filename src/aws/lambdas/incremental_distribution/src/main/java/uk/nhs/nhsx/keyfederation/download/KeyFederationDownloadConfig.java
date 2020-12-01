package uk.nhs.nhsx.keyfederation.download;

import uk.nhs.nhsx.core.Environment;
import uk.nhs.nhsx.core.Environment.EnvironmentKey;
import uk.nhs.nhsx.core.FeatureFlag;
import uk.nhs.nhsx.core.StandardSigning;
import uk.nhs.nhsx.core.WorkspaceFeatureFlag;
import uk.nhs.nhsx.core.aws.s3.BucketName;
import uk.nhs.nhsx.core.aws.secretsmanager.SecretName;
import uk.nhs.nhsx.core.aws.ssm.ParameterName;

import java.util.List;

import static uk.nhs.nhsx.core.Environment.EnvironmentKey.*;

public class KeyFederationDownloadConfig {

    public final int maxSubsequentBatchDownloadCount;
    public final int initialDownloadHistoryDays;
    public final FeatureFlag downloadFeatureFlag;
    public final boolean downloadRiskLevelDefaultEnabled;
    public final int downloadRiskLevelDefault;
    public final BucketName submissionBucketName;
    public final String interopBaseUrl;
    public final SecretName interopAuthTokenSecretName;
    public final ParameterName signingKeyParameterName;
    public final String federatedKeyDownloadPrefix;
    public final String stateTableName;
    public final List<String> validOrigins;
    public final String region;

    public KeyFederationDownloadConfig(
        int maxSubsequentBatchDownloadCount,
        int initialDownloadHistoryDays,
        FeatureFlag downloadFeatureFlag,
        boolean downloadRiskLevelDefaultEnabled,
        int downloadRiskLevelDefault,
        BucketName submissionBucketName,
        String interopBaseUrl,
        SecretName interopAuthTokenSecretName,
        ParameterName signingKeyParameterName,
        String federatedKeyDownloadPrefix,
        String stateTableName,
        List<String> validOrigins,
        String region) {
        this.maxSubsequentBatchDownloadCount = maxSubsequentBatchDownloadCount;
        this.initialDownloadHistoryDays = initialDownloadHistoryDays;
        this.downloadFeatureFlag = downloadFeatureFlag;
        this.downloadRiskLevelDefault = downloadRiskLevelDefault;
        this.downloadRiskLevelDefaultEnabled = downloadRiskLevelDefaultEnabled;
        this.submissionBucketName = submissionBucketName;
        this.interopBaseUrl = interopBaseUrl;
        this.interopAuthTokenSecretName = interopAuthTokenSecretName;
        this.signingKeyParameterName = signingKeyParameterName;
        this.federatedKeyDownloadPrefix = federatedKeyDownloadPrefix;
        this.stateTableName = stateTableName;
        this.validOrigins = validOrigins;
        this.region = region;
    }

    private static final EnvironmentKey<Integer> MAX_SUBSEQUENT_BATCH_DOWNLOAD_COUNT = integer("MAX_SUBSEQUENT_BATCH_DOWNLOAD_COUNT");
    private static final EnvironmentKey<Integer> INITIAL_DOWNLOAD_HISTORY_DAYS = integer("INITIAL_DOWNLOAD_HISTORY_DAYS");
    private static final EnvironmentKey<List<String>> DOWNLOAD_ENABLED_WORKSPACES = strings("DOWNLOAD_ENABLED_WORKSPACES");
    private static final EnvironmentKey<Boolean> DOWNLOAD_RISK_LEVEL_DEFAULT_ENABLED = bool("DOWNLOAD_RISK_LEVEL_DEFAULT_ENABLED");
    private static final EnvironmentKey<Integer> DOWNLOAD_RISK_LEVEL_DEFAULT = integer("DOWNLOAD_RISK_LEVEL_DEFAULT");
    private static final EnvironmentKey<BucketName> SUBMISSION_BUCKET_NAME = value("SUBMISSION_BUCKET_NAME", BucketName.class);
    private static final EnvironmentKey<String> INTEROP_BASE_URL = string("INTEROP_BASE_URL");
    private static final EnvironmentKey<SecretName> INTEROP_AUTH_TOKEN_SECRET_NAME = value("INTEROP_AUTH_TOKEN_SECRET_NAME", SecretName.class);
    private static final EnvironmentKey<String> FEDERATED_KEY_DOWNLOAD_PREFIX = string("FEDERATED_KEY_DOWNLOAD_PREFIX");
    private static final EnvironmentKey<String> PROCESSOR_STATE_TABLE = string("PROCESSOR_STATE_TABLE");
    private static final EnvironmentKey<List<String>> VALID_DOWNLOAD_ORIGINS = strings("VALID_DOWNLOAD_ORIGINS");
    private static final EnvironmentKey<String> REGION = string("REGION");
    private static final EnvironmentKey<String> WORKSPACE = string("WORKSPACE");

    public static KeyFederationDownloadConfig fromEnvironment(Environment e) {
        return new KeyFederationDownloadConfig(
            e.access.required(MAX_SUBSEQUENT_BATCH_DOWNLOAD_COUNT),
            e.access.required(INITIAL_DOWNLOAD_HISTORY_DAYS),
            new WorkspaceFeatureFlag(
                e.access.required(WORKSPACE),
                e.access.required(DOWNLOAD_ENABLED_WORKSPACES)
            ),
            e.access.required(DOWNLOAD_RISK_LEVEL_DEFAULT_ENABLED),
            e.access.required(DOWNLOAD_RISK_LEVEL_DEFAULT),
            e.access.required(SUBMISSION_BUCKET_NAME),
            e.access.required(INTEROP_BASE_URL),
            e.access.required(INTEROP_AUTH_TOKEN_SECRET_NAME),
            e.access.required(StandardSigning.SSM_KEY_ID_PARAMETER_NAME),
            e.access.required(FEDERATED_KEY_DOWNLOAD_PREFIX),
            e.access.required(PROCESSOR_STATE_TABLE),
            e.access.required(VALID_DOWNLOAD_ORIGINS),
            e.access.required(REGION)
        );
    }
}
