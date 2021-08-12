package uk.nhs.nhsx.virology.result

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import uk.nhs.nhsx.core.Json.readStrictOrNull
import uk.nhs.nhsx.domain.TestEndDate
import uk.nhs.nhsx.domain.TestKit.LAB_RESULT
import uk.nhs.nhsx.domain.TestResult

class VirologyTokenGenRequestV2Test {

    @Test
    fun `treats INDETERMINATE as VOID`() {
        val request = readStrictOrNull<VirologyTokenGenRequestV2>(
            """{
                    "testEndDate": "2020-09-29T00:00:00Z",
                    "testResult": "INDETERMINATE",
                    "testKit": "LAB_RESULT"
                }
            """.trimIndent()
        )

        expectThat(request).isEqualTo(
            VirologyTokenGenRequestV2(
                TestEndDate.of(2020, 9, 29),
                TestResult.Void,
                LAB_RESULT
            )
        )
    }

    @Test
    fun `does not read json if testEndDate is not at midnight`() {
        expectThat(
            readStrictOrNull<VirologyTokenGenRequestV2>(
                """{
                    "testEndDate": "2020-09-29T20:00:00Z",
                    "testResult": "VOID",
                    "testKit": "LAB_RESULT"
                }
                """.trimIndent()
            )
        ).isNull()
    }

    @Test
    fun `does not read json for LFD negative`() {
        expectThat(
            readStrictOrNull<VirologyTokenGenRequestV2>(
                """{
                    "testEndDate": "2020-09-29T20:00:00Z",
                    "testResult": "NEGATIVE",
                    "testKit": "RAPID_RESULT"
                }
                """.trimIndent(),
            )
        ).isNull()
    }

    @Test
    fun `does not read json for LFD void`() {
        expectThat(
            readStrictOrNull<VirologyTokenGenRequestV2>(
                """{
                    "testEndDate": "2020-09-29T20:00:00Z",
                    "testResult": "VOID",
                    "testKit": "RAPID_RESULT"
                }
                """.trimIndent()
            )
        ).isNull()
    }

    @Test
    fun `can convert V1 to V2`() {
        val version1 = VirologyTokenGenRequestV1(
            TestEndDate.of(2020, 9, 29),
            TestResult.Void
        )

        val version2 = VirologyTokenGenRequestV2(
            TestEndDate.of(2020, 9, 29),
            TestResult.Void,
            LAB_RESULT
        )

        expectThat(version1.convertToV2()).isEqualTo(version2)
    }
}
