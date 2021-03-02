package uk.nhs.nhsx.analyticsevents;

import uk.nhs.nhsx.core.Environment;
import uk.nhs.nhsx.core.EnvironmentKeys;
import uk.nhs.nhsx.core.HttpResponses;
import uk.nhs.nhsx.core.SystemClock;
import uk.nhs.nhsx.core.UniqueId;
import uk.nhs.nhsx.core.auth.ApiName;
import uk.nhs.nhsx.core.auth.Authenticator;
import uk.nhs.nhsx.core.auth.ResponseSigner;
import uk.nhs.nhsx.core.aws.s3.AwsS3Client;
import uk.nhs.nhsx.core.aws.s3.BucketName;
import uk.nhs.nhsx.core.aws.s3.ObjectKeyNameProvider;
import uk.nhs.nhsx.core.aws.s3.PartitionedObjectKeyNameProvider;
import uk.nhs.nhsx.core.aws.s3.S3Storage;
import uk.nhs.nhsx.core.events.Events;
import uk.nhs.nhsx.core.events.PrintingJsonEvents;
import uk.nhs.nhsx.core.routing.Routing;
import uk.nhs.nhsx.core.routing.RoutingHandler;

import java.time.Instant;
import java.util.function.Supplier;

import static uk.nhs.nhsx.core.StandardSigning.signResponseWithKeyGivenInSsm;
import static uk.nhs.nhsx.core.auth.StandardAuthentication.awsAuthentication;
import static uk.nhs.nhsx.core.routing.Routing.Method.POST;
import static uk.nhs.nhsx.core.routing.Routing.path;
import static uk.nhs.nhsx.core.routing.StandardHandlers.authorisedBy;
import static uk.nhs.nhsx.core.routing.StandardHandlers.withSignedResponses;

public class AnalyticsEventsHandler extends RoutingHandler {

    private final Routing.Handler handler;

    @SuppressWarnings("unused")
    public AnalyticsEventsHandler() {
        this(Environment.fromSystem(), SystemClock.CLOCK, new PrintingJsonEvents(SystemClock.CLOCK));
    }

    public AnalyticsEventsHandler(Environment environment, Supplier<Instant> clock, Events events) {
        this(environment,
            awsAuthentication(ApiName.Mobile, events),
            signResponseWithKeyGivenInSsm(environment, clock, events),
            new AwsS3Client(events),
            new PartitionedObjectKeyNameProvider(clock, UniqueId.ID),
            events,
            awsAuthentication(ApiName.Health, events));
    }

    public AnalyticsEventsHandler(Environment environment,
                                  Authenticator authenticator,
                                  ResponseSigner signer,
                                  S3Storage s3Storage,
                                  ObjectKeyNameProvider objectKeyNameProvider,
                                  Events events,
                                  Authenticator healthAuthenticator) {
        BucketName bucketName = environment.access.required(EnvironmentKeys.SUBMISSION_STORE);
        boolean acceptRequestsEnabled = environment.access.required(Environment.EnvironmentKey.bool("ACCEPT_REQUESTS_ENABLED"));

        this.handler = withSignedResponses(
            events,
            environment,
            signer,
            Routing.routes(
                authorisedBy(authenticator,
                    path(POST, "/submission/mobile-analytics-events", request -> {
                        if (!acceptRequestsEnabled) {
                            return HttpResponses.serviceUnavailable();
                        }

                        var payload = new PayloadValidator().maybeValidPayload(request.getBody());
                        if (payload.isEmpty()) {
                            return HttpResponses.badRequest();
                        }
                        new AnalyticsEventsSubmissionService(s3Storage, objectKeyNameProvider, bucketName, events).accept(payload.get());
                        return HttpResponses.ok();
                    })
                ),
                authorisedBy(healthAuthenticator,
                    path(POST, "/submission/mobile-analytics-events/health", (r) ->
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
