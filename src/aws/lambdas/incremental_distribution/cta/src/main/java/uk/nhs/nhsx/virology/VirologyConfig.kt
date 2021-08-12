package uk.nhs.nhsx.virology

import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.Environment.EnvironmentKey
import uk.nhs.nhsx.core.EnvironmentKeys.SUBMISSIONS_TOKENS_TABLE
import uk.nhs.nhsx.core.aws.dynamodb.IndexName
import uk.nhs.nhsx.core.aws.dynamodb.TableName

data class VirologyConfig(
    val testOrdersTable: TableName,
    val testResultsTable: TableName,
    val submissionTokensTable: TableName,
    val testOrdersIndex: IndexName
) {
    val maxTokenPersistenceRetryCount = 3

    companion object {
        private val TEST_ORDERS_TABLE = EnvironmentKey.value("test_orders_table", TableName)
        private val TEST_RESULTS_TABLE = EnvironmentKey.value("test_results_table", TableName)
        private val TEST_ORDERS_INDEX = EnvironmentKey.value("test_orders_index", IndexName)

        fun fromEnvironment(environment: Environment) = VirologyConfig(
            environment.access.required(TEST_ORDERS_TABLE),
            environment.access.required(TEST_RESULTS_TABLE),
            environment.access.required(SUBMISSIONS_TOKENS_TABLE),
            environment.access.required(TEST_ORDERS_INDEX)
        )
    }
}
