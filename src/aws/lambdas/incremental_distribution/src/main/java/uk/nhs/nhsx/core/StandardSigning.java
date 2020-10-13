package uk.nhs.nhsx.core;

import uk.nhs.nhsx.core.auth.AwsResponseSigner;
import uk.nhs.nhsx.core.auth.ResponseSigner;
import uk.nhs.nhsx.core.aws.kms.KmsKeySigner;
import uk.nhs.nhsx.core.aws.ssm.AwsSsmParameters;
import uk.nhs.nhsx.core.aws.ssm.ParameterKeyLookup;
import uk.nhs.nhsx.core.aws.ssm.ParameterName;
import uk.nhs.nhsx.core.aws.ssm.Parameters;
import uk.nhs.nhsx.core.signature.AwsSigner;
import uk.nhs.nhsx.core.signature.RFC2616DatedSigner;
import uk.nhs.nhsx.core.signature.Signer;

import java.time.Instant;
import java.util.function.Supplier;

public class StandardSigning {

    public static ResponseSigner signResponseWithKeyGivenInSsm(Supplier<Instant> clock, Environment environment) {
        return new AwsResponseSigner(
            datedSigner(clock, environment)
        );
    }

    public static RFC2616DatedSigner datedSigner(Supplier<Instant> clock, Environment environment) {
        return datedSigner(clock, new AwsSsmParameters(), ParameterName.of(environment.access.required("SSM_KEY_ID_PARAMETER_NAME")));
    }
    
    public static RFC2616DatedSigner datedSigner(Supplier<Instant> clock, Parameters parameters, ParameterName parameterName) {
        return new RFC2616DatedSigner(clock, signContentWithKeyFromParameter(parameters, parameterName));
    }

    public static Signer signContentWithKeyFromParameter(Parameters parameters, ParameterName name) {
        return signContentWithKeyId(new ParameterKeyLookup(parameters, name));
    }

    private static AwsSigner signContentWithKeyId(ParameterKeyLookup keyLookup) {
        return new AwsSigner(keyLookup, new KmsKeySigner());
    }
}
