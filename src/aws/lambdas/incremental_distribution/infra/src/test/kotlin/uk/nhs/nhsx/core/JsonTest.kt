package uk.nhs.nhsx.core

import com.fasterxml.jackson.core.JsonProcessingException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import uk.nhs.nhsx.core.Json.toJson
import uk.nhs.nhsx.domain.ReportType
import uk.nhs.nhsx.domain.TestType
import uk.nhs.nhsx.domain.DiagnosisKeySubmissionToken
import uk.nhs.nhsx.domain.TestResult
import uk.nhs.nhsx.testhelper.assertions.isEqualToJson
import uk.nhs.nhsx.testhelper.assertions.readJsonOrThrow
import java.time.Instant
import java.time.Instant.EPOCH

class JsonTest {

    @Test
    fun `round-trips value types from JSON`() {
        val input = MyValueWrapper(DiagnosisKeySubmissionToken.of("hello"))
        val json = toJson(input)

        expectThat(json).isEqualToJson("""{ "value": "hello" }""")
        expectThat(json).readJsonOrThrow<MyValueWrapper>().isEqualTo(input)
    }

    @Test
    fun `validates value type as null if invalid`() {
        expectThat(Json.readJsonOrNull<MyValueWrapper>("""{ "value": "" }""")).isNull()
    }

    @Test
    fun `instant round-trips`() {
        val wrapper = MyInstantWrapper(EPOCH)

        val asString = """{"value":"1970-01-01T00:00:00Z"}"""

        expectThat(toJson(wrapper)).isEqualToJson(asString)
        expectThat(asString).readJsonOrThrow<MyInstantWrapper>().isEqualTo(wrapper)
    }

    @Test
    fun `reads json strictly`() {
        val input = """{"value":"1970-01-01T00:00:00Z", "anotherField": "foobar" }"""

        expectThat(Json.readStrictOrNull<MyInstantWrapper>(input)).isNull()
    }

    @ParameterizedTest
    @ValueSource(strings = ["POSITIVE", "NEGATIVE", "VOID"])
    fun `round-trips TestResult`(value: String) {
        val input = """{ "value":"$value" }"""
        val wrapper = Json.readJsonOrThrow<MyTestResultWrapper>(input)

        expectThat(wrapper.value).isEqualTo(TestResult.from(value))
        expectThat(toJson(wrapper)).isEqualToJson(input)
    }

    @Test
    fun `converts indeterminate test result to VOID`() {
        expectThat(toJson(MyTestResultWrapper(TestResult.from("INDETERMINATE")))).isEqualToJson("""{ "value":"VOID" }""")
    }

    @ParameterizedTest
    @ValueSource(ints = [1, 2, 3])
    fun `round-trips test type`(value: Int) {
        val input = """{ "value":$value }"""
        val wrapper = Json.readJsonOrThrow<MyTestTypeWrapper>(input)
        expectThat(wrapper.value).isEqualTo(TestType.from(value))
        expectThat(toJson(wrapper)).isEqualToJson(input)
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 1, 2, 3, 4, 5])
    fun `round-trips report type`(value: Int) {
        val input = """{ "value":$value }"""
        val wrapper = Json.readJsonOrThrow<MyReportTypeWrapper>(input)
        expectThat(wrapper.value).isEqualTo(ReportType.from(value))
        expectThat(toJson(wrapper)).isEqualToJson(input)
    }

    @Test
    fun `throws exception if list contains null`() {
        expectThrows<JsonProcessingException> {
            Json.readJsonOrThrow<MyListWrapper>("""{ "value": [null, {"value": "1970-01-01T00:00:00Z"}] }""")
        }
    }
}

data class MyValueWrapper(val value: DiagnosisKeySubmissionToken)
data class MyInstantWrapper(val value: Instant)
data class MyTestResultWrapper(val value: TestResult)
data class MyTestTypeWrapper(val value: TestType)
data class MyReportTypeWrapper(val value: ReportType)
data class MyListWrapper(val value: List<MyInstantWrapper>)
