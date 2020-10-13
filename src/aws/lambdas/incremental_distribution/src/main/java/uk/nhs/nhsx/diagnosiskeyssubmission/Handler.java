package uk.nhs.nhsx.diagnosiskeyssubmission;

import uk.nhs.nhsx.core.Environment;
import uk.nhs.nhsx.core.HttpResponses;
import uk.nhs.nhsx.core.StandardSigning;
import uk.nhs.nhsx.core.SystemClock;
import uk.nhs.nhsx.core.UniqueId;
import uk.nhs.nhsx.core.auth.ApiName;
import uk.nhs.nhsx.core.auth.Authenticator;
import uk.nhs.nhsx.core.auth.ResponseSigner;
import uk.nhs.nhsx.core.aws.dynamodb.AwsDynamoClient;
import uk.nhs.nhsx.core.aws.dynamodb.DynamoDBUtils;
import uk.nhs.nhsx.core.aws.s3.*;
import uk.nhs.nhsx.core.routing.Routing;
import uk.nhs.nhsx.core.routing.RoutingHandler;
import uk.nhs.nhsx.diagnosiskeyssubmission.model.ClientTemporaryExposureKeysPayload;

import java.time.Instant;
import java.util.function.Supplier;

import static uk.nhs.nhsx.core.Jackson.deserializeMaybe;
import static uk.nhs.nhsx.core.auth.StandardAuthentication.awsAuthentication;
import static uk.nhs.nhsx.core.routing.Routing.path;
import static uk.nhs.nhsx.core.routing.Routing.routes;
import static uk.nhs.nhsx.core.routing.StandardHandlers.withSignedResponses;

/**
 * Diagnosis keys submission lambda
 * <p>
 * doc/design/api-contracts/diagnosis-key-submission.md
 */
public class Handler extends RoutingHandler {

    private final Routing.Handler handler;

    public Handler() {
        this(Environment.fromSystem(), SystemClock.CLOCK);
    }

    public Handler(Environment environment, Supplier<Instant> clock) {
        this(
            environment,
            awsAuthentication(ApiName.Mobile),
            StandardSigning.signResponseWithKeyGivenInSsm(clock, environment),
            new AwsS3Client(),
            new DynamoDBUtils(),
            new UniqueObjectKeyNameProvider(clock, UniqueId.ID)
        );
    }

    Handler(
            Environment environment,
            Authenticator authenticator,
            ResponseSigner signer,
            S3Storage s3Storage,
            AwsDynamoClient awsDynamoClient,
            ObjectKeyNameProvider objectKeyNameProvider
    ) {

        DiagnosisKeysSubmissionService service =
            new DiagnosisKeysSubmissionService(
                s3Storage,
                awsDynamoClient,
                objectKeyNameProvider,
                environment.access.required("submission_tokens_table"),
                BucketName.of(environment.access.required("SUBMISSION_STORE"))
            );

        this.handler = withSignedResponses(environment, authenticator, signer,
            routes(
                path(Routing.Method.POST, "/submission/diagnosis-keys",
                    (r) -> {
                        deserializeMaybe(r.getBody(), ClientTemporaryExposureKeysPayload.class)
                            .ifPresent(service::acceptTemporaryExposureKeys);

                        return HttpResponses.ok();
                    }
                ),
                path(Routing.Method.POST, "/submission/diagnosis-keys/health", (r) ->
                    HttpResponses.ok()
                )
            )
        );
    }

    @Override
    public Routing.Handler handler() {
        return handler;
    }
}