package uk.nhs.nhsx.virology

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test

class VirologyConfigTest {

    @Test
    fun `creates config from valid parameters`() {
        val virologyConfig = VirologyConfig(
            "testOrdersTableName",
            "testResultsTableName",
            "submissionTokensTableName",
            "testOrdersIndex",
            1
        )

        assertThat(virologyConfig).isEqualTo(
            VirologyConfig(
                "testOrdersTableName",
                "testResultsTableName",
                "submissionTokensTableName",
                "testOrdersIndex",
                1
            )
        )
    }

    @Test
    fun `throws when max retry count set to zero`() {
        assertThatThrownBy {
            VirologyConfig(
                "testOrdersTableName",
                "testResultsTableName",
                "submissionTokensTableName",
                "testOrdersIndex",
                0
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `throws when max retry count is negative`() {
        assertThatThrownBy {
            VirologyConfig(
                "testOrdersTableName",
                "testResultsTableName",
                "submissionTokensTableName",
                "testOrdersIndex",
                    -1
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
    }
}