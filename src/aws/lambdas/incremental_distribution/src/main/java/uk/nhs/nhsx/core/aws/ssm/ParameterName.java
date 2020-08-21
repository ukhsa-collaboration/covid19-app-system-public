package uk.nhs.nhsx.core.aws.ssm;

import uk.nhs.nhsx.core.ValueType;

public class ParameterName extends ValueType<ParameterName> {

    protected ParameterName(String value) {
        super(value);
    }

    public static ParameterName of(String value) {
        return new ParameterName(value);
    }

    public ParameterName withPrefix(String prefix) {
        return ParameterName.of(prefix + "/" + value);
    }
}
