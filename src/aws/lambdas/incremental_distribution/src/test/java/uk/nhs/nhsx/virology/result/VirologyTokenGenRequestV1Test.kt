package uk.nhs.nhsx.virology.result

import com.fasterxml.jackson.core.JsonProcessingException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.Jackson.readStrict
import uk.nhs.nhsx.core.Jackson.readStrictOrNull

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

        assertThat(request).isEqualTo(
            VirologyTokenGenRequestV1(
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
                    "testEndDate": "2020-09-29T00:00:00Z",
                    "testResult": "INDETERMINATE",
                    "testKit":"LAB_RESULT"
                }
            """.trimIndent(),
                VirologyTokenGenRequestV1::class.java
            )
        }.isInstanceOf(JsonProcessingException::class.java)
    }

    @Test
    fun `throws exception if testEndDate is not at midnight`() {
        assertThatThrownBy {
            readStrict(
                """{
                    "testEndDate": "2020-09-29T20:00:00Z",
                    "testResult": "VOID",
                }
                """.trimIndent(),
                VirologyTokenGenRequestV1::class.java
            )
        }.isInstanceOf(JsonProcessingException::class.java)
    }
}
