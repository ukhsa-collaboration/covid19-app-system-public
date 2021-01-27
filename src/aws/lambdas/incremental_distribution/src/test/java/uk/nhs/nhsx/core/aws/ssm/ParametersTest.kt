package uk.nhs.nhsx.core.aws.ssm

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.circuitbreakers.ApprovalStatus
import java.util.function.Function

class ParametersTest {
    private val whatever = ParameterName.of("whatever")

    @Test
    fun parsesYesYesTrueTrue() {
        assertFixedValueParses("xx", false)
        assertFixedValueParses("yes", true)
        assertFixedValueParses("no", false)
        assertFixedValueParses("YES", true)
        assertFixedValueParses("NO", false)
        assertFixedValueParses("YeS", true)
        assertFixedValueParses("No", false)
        assertFixedValueParses("True", true)
        assertFixedValueParses("No", false)
        assertFixedValueParses("true", true)
        assertFixedValueParses("enabled", true)
        assertFixedValueParses("disabled", false)
    }


    @Test
    fun parsesEnum() {
        assertThat(
            enumParam("xx", ApprovalStatus::class.java, ApprovalStatus.NO).value(),
            equalTo(ApprovalStatus.NO)
        )
        assertThat(
            enumParam("YES", ApprovalStatus::class.java, ApprovalStatus.NO).value(),
            equalTo(ApprovalStatus.YES)
        )
        assertThat(
            enumParam("NO", ApprovalStatus::class.java, ApprovalStatus.NO).value(),
            equalTo(ApprovalStatus.NO)
        )
        assertThat(
            enumParam("pending", ApprovalStatus::class.java, ApprovalStatus.NO).value(),
            equalTo(ApprovalStatus.PENDING)
        )
    }

    private fun enumParam(value: String, type: Class<ApprovalStatus>, whenError: ApprovalStatus) =
        fixedValue(value).ofEnum(whatever, type, whenError)

    private fun assertFixedValueParses(value: String, expected: Boolean) {
        assertThat(value, fixedValue(value).ofBoolean(whatever).value(), equalTo(expected))
    }

    private companion object {
        private fun fixedValue(value: String): Parameters =
            object : Parameters {
                override fun <T> parameter(name: ParameterName, convert: Function<String, T>): Parameter<T> =
                    Parameter { convert.apply(value) }
            }
    }
}