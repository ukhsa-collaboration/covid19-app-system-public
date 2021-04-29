package uk.nhs.nhsx.virology.result

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
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

        assertThat(request).isEqualTo(
            VirologyTokenGenRequestV2(
                TestEndDate.of(2020, 9, 29),
                TestResult.Void,
                LAB_RESULT
            )
        )
    }

    @Test
    fun `does not read json if testEndDate is not at midnight`() {
        assertNull(
            readStrictOrNull<VirologyTokenGenRequestV2>(
                """{
                    "testEndDate": "2020-09-29T20:00:00Z",
                    "testResult": "VOID",
                    "testKit": "LAB_RESULT"
                }
                """.trimIndent()
            )
        )
    }

    @Test
    fun `does not read json for LFD negative`() {
        assertNull(
            readStrictOrNull<VirologyTokenGenRequestV2>(
                """{
                    "testEndDate": "2020-09-29T20:00:00Z",
                    "testResult": "NEGATIVE",
                    "testKit": "RAPID_RESULT"
                }
                """.trimIndent(),
            )
        )
    }

    @Test
    fun `does not read json for LFD void`() {
        assertNull(
            readStrictOrNull<VirologyTokenGenRequestV2>(
                """{
                    "testEndDate": "2020-09-29T20:00:00Z",
                    "testResult": "VOID",
                    "testKit": "RAPID_RESULT"
                }
                """.trimIndent()
            )
        )
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

        assertThat(version1.convertToV2()).isEqualTo(version2)
    }
}
