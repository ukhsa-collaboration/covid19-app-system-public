package uk.nhs.nhsx.core;

import uk.nhs.nhsx.activationsubmission.persist.Environment;
import uk.nhs.nhsx.core.auth.AwsResponseSigner;
import uk.nhs.nhsx.core.auth.ResponseSigner;
import uk.nhs.nhsx.core.aws.kms.KmsKeySigner;
import uk.nhs.nhsx.core.aws.ssm.AwsSsmFetcher;
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
        return datedSigner(clock, environment, "SSM_KEY_ID_PARAMETER_NAME");
    }

    public static RFC2616DatedSigner datedSigner(Supplier<Instant> clock, Environment environment, String ssmKeyIdParameterName) {
        String ssmKeyId = environment.access.required(ssmKeyIdParameterName);
        return datedSigner(clock, ssmKeyId);
    }

    public static RFC2616DatedSigner datedSigner(Supplier<Instant> clock, String ssmKeyId) {
        return new RFC2616DatedSigner(clock, signContentWithKeyGivenInSsm(ssmKeyId));
    }

    public static Signer signContentWithKeyGivenInSsm(String ssmParameterName) {
        return new AwsSigner(new AwsSsmFetcher(ssmParameterName), new KmsKeySigner());
    }
}
