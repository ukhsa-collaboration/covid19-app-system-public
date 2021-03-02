package uk.nhs.nhsx.virology

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class VirologyConfigTest {

    @Test
    fun `creates config from valid parameters`() {
        val virologyConfig = VirologyConfig(
            "testOrdersTableName",
            "testResultsTableName",
            "submissionTokensTableName",
            "testOrdersIndex"
        )

        assertThat(virologyConfig).isEqualTo(
            VirologyConfig(
                "testOrdersTableName",
                "testResultsTableName",
                "submissionTokensTableName",
                "testOrdersIndex"
            )
        )
    }
}
