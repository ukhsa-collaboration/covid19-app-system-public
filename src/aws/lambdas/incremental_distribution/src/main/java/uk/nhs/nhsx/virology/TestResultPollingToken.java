package uk.nhs.nhsx.virology;

import uk.nhs.nhsx.core.ValueType;

import static uk.nhs.nhsx.core.Preconditions.checkArgument;

public class TestResultPollingToken extends ValueType<TestResultPollingToken> {

    private TestResultPollingToken(String value) {
        super(value);
        checkArgument(!value.isEmpty(), "Test result polling token not present");
    }

    public static TestResultPollingToken of(String value) {
        return new TestResultPollingToken(value);
    }
}
