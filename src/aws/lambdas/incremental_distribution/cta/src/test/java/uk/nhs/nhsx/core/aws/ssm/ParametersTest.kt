package uk.nhs.nhsx.core.aws.ssm

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import uk.nhs.nhsx.circuitbreakers.ApprovalStatus
import uk.nhs.nhsx.circuitbreakers.ApprovalStatus.NO
import java.util.function.Function

class ParametersTest {
    private val whatever = ParameterName.of("whatever")

    @ParameterizedTest
    @CsvSource(
        value = [
            "xx,false",
            "yes,true",
            "no,false",
            "YES,true",
            "NO,false",
            "YeS,true",
            "No,false",
            "True,true",
            "No,false",
            "true,true",
            "enabled,true",
            "disabled,false",
        ]
    )
    fun `parses yes yes true true`(value: String, expected: Boolean) {
        expectThat(value)
            .get { fixedValue(this) }
            .get { ofBoolean(whatever) }
            .get { value() }
            .isEqualTo(expected)
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "xx,NO",
            "YES,YES",
            "NO,NO",
            "pending,PENDING",
        ]
    )
    fun `parses enum`(value: String, expected: ApprovalStatus) {
        expectThat(value)
            .get { fixedValue(value) }
            .get { ofEnum(whatever, ApprovalStatus::class.java, NO) }
            .get { value() }
            .isEqualTo(expected)
    }

    private companion object {
        private fun fixedValue(value: String): Parameters =
            object : Parameters {
                override fun <T> parameter(
                    name: ParameterName,
                    convert: Function<String, T>
                ) = Parameter { convert.apply(value) }
            }
    }
}
