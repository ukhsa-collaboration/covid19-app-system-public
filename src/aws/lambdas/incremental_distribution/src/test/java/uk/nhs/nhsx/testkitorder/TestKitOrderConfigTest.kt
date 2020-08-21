package uk.nhs.nhsx.testkitorder

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test

class TestKitOrderConfigTest {

    @Test
    fun `creates config from valid parameters`() {
        val config = TestKitOrderConfig(
            "testOrdersTableName",
            "testResultsTableName",
            "submissionTokensTableName",
            "orderWebsite",
            "registerWebsite",
            1
        )
        assertThat(config).isEqualTo(
            TestKitOrderConfig(
                "testOrdersTableName",
                "testResultsTableName",
                "submissionTokensTableName",
                "orderWebsite",
                "registerWebsite",
                1
            )
        )
    }

    @Test
    fun `throws when max retry count set to zero`() {
        assertThatThrownBy {
            TestKitOrderConfig(
                "testOrdersTableName",
                "testResultsTableName",
                "submissionTokensTableName",
                "orderWebsite",
                "registerWebsite",
                0
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `throws when max retry count is negative`() {
        assertThatThrownBy {
            TestKitOrderConfig(
                "testOrdersTableName",
                "testResultsTableName",
                "submissionTokensTableName",
                "orderWebsite",
                "registerWebsite",
                -1
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
    }
}