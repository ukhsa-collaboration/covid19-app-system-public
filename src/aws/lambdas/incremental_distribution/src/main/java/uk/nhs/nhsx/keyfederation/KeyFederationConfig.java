package uk.nhs.nhsx.keyfederation;

import uk.nhs.nhsx.core.Environment;
import uk.nhs.nhsx.core.aws.s3.BucketName;
import uk.nhs.nhsx.core.aws.secretsmanager.SecretName;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class KeyFederationConfig {

    public final boolean downloadEnabled;
    public final boolean uploadEnabled;
    public final BucketName submissionBucketName;
    public final String interopBaseUrl;
    public final SecretName interopAuthTokenSecretName;
    public final SecretName interopPrivateKeySecretName;
    public final String federatedKeyPrefix;
    public final String stateTableName;
    public final List<String> validRegions;
    public final String region;

    public KeyFederationConfig(
        boolean downloadEnabled,
        boolean uploadEnabled,
        BucketName submissionBucketName,
        String interopBaseUrl,
        SecretName interopAuthTokenSecretName,
        SecretName interopPrivateKeySecretName,
        String federatedKeyPrefix,
        String stateTableName,
        String validRegions,
        String region) {
        this.downloadEnabled = downloadEnabled;
        this.uploadEnabled = uploadEnabled;
        this.submissionBucketName = submissionBucketName;
        this.interopBaseUrl = interopBaseUrl;
        this.interopAuthTokenSecretName = interopAuthTokenSecretName;
        this.interopPrivateKeySecretName = interopPrivateKeySecretName;
        this.federatedKeyPrefix = federatedKeyPrefix;
        this.stateTableName = stateTableName;
        this.validRegions = toList(validRegions);
        this.region = region;
    }

    public static KeyFederationConfig fromEnvironment(Environment e) {
        return new KeyFederationConfig(
            Boolean.parseBoolean(e.access.required("DOWNLOAD_ENABLED")),
            Boolean.parseBoolean(e.access.required("UPLOAD_ENABLED")),
            BucketName.of(e.access.required("SUBMISSION_BUCKET_NAME")),
            e.access.required("INTEROP_BASE_URL"),
            SecretName.of(e.access.required("INTEROP_AUTH_TOKEN_SECRET_NAME")),
            SecretName.of(e.access.required("INTEROP_PRIVATE_KEY_SECRET_NAME")),
            e.access.required("FEDERATED_KEY_PREFIX"),
            e.access.required("PROCESSOR_STATE_TABLE"),
            e.access.required("VALID_REGIONS"),
            e.access.required("REGION"));
    }

    private static List<String> toList(String validRegionsCsv) {
        return Optional.ofNullable(validRegionsCsv).map(csv ->
            Arrays.stream(csv.split(","))
                .filter(it -> !it.isBlank())
                .collect(Collectors.toList())
        ).orElse(Collections.emptyList());
    }

}
