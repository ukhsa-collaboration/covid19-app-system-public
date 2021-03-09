package uk.nhs.nhsx.keyfederation.upload

import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.Environment.Companion.WORKSPACE
import uk.nhs.nhsx.core.Environment.EnvironmentKey
import uk.nhs.nhsx.core.FeatureFlag
import uk.nhs.nhsx.core.StandardSigning
import uk.nhs.nhsx.core.WorkspaceFeatureFlag
import uk.nhs.nhsx.core.aws.secretsmanager.SecretName
import uk.nhs.nhsx.core.aws.ssm.ParameterName

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
    val stateTableName: String,
    val region: String,
    val federatedKeyUploadPrefixes: List<String>
) {
    companion object {
        private val MAX_SUBSEQUENT_BATCH_UPLOAD_COUNT =
            EnvironmentKey.integer("MAX_SUBSEQUENT_BATCH_UPLOAD_COUNT")
        private val INITIAL_UPLOAD_HISTORY_DAYS = EnvironmentKey.integer("INITIAL_UPLOAD_HISTORY_DAYS")
        private val MAX_UPLOAD_BATCH_SIZE = EnvironmentKey.integer("MAX_UPLOAD_BATCH_SIZE")
        private val UPLOAD_ENABLED_WORKSPACES = EnvironmentKey.strings("UPLOAD_ENABLED_WORKSPACES")
        private val UPLOAD_RISK_LEVEL_DEFAULT_ENABLED =
            EnvironmentKey.bool("UPLOAD_RISK_LEVEL_DEFAULT_ENABLED")
        private val UPLOAD_RISK_LEVEL_DEFAULT = EnvironmentKey.integer("UPLOAD_RISK_LEVEL_DEFAULT")
        private val INTEROP_BASE_URL = EnvironmentKey.string("INTEROP_BASE_URL")
        private val INTEROP_AUTH_TOKEN_SECRET_NAME =
            EnvironmentKey.value("INTEROP_AUTH_TOKEN_SECRET_NAME", SecretName)
        private val PROCESSOR_STATE_TABLE = EnvironmentKey.string("PROCESSOR_STATE_TABLE")
        private val REGION = EnvironmentKey.string("REGION")
        private val FEDERATED_KEY_UPLOAD_PREFIXES = EnvironmentKey.strings("FEDERATED_KEY_UPLOAD_PREFIXES")
        fun fromEnvironment(e: Environment): KeyFederationUploadConfig {
            return KeyFederationUploadConfig(
                e.access.required(MAX_SUBSEQUENT_BATCH_UPLOAD_COUNT),
                e.access.required(INITIAL_UPLOAD_HISTORY_DAYS),
                e.access.required(MAX_UPLOAD_BATCH_SIZE),
                WorkspaceFeatureFlag(e.access.required(WORKSPACE), e.access.required(UPLOAD_ENABLED_WORKSPACES)),
                e.access.required(UPLOAD_RISK_LEVEL_DEFAULT_ENABLED),
                e.access.required(UPLOAD_RISK_LEVEL_DEFAULT),
                e.access.required(INTEROP_BASE_URL),
                e.access.required(INTEROP_AUTH_TOKEN_SECRET_NAME),
                e.access.required(StandardSigning.SSM_KEY_ID_PARAMETER_NAME),
                e.access.required(PROCESSOR_STATE_TABLE),
                e.access.required(REGION),
                e.access.required(FEDERATED_KEY_UPLOAD_PREFIXES)
            )
        }
    }
}
