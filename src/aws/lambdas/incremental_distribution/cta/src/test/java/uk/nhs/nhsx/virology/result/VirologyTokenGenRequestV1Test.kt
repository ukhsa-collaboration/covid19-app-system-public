package uk.nhs.nhsx.virology.result

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import uk.nhs.nhsx.core.Json.readStrictOrNull
import uk.nhs.nhsx.domain.TestEndDate
import uk.nhs.nhsx.domain.TestResult

class VirologyTokenGenRequestV1Test {

    @Test
    fun `treats INDETERMINATE as VOID`() {
        val request = readStrictOrNull<VirologyTokenGenRequestV1>(
            """{
                    "testEndDate": "2020-09-29T00:00:00Z",
                    "testResult": "INDETERMINATE"
                }
            """.trimIndent()
        )

        expectThat(request).isEqualTo(
            VirologyTokenGenRequestV1(
                TestEndDate.of(2020, 9, 29),
                TestResult.Void
            )
        )
    }

    @Test
    fun `does not read JSON if testKit is present`() {
        expectThat(
            readStrictOrNull<VirologyTokenGenRequestV1>(
                """{
                    "testEndDate": "2020-09-29T00:00:00Z",
                    "testResult": "INDETERMINATE",
                    "testKit":"LAB_RESULT"
                }
            """.trimIndent()
            )
        ).isNull()
    }

    @Test
    fun `does not read JSON if testEndDate is not at midnight`() {
        expectThat(
            readStrictOrNull<VirologyTokenGenRequestV1>(
                """{
                    "testEndDate": "2020-09-29T20:00:00Z",
                    "testResult": "VOID",
                }
                """.trimIndent()
            )).isNull()
    }
}
