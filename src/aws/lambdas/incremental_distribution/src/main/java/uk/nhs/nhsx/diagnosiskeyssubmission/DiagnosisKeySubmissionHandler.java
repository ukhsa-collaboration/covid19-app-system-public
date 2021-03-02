package uk.nhs.nhsx.diagnosiskeyssubmission;

import uk.nhs.nhsx.core.Environment;
import uk.nhs.nhsx.core.EnvironmentKeys;
import uk.nhs.nhsx.core.HttpResponses;
import uk.nhs.nhsx.core.Jackson;
import uk.nhs.nhsx.core.StandardSigning;
import uk.nhs.nhsx.core.SystemClock;
import uk.nhs.nhsx.core.UniqueId;
import uk.nhs.nhsx.core.auth.ApiName;
import uk.nhs.nhsx.core.auth.Authenticator;
import uk.nhs.nhsx.core.auth.ResponseSigner;
import uk.nhs.nhsx.core.aws.dynamodb.AwsDynamoClient;
import uk.nhs.nhsx.core.aws.dynamodb.DynamoDBUtils;
import uk.nhs.nhsx.core.aws.s3.AwsS3Client;
import uk.nhs.nhsx.core.aws.s3.ObjectKeyNameProvider;
import uk.nhs.nhsx.core.aws.s3.S3Storage;
import uk.nhs.nhsx.core.aws.s3.UniqueObjectKeyNameProvider;
import uk.nhs.nhsx.core.events.DiagnosisKeySubmission;
import uk.nhs.nhsx.core.events.Events;
import uk.nhs.nhsx.core.events.PrintingJsonEvents;
import uk.nhs.nhsx.core.events.UnprocessableJson;
import uk.nhs.nhsx.core.routing.Routing;
import uk.nhs.nhsx.core.routing.RoutingHandler;
import uk.nhs.nhsx.diagnosiskeyssubmission.model.ClientTemporaryExposureKeysPayload;

import java.time.Instant;
import java.util.function.Supplier;

import static uk.nhs.nhsx.core.auth.StandardAuthentication.awsAuthentication;
import static uk.nhs.nhsx.core.routing.Routing.Method.POST;
import static uk.nhs.nhsx.core.routing.Routing.path;
import static uk.nhs.nhsx.core.routing.Routing.routes;
import static uk.nhs.nhsx.core.routing.StandardHandlers.authorisedBy;
import static uk.nhs.nhsx.core.routing.StandardHandlers.withSignedResponses;

public class DiagnosisKeySubmissionHandler extends RoutingHandler {

    private final Routing.Handler handler;

    @SuppressWarnings("unused")
    public DiagnosisKeySubmissionHandler() {
        this(Environment.fromSystem(), SystemClock.CLOCK, new PrintingJsonEvents(SystemClock.CLOCK));
    }

    public DiagnosisKeySubmissionHandler(Environment environment, Supplier<Instant> clock, Events events) {
        this(
            environment,
            awsAuthentication(ApiName.Mobile, events),
            awsAuthentication(ApiName.Health, events),
            StandardSigning.signResponseWithKeyGivenInSsm(environment, clock, events),
            new AwsS3Client(events),
            new DynamoDBUtils(),
            new UniqueObjectKeyNameProvider(clock, UniqueId.ID),
            clock,
            events
        );
    }

    DiagnosisKeySubmissionHandler(
        Environment environment,
        Authenticator mobileAuthenticator,
        Authenticator healthAuthenticator,
        ResponseSigner signer,
        S3Storage s3Storage,
        AwsDynamoClient awsDynamoClient,
        ObjectKeyNameProvider objectKeyNameProvider,
        Supplier<Instant> clock,
        Events events
    ) {
        DiagnosisKeysSubmissionService service =
            new DiagnosisKeysSubmissionService(
                s3Storage,
                awsDynamoClient,
                objectKeyNameProvider,
                environment.access.required(EnvironmentKeys.SUBMISSIONS_TOKENS_TABLE),
                environment.access.required(EnvironmentKeys.SUBMISSION_STORE),
                clock,
                events
            );

        handler = withSignedResponses(events, environment, signer,
            routes(
                authorisedBy(mobileAuthenticator,
                    path(POST, "/submission/diagnosis-keys", r -> {
                            events.emit(getClass(), new DiagnosisKeySubmission());
                            Jackson.readMaybe(r.getBody(), ClientTemporaryExposureKeysPayload.class,  e -> events.emit(getClass(), new UnprocessableJson(e)))
                                .ifPresent(service::acceptTemporaryExposureKeys);

                            return HttpResponses.ok();
                        }
                    )
                ),
                authorisedBy(healthAuthenticator,
                    path(POST, "/submission/diagnosis-keys/health", r ->
                        HttpResponses.ok()
                    )
                )
            )
        );
    }

    @Override
    public Routing.Handler handler() {
        return handler;
    }
}
