package uk.nhs.nhsx.virology.result

import com.fasterxml.jackson.core.JsonProcessingException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.Jackson.readStrict
import uk.nhs.nhsx.core.Jackson.readStrictOrNull
import uk.nhs.nhsx.virology.CtaToken

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
    fun `throws exception if testKit is present`() {
        assertThatThrownBy {
            readStrict(
                """{
                    "ctaToken": "cc8f0b6z",
                    "testEndDate": "2020-09-29T00:00:00Z",
                    "testResult": "INDETERMINATE",
                    "testKit":"LAB_RESULT"
                }
                """.trimIndent(),
                VirologyResultRequestV1::class.java
            )
        }.isInstanceOf(JsonProcessingException::class.java)
    }

    @Test
    fun `throws exception if testEndDate is not at midnight`() {
        assertThatThrownBy {
            readStrict(
                """{
                    "ctaToken": "cc8f0b6z",
                    "testEndDate": "2020-09-29T20:00:00Z",
                    "testResult": "VOID"
                }
                """.trimIndent(),
                VirologyResultRequestV1::class.java
            )
        }.isInstanceOf(JsonProcessingException::class.java)
    }
}
