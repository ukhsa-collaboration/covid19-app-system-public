package uk.nhs.nhsx.testkitorder;

import uk.nhs.nhsx.core.ValueType;

import static com.google.common.base.Preconditions.checkArgument;

public class TestResultPollingToken extends ValueType<TestResultPollingToken> {

    private TestResultPollingToken(String value) {
        super(value);
        checkArgument(!value.isEmpty(), "Test result polling token not present");
    }

    public static TestResultPollingToken of(String value) {
        return new TestResultPollingToken(value);
    }
}
