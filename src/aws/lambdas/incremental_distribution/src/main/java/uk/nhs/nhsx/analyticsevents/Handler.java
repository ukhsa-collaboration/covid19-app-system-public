package uk.nhs.nhsx.analyticsevents;

import uk.nhs.nhsx.core.*;
import uk.nhs.nhsx.core.auth.ApiName;
import uk.nhs.nhsx.core.auth.Authenticator;
import uk.nhs.nhsx.core.auth.ResponseSigner;
import uk.nhs.nhsx.core.aws.s3.*;
import uk.nhs.nhsx.core.routing.Routing;
import uk.nhs.nhsx.core.routing.RoutingHandler;

import java.time.Instant;
import java.util.function.Supplier;

import static uk.nhs.nhsx.core.StandardSigning.signResponseWithKeyGivenInSsm;
import static uk.nhs.nhsx.core.auth.StandardAuthentication.awsAuthentication;
import static uk.nhs.nhsx.core.routing.Routing.Method.POST;
import static uk.nhs.nhsx.core.routing.Routing.path;
import static uk.nhs.nhsx.core.routing.StandardHandlers.withSignedResponses;

public class Handler extends RoutingHandler {

    private final Routing.Handler handler;

    public Handler() {
        this(Environment.fromSystem(), SystemClock.CLOCK);
    }

    public Handler(Environment environment, Supplier<Instant> clock) {
        this(environment, awsAuthentication(ApiName.Mobile), signResponseWithKeyGivenInSsm(clock, environment), new AwsS3Client(), new PartitionedObjectKeyNameProvider(clock, UniqueId.ID));
    }

    public Handler(Environment environment, Authenticator authenticator, ResponseSigner signer, S3Storage s3Storage, ObjectKeyNameProvider objectKeyNameProvider) {

        BucketName bucketName = environment.access.required(EnvironmentKeys.SUBMISSION_STORE);
        boolean acceptRequestsEnabled = environment.access.required(Environment.EnvironmentKey.bool("ACCEPT_REQUESTS_ENABLED"));

        this.handler = withSignedResponses(
            environment,
            authenticator,
            signer,
            Routing.routes(
                path(POST, "/submission/mobile-analytics-events", request -> {
                    if (!acceptRequestsEnabled) {
                        return HttpResponses.serviceUnavailable();
                    }

                    var payload = new PayloadValidator().maybeValidPayload(request.getBody());
                    if (payload.isEmpty()) {
                        return HttpResponses.badRequest();
                    }
                    new AnalyticsEventsSubmissionService(s3Storage, objectKeyNameProvider, bucketName).accept(payload.get());
                    return HttpResponses.ok();
                }),
                path(Routing.Method.POST, "/submission/mobile-analytics-events/health", (r) ->
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
