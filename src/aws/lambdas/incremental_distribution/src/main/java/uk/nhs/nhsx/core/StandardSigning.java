package uk.nhs.nhsx.core;

import uk.nhs.nhsx.core.auth.AwsResponseSigner;
import uk.nhs.nhsx.core.auth.ResponseSigner;
import uk.nhs.nhsx.core.aws.kms.KmsSigner;
import uk.nhs.nhsx.core.aws.ssm.AwsSsmParameters;
import uk.nhs.nhsx.core.aws.ssm.ParameterKeyLookup;
import uk.nhs.nhsx.core.aws.ssm.ParameterName;
import uk.nhs.nhsx.core.aws.ssm.Parameters;
import uk.nhs.nhsx.core.signature.RFC2616DatedSigner;
import uk.nhs.nhsx.core.signature.Signer;

import java.time.Instant;
import java.util.function.Supplier;

public class StandardSigning {

    public static final Environment.EnvironmentKey<ParameterName> SSM_KEY_ID_PARAMETER_NAME = Environment.EnvironmentKey.value("SSM_KEY_ID_PARAMETER_NAME", ParameterName::of);

    public static ResponseSigner signResponseWithKeyGivenInSsm(Environment environment, Supplier<Instant> clock) {
        return new AwsResponseSigner(datedSigner(environment, clock));
    }

    public static RFC2616DatedSigner datedSigner(Environment environment, Supplier<Instant> clock) {
        return datedSigner(clock, new AwsSsmParameters(), environment.access.required(SSM_KEY_ID_PARAMETER_NAME));
    }

    public static RFC2616DatedSigner datedSigner(Supplier<Instant> clock, Parameters parameters, ParameterName parameterName) {
        return new RFC2616DatedSigner(clock, signContentWithKeyFromParameter(parameters, parameterName));
    }

    public static Signer signContentWithKeyFromParameter(Parameters parameters, ParameterName name) {
        return signContentWithKeyId(new ParameterKeyLookup(parameters, name));
    }

    private static Signer signContentWithKeyId(ParameterKeyLookup keyLookup) {
        return new KmsSigner(keyLookup::getKmsKeyId);
    }
}
