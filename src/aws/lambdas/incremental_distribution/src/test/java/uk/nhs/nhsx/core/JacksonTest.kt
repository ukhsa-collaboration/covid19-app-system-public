package uk.nhs.nhsx.core

import com.fasterxml.jackson.core.JsonProcessingException
import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.skyscreamer.jsonassert.JSONAssert.assertEquals
import org.skyscreamer.jsonassert.JSONCompareMode.STRICT
import uk.nhs.nhsx.core.Jackson.toJson
import uk.nhs.nhsx.keyfederation.download.ReportType
import uk.nhs.nhsx.keyfederation.download.TestType
import uk.nhs.nhsx.virology.DiagnosisKeySubmissionToken
import uk.nhs.nhsx.virology.result.TestResult
import java.time.Instant
import java.time.Instant.EPOCH

class JacksonTest {

    @Test
    fun `round-trips value types from JSON`() {
        val input = MyValueWrapper(DiagnosisKeySubmissionToken.of("hello"))
        val json = toJson(input)
        assertEquals("""{ "value": "hello" }""".trimIndent(), json, STRICT)
        assertThat(Jackson.readJson(json, MyValueWrapper::class.java), equalTo(input))
    }

    @Test
    fun `validates value type as null if invalid`() {
        assertThat(Jackson.readOrNull<MyValueWrapper>("""{ "value": "" }""") { }, absent())
    }

    @Test
    fun `instant round-trips`() {
        val wrapper = MyInstantWrapper(EPOCH)

        val asString = """{"value":"1970-01-01T00:00:00Z"}"""

        assertThat(toJson(wrapper), equalTo(asString))
        val actual = Jackson.readOrNull<MyInstantWrapper>(asString)
        assertThat(actual, equalTo(wrapper))
    }

    @Test
    fun `reads json strictly`() {
        val input = """{"value":"1970-01-01T00:00:00Z", "anotherField": "foobar" }"""

        assertThatThrownBy { Jackson.readStrict(input, MyInstantWrapper::class.java) }
            .isInstanceOf(JsonProcessingException::class.java)

        assertThat(Jackson.readStrictOrNull<MyInstantWrapper>(input), absent())
    }

    @ParameterizedTest
    @ValueSource(strings = ["POSITIVE", "NEGATIVE", "VOID"])
    fun `round-trips TestResult`(value: String) {
        val input = """{ "value":"$value" }"""
        val wrapper = Jackson.readJson(input, MyTestResultWrapper::class.java)
        assertThat(wrapper.value, equalTo(TestResult.from(value)))
        assertEquals(input, toJson(wrapper), STRICT)
    }

    @Test
    fun `converts indeterminate test result to VOID`() {
        assertEquals("""{ "value":"VOID" }""", toJson(MyTestResultWrapper(TestResult.from("INDETERMINATE"))), STRICT)
    }

    @ParameterizedTest
    @ValueSource(ints = [1, 2, 3])
    internal fun `round-trips test type`(value: Int) {
        val input = """{ "value":$value }"""
        val wrapper = Jackson.readJson(input, MyTestTypeWrapper::class.java)
        assertThat(wrapper.value, equalTo(TestType.from(value)))
        assertEquals(input, toJson(wrapper), STRICT)
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 1, 2, 3, 4, 5])
    internal fun `round-trips report type`(value: Int) {
        val input = """{ "value":$value }"""
        val wrapper = Jackson.readJson(input, MyReportTypeWrapper::class.java)
        assertThat(wrapper.value, equalTo(ReportType.from(value)))
        assertEquals(input, toJson(wrapper), STRICT)
    }
}

data class MyValueWrapper(val value: DiagnosisKeySubmissionToken)
data class MyInstantWrapper(val value: Instant)
data class MyTestResultWrapper(val value: TestResult)
data class MyTestTypeWrapper(val value: TestType)
data class MyReportTypeWrapper(val value: ReportType)
