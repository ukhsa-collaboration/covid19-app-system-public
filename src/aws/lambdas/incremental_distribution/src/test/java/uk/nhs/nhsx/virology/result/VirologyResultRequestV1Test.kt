package uk.nhs.nhsx.virology.result

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.Json.readStrictOrNull
import uk.nhs.nhsx.domain.CtaToken
import uk.nhs.nhsx.domain.TestEndDate
import uk.nhs.nhsx.domain.TestResult

class VirologyResultRequestV1Test {

    @Test
    fun `treats INDETERMINATE as VOID`() {
        val request = readStrictOrNull<VirologyResultRequestV1>(
            """{
                    "ctaToken": "cc8f0b6z",
                    "testEndDate": "2020-09-29T00:00:00Z",
                    "testResult": "INDETERMINATE"
                }
            """.trimIndent()
        )

        assertThat(request).isEqualTo(
            VirologyResultRequestV1(
                CtaToken.of("cc8f0b6z"),
                TestEndDate.of(2020, 9, 29),
                TestResult.Void
            )
        )
    }

    @Test
    fun `does not read json if testKit is present`() {
        assertNull(
            readStrictOrNull<VirologyResultRequestV1>("""{
                    "ctaToken": "cc8f0b6z",
                    "testEndDate": "2020-09-29T00:00:00Z",
                    "testResult": "INDETERMINATE",
                    "testKit":"LAB_RESULT"
                }
                """.trimIndent()))
    }

    @Test
    fun `does not read json if testEndDate is not at midnight`() {
        assertNull(
            readStrictOrNull<VirologyResultRequestV1>("""{
                    "ctaToken": "cc8f0b6z",
                    "testEndDate": "2020-09-29T20:00:00Z",
                    "testResult": "VOID"
                }
                """.trimIndent()))
    }
}
