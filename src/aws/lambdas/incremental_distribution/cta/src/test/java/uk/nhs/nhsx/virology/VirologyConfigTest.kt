package uk.nhs.nhsx.virology

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import uk.nhs.nhsx.core.TestEnvironments.TEST
import uk.nhs.nhsx.core.aws.dynamodb.IndexName
import uk.nhs.nhsx.core.aws.dynamodb.TableName

class VirologyConfigTest {

    @Test
    fun `creates config from valid parameters`() {
        val virologyConfig = VirologyConfig(
            TableName.of("testOrdersTableName"),
            TableName.of("testResultsTableName"),
            TableName.of("submissionTokensTableName"),
            IndexName.of("testOrdersIndex")
        )

        expectThat(virologyConfig).isEqualTo(
            VirologyConfig.fromEnvironment(TEST.apply(mapOf(
                "test_orders_table" to "testOrdersTableName",
                "test_results_table" to "testResultsTableName",
                "test_orders_index" to "testOrdersIndex",
                "submission_tokens_table" to "submissionTokensTableName"
            )))
        )
    }
}
