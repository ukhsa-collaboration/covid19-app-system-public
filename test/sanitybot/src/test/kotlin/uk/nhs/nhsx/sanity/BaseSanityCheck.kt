package uk.nhs.nhsx.sanity

import uk.nhs.nhsx.sanity.config.EnvironmentConfiguration
import java.io.File

abstract class BaseSanityCheck {
    @Suppress("unused")
    companion object {
        val pathname get() = System.getenv("SANITY_TEST_CONFIG") ?: "../../out/gen/config/test_config_branch.json"
        val env = EnvironmentConfiguration.from(File(pathname).also { println("Loading test config from ${it.absolutePath}") })
    }
}
