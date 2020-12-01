package uk.nhs.nhsx.aae;

import uk.nhs.nhsx.core.Environment;
import uk.nhs.nhsx.core.Environment.EnvironmentKey;

import static uk.nhs.nhsx.core.Environment.EnvironmentKey.*;

public class AAEUploadConfig {
    public final String aaeUrlPrefix;
    public final String aaeUrlSuffix;
    public final String p12CertificateSecretName;
    public final String p12CertificatePasswordSecretName;
    public final String subscriptionKeySecretName;

    public AAEUploadConfig(
            String aaeUrlPrefix,
            String aaeUrlSuffix,
            String p12CertificateSecretName,
            String p12CertificatePasswordSecretName,
            String subscriptionKeySecretName)
    {
        this.aaeUrlPrefix = aaeUrlPrefix;
        this.aaeUrlSuffix = aaeUrlSuffix;
        this.p12CertificateSecretName = p12CertificateSecretName;
        this.p12CertificatePasswordSecretName = p12CertificatePasswordSecretName;
        this.subscriptionKeySecretName = subscriptionKeySecretName;
    }

    private static final EnvironmentKey<String> AAE_URL_PREFIX = string("AAE_URL_PREFIX");
    private static final EnvironmentKey<String> AAE_URL_SUFFIX = string("AAE_URL_SUFFIX");
    private static final EnvironmentKey<String> P12_CERT_SECRET_NAME = string("P12_CERT_SECRET_NAME");
    private static final EnvironmentKey<String> P12_CERT_PASSWORD_SECRET_NAME = string("P12_CERT_PASSWORD_SECRET_NAME");
    private static final EnvironmentKey<String> AAE_SUBSCRIPTION_SECRET_NAME = string("AAE_SUBSCRIPTION_SECRET_NAME");

    public static AAEUploadConfig fromEnvironment(Environment e) {
        return new AAEUploadConfig(
            e.access.required(AAE_URL_PREFIX),
            e.access.required(AAE_URL_SUFFIX),
            e.access.required(P12_CERT_SECRET_NAME),
            e.access.required(P12_CERT_PASSWORD_SECRET_NAME),
            e.access.required(AAE_SUBSCRIPTION_SECRET_NAME)
        );
    }
}
