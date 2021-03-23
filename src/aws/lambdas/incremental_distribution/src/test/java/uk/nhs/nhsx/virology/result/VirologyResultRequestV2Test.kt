package uk.nhs.nhsx.virology.result

import com.fasterxml.jackson.core.JsonProcessingException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.Jackson.readStrictOrNull
import uk.nhs.nhsx.virology.CtaToken
import uk.nhs.nhsx.virology.TestKit.LAB_RESULT

class VirologyResultRequestV2Test {

    @Test
    fun `treats INDETERMINATE as VOID`() {
        val request = readStrictOrNull<VirologyResultRequestV2>(
            """{
                    "ctaToken": "cc8f0b6z",
                    "testEndDate": "2020-09-29T00:00:00Z",
                    "testResult": "INDETERMINATE",
                    "testKit": "LAB_RESULT"
                }
            """.trimIndent()
        )

        assertThat(request).isEqualTo(
            VirologyResultRequestV2(
                CtaToken.of("cc8f0b6z"),
                TestEndDate.of(2020, 9, 29),
                TestResult.Void,
                LAB_RESULT
            )
        )
    }

    @Test
    fun `does not read JSON if testEndDate is not at midnight`() {
        assertNull(
            readStrictOrNull<VirologyResultRequestV2>(
                """{
                    "ctaToken": "cc8f0b6z",
                    "testEndDate": "2020-09-29T20:00:00Z",
                    "testResult": "VOID",
                    "testKit": "LAB_RESULT"
                }
                """.trimIndent()
            )
        )
    }

    @Test
    fun `does not read JSON for LFD negative`() {
        assertNull(
            readStrictOrNull<VirologyResultRequestV2>(
                """{
                    "ctaToken": "cc8f0b6z",
                    "testEndDate": "2020-09-29T20:00:00Z",
                    "testResult": "NEGATIVE",
                    "testKit": "RAPID_RESULT"
                }
                """.trimIndent()
            )
        )
    }

    @Test
    fun `does not read JSON for LFD void`() {
        assertNull(
            readStrictOrNull<VirologyResultRequestV2>("""{
                    "ctaToken": "cc8f0b6z",
                    "testEndDate": "2020-09-29T20:00:00Z",
                    "testResult": "VOID",
                    "testKit": "RAPID_RESULT"
                }
                """.trimIndent())
        )
    }

    @Test
    fun `can convert V1 to V2`() {
        val version1 = VirologyResultRequestV1(
            CtaToken.of("cc8f0b6z"),
            TestEndDate.of(2020, 9, 29),
            TestResult.Void
        )

        val version2 = VirologyResultRequestV2(
            CtaToken.of("cc8f0b6z"),
            TestEndDate.of(2020, 9, 29),
            TestResult.Void,
            LAB_RESULT
        )

        assertThat(version1.convertToV2()).isEqualTo(version2)
    }
}
