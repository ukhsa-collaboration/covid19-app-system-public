package uk.nhs.nhsx.virology.result

import com.fasterxml.jackson.core.JsonProcessingException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.Jackson.readStrict
import uk.nhs.nhsx.core.Jackson.readStrictOrNull
import uk.nhs.nhsx.virology.TestKit.LAB_RESULT

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
    fun `throws exception if testEndDate is not at midnight`() {
        assertThatThrownBy {
            readStrict(
                """{
                    "testEndDate": "2020-09-29T20:00:00Z",
                    "testResult": "VOID",
                    "testKit": "LAB_RESULT"
                }
                """.trimIndent(),
                VirologyTokenGenRequestV2::class.java
            )
        }.isInstanceOf(JsonProcessingException::class.java)
    }

    @Test
    fun `throws exception for LFD negative`() {
        assertThatThrownBy {
            readStrict(
                """{
                    "testEndDate": "2020-09-29T20:00:00Z",
                    "testResult": "NEGATIVE",
                    "testKit": "RAPID_RESULT"
                }
                """.trimIndent(),
                VirologyTokenGenRequestV2::class.java
            )
        }.isInstanceOf(JsonProcessingException::class.java)
    }

    @Test
    fun `throws exception for LFD void`() {
        assertThatThrownBy {
            readStrict(
                """{
                    "testEndDate": "2020-09-29T20:00:00Z",
                    "testResult": "VOID",
                    "testKit": "RAPID_RESULT"
                }
                """.trimIndent(),
                VirologyTokenGenRequestV2::class.java
            )
        }.isInstanceOf(JsonProcessingException::class.java)
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
