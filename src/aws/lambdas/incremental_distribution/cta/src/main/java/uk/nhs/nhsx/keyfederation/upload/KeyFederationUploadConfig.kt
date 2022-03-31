package uk.nhs.nhsx.keyfederation.upload

import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.Environment.Companion.WORKSPACE
import uk.nhs.nhsx.core.Environment.EnvironmentKey
import uk.nhs.nhsx.core.FeatureFlag
import uk.nhs.nhsx.core.StandardSigning
import uk.nhs.nhsx.core.WorkspaceFeatureFlag
import uk.nhs.nhsx.core.aws.dynamodb.TableName
import uk.nhs.nhsx.core.aws.secretsmanager.SecretName
import uk.nhs.nhsx.core.aws.ssm.ParameterName
import java.time.Duration

class KeyFederationUploadConfig(
    val maxSubsequentBatchUploadCount: Int,
    val initialUploadHistoryDays: Int,
    val maxUploadBatchSize: Int,
    val uploadFeatureFlag: FeatureFlag,
    val uploadRiskLevelDefaultEnabled: Boolean,
    val uploadRiskLevelDefault: Int,
    val interopBaseUrl: String,
    val interopAuthTokenSecretName: SecretName,
    val signingKeyParameterName: ParameterName,
    val stateTableName: TableName,
    val region: String,
    val federatedKeyUploadPrefixes: List<String>,
    val loadSubmissionsTimeout: Duration,
    val loadSubmissionsThreadPoolSize: Int
) {
    companion object {
        private val MAX_SUBSEQUENT_BATCH_UPLOAD_COUNT = EnvironmentKey.integer("MAX_SUBSEQUENT_BATCH_UPLOAD_COUNT")
        private val INITIAL_UPLOAD_HISTORY_DAYS = EnvironmentKey.integer("INITIAL_UPLOAD_HISTORY_DAYS")
        private val MAX_UPLOAD_BATCH_SIZE = EnvironmentKey.integer("MAX_UPLOAD_BATCH_SIZE")
        private val UPLOAD_ENABLED_WORKSPACES = EnvironmentKey.strings("UPLOAD_ENABLED_WORKSPACES")
        private val UPLOAD_RISK_LEVEL_DEFAULT_ENABLED = EnvironmentKey.bool("UPLOAD_RISK_LEVEL_DEFAULT_ENABLED")
        private val UPLOAD_RISK_LEVEL_DEFAULT = EnvironmentKey.integer("UPLOAD_RISK_LEVEL_DEFAULT")
        private val INTEROP_BASE_URL = EnvironmentKey.string("INTEROP_BASE_URL")
        private val INTEROP_AUTH_TOKEN_SECRET_NAME = EnvironmentKey.value("INTEROP_AUTH_TOKEN_SECRET_NAME", SecretName)
        private val PROCESSOR_STATE_TABLE = EnvironmentKey.value("PROCESSOR_STATE_TABLE", TableName)
        private val REGION = EnvironmentKey.string("REGION")
        private val FEDERATED_KEY_UPLOAD_PREFIXES = EnvironmentKey.strings("FEDERATED_KEY_UPLOAD_PREFIXES")
        private val LOAD_SUBMISSIONS_TIMEOUT = EnvironmentKey.duration("LOAD_SUBMISSIONS_TIMEOUT")
        private val LOAD_SUBMISSIONS_THREAD_POOL_SIZE = EnvironmentKey.integer("LOAD_SUBMISSIONS_THREAD_POOL_SIZE")

        fun fromEnvironment(e: Environment) = KeyFederationUploadConfig(
            maxSubsequentBatchUploadCount = e.access.required(MAX_SUBSEQUENT_BATCH_UPLOAD_COUNT),
            initialUploadHistoryDays = e.access.required(INITIAL_UPLOAD_HISTORY_DAYS),
            maxUploadBatchSize = e.access.required(MAX_UPLOAD_BATCH_SIZE),
            uploadFeatureFlag = WorkspaceFeatureFlag(e.access.required(WORKSPACE), e.access.required(UPLOAD_ENABLED_WORKSPACES)),
            uploadRiskLevelDefaultEnabled = e.access.required(UPLOAD_RISK_LEVEL_DEFAULT_ENABLED),
            uploadRiskLevelDefault = e.access.required(UPLOAD_RISK_LEVEL_DEFAULT),
            interopBaseUrl = e.access.required(INTEROP_BASE_URL),
            interopAuthTokenSecretName = e.access.required(INTEROP_AUTH_TOKEN_SECRET_NAME),
            signingKeyParameterName = e.access.required(StandardSigning.SSM_KEY_ID_PARAMETER_NAME),
            stateTableName = e.access.required(PROCESSOR_STATE_TABLE),
            region = e.access.required(REGION),
            federatedKeyUploadPrefixes = e.access.required(FEDERATED_KEY_UPLOAD_PREFIXES),
            loadSubmissionsTimeout = e.access.defaulted(LOAD_SUBMISSIONS_TIMEOUT) { Duration.ofMinutes(12) },
            loadSubmissionsThreadPoolSize = e.access.defaulted(LOAD_SUBMISSIONS_THREAD_POOL_SIZE) { 15 },
        )
    }
}
