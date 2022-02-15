package uk.nhs.nhsx.sanity

import uk.nhs.nhsx.sanity.stores.config.EnvironmentConfiguration
import java.io.File

abstract class BaseSanityCheck {
    @Suppress("unused")
    companion object {
        private val pathname get() = System.getenv("SANITY_TEST_CONFIG") ?: "../../out/gen/config/analytics/test_config_branch.json"
        val env = EnvironmentConfiguration.from(File(pathname).also { println("Loading test config from ${it.absolutePath}") })

        val account = System.getenv("ACCOUNT") ?: setDefaultAccountWithWarning()
        val targetWorkspace = System.getenv("TARGET_WORKSPACE") ?: setDefaultTargetEnvironmentWithWarning()

        private fun setDefaultAccountWithWarning(): String {
            println("WARNING: `ACCOUNT` env var not set, using aa-dev to run Athena checks")
            return "aa-dev"
        }

        private fun setDefaultTargetEnvironmentWithWarning(): String {
            println("WARNING: `TARGET_WORKSPACE` env var not set, using aa-ci to run Athena checks")
            return "aa-ci"
        }
    }
}
