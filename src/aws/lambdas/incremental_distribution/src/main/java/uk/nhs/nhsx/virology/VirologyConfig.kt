package uk.nhs.nhsx.virology

import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.EnvironmentKeys

data class VirologyConfig(@JvmField val testOrdersTable: String,
                          @JvmField val testResultsTable: String,
                          @JvmField val submissionTokensTable: String,
                          @JvmField val testOrdersIndex: String) {
    @JvmField
    val maxTokenPersistenceRetryCount = 3

    companion object {

        private val TEST_ORDERS_TABLE = Environment.EnvironmentKey.string("test_orders_table")
        private val TEST_RESULTS_TABLE = Environment.EnvironmentKey.string("test_results_table")
        private val TEST_ORDERS_INDEX = Environment.EnvironmentKey.string("test_orders_index")

        @JvmStatic
        fun fromEnvironment(environment: Environment): VirologyConfig {
            return VirologyConfig(
                environment.access.required(TEST_ORDERS_TABLE),
                environment.access.required(TEST_RESULTS_TABLE),
                environment.access.required(EnvironmentKeys.SUBMISSIONS_TOKENS_TABLE),
                environment.access.required(TEST_ORDERS_INDEX)
            )
        }
    }

}
