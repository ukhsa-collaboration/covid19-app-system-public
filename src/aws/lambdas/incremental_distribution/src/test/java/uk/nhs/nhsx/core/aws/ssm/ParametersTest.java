package uk.nhs.nhsx.core.aws.ssm;

import org.junit.Test;
import uk.nhs.nhsx.circuitbreakers.ApprovalStatus;

import java.util.function.Function;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class ParametersTest {

    private final ParameterName whatever = ParameterName.of("whatever");

    @Test
    public void parsesYesyesTruetrue() throws Exception {
        assertFixedValueParses("xx", false);
        assertFixedValueParses("yes", true);
        assertFixedValueParses("no", false);
        assertFixedValueParses("YES", true);
        assertFixedValueParses("NO", false);
        assertFixedValueParses("YeS", true);
        assertFixedValueParses("No", false);
        assertFixedValueParses("True", true);
        assertFixedValueParses("No", false);
        assertFixedValueParses("true", true);
        assertFixedValueParses("enabled", true);
        assertFixedValueParses("disabled", false);
    }

    @Test
    public void parsesEnum() throws Exception {
        assertThat(enumParam("xx", ApprovalStatus.class, ApprovalStatus.NO).value(), equalTo(ApprovalStatus.NO));
        assertThat(enumParam("YES", ApprovalStatus.class, ApprovalStatus.NO).value(), equalTo(ApprovalStatus.YES));
        assertThat(enumParam("NO", ApprovalStatus.class, ApprovalStatus.NO).value(), equalTo(ApprovalStatus.NO));
        assertThat(enumParam("pending", ApprovalStatus.class, ApprovalStatus.NO).value(), equalTo(ApprovalStatus.PENDING));
    }

    private Parameter<ApprovalStatus> enumParam(String value, Class<ApprovalStatus> type, ApprovalStatus whenError) {
        return fixedValue(value).ofEnum(whatever, type, whenError);
    }

    private void assertFixedValueParses(String value, boolean expected) {
        assertThat(value, fixedValue(value).ofBoolean(whatever).value(), equalTo(expected));
    }

    private static Parameters fixedValue(String value) {
        return new Parameters() {
            @Override
            public <T> Parameter<T> parameter(ParameterName name, Function<String, T> convert) {
                return () -> convert.apply(value);
            }
        };
    }
}