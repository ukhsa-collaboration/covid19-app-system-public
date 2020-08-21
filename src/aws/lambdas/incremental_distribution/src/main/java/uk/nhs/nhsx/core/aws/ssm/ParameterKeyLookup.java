package uk.nhs.nhsx.core.aws.ssm;

import uk.nhs.nhsx.core.signature.KeyId;

public class ParameterKeyLookup implements KeyLookup {

    private final Parameter<KeyId> parameter;

    public ParameterKeyLookup(Parameters parameters, ParameterName parameterName) {
        this.parameter = parameters.parameter(parameterName, KeyId::of);
    }

    public KeyId getKmsKeyId() {
        return parameter.value();
    }
}
