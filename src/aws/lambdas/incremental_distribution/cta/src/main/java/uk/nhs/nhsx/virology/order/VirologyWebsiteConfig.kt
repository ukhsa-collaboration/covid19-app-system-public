package uk.nhs.nhsx.virology.order

import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.Environment.EnvironmentKey

data class VirologyWebsiteConfig(val orderWebsite: String,
                                 val registerWebsite: String) {

    companion object {
        private val ORDER_WEBSITE = EnvironmentKey.string("order_website")
        private val REGISTER_WEBSITE = EnvironmentKey.string("register_website")

        fun fromEnvironment(environment: Environment): VirologyWebsiteConfig = VirologyWebsiteConfig(
            environment.access.required(ORDER_WEBSITE),
            environment.access.required(REGISTER_WEBSITE)
        )
    }
}
