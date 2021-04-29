package uk.nhs.nhsx.virology

import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.Environment.EnvironmentKey
import uk.nhs.nhsx.core.EnvironmentKeys

data class VirologyConfig(val testOrdersTable: String,
                          val testResultsTable: String,
                          val submissionTokensTable: String,
                          val testOrdersIndex: String) {

    val maxTokenPersistenceRetryCount = 3

    companion object {

        private val TEST_ORDERS_TABLE = EnvironmentKey.string("test_orders_table")
        private val TEST_RESULTS_TABLE = EnvironmentKey.string("test_results_table")
        private val TEST_ORDERS_INDEX = EnvironmentKey.string("test_orders_index")

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
