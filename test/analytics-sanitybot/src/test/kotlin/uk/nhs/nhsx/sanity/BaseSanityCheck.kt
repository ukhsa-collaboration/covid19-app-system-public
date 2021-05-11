package uk.nhs.nhsx.sanity

import uk.nhs.nhsx.sanity.stores.config.EnvironmentConfiguration
import java.io.File

abstract class BaseSanityCheck {
    @Suppress("unused")
    companion object {
        private val pathname get() = System.getenv("SANITY_TEST_CONFIG") ?: "../../out/gen/config/analytics/test_config_branch.json"
        val env = EnvironmentConfiguration.from(File(pathname).also { println("Loading test config from ${it.absolutePath}") })
    }
}
