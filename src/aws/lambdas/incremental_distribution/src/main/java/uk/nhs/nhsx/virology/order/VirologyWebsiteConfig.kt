package uk.nhs.nhsx.virology.order

import uk.nhs.nhsx.core.Environment

data class VirologyWebsiteConfig(@JvmField val orderWebsite: String,
                                 @JvmField val registerWebsite: String) {

    companion object {

        private val ORDER_WEBSITE = Environment.EnvironmentKey.string("order_website")
        private val REGISTER_WEBSITE = Environment.EnvironmentKey.string("register_website")

        @JvmStatic
        fun fromEnvironment(environment: Environment): VirologyWebsiteConfig {
            return VirologyWebsiteConfig(
                environment.access.required(ORDER_WEBSITE),
                environment.access.required(REGISTER_WEBSITE)
            )
        }
    }
}
