package uk.nhs.nhsx.analyticsedge.upload

import uk.nhs.nhsx.core.Environment

data class EdgeUploaderConfig(
    val targetUrl: String,
    val sasTokenSecretName: String
) {
    companion object {
        private val TARGET_URL = Environment.EnvironmentKey.string("TARGET_URL")
        private val SAS_TOKEN_SECRET_NAME = Environment.EnvironmentKey.string("SAS_TOKEN_SECRET_NAME")

        fun fromEnvironment(e: Environment) = EdgeUploaderConfig(
            e.access.required(TARGET_URL),
            e.access.required(SAS_TOKEN_SECRET_NAME)
        )
    }
}
