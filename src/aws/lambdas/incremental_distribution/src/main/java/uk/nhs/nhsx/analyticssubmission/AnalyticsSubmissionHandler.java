package uk.nhs.nhsx.analyticssubmission;

import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose;
import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehoseClientBuilder;
import uk.nhs.nhsx.analyticssubmission.model.ClientAnalyticsSubmissionPayload;
import uk.nhs.nhsx.core.Environment;
import uk.nhs.nhsx.core.Environment.EnvironmentKey;
import uk.nhs.nhsx.core.EnvironmentKeys;
import uk.nhs.nhsx.core.HttpResponses;
import uk.nhs.nhsx.core.Jackson;
import uk.nhs.nhsx.core.SystemClock;
import uk.nhs.nhsx.core.UniqueId;
import uk.nhs.nhsx.core.auth.ApiName;
import uk.nhs.nhsx.core.auth.Authenticator;
import uk.nhs.nhsx.core.aws.s3.AwsS3Client;
import uk.nhs.nhsx.core.aws.s3.ObjectKeyNameProvider;
import uk.nhs.nhsx.core.aws.s3.S3Storage;
import uk.nhs.nhsx.core.aws.s3.UniqueObjectKeyNameProvider;
import uk.nhs.nhsx.core.events.Events;
import uk.nhs.nhsx.core.events.MobileAnalyticsSubmission;
import uk.nhs.nhsx.core.events.PrintingJsonEvents;
import uk.nhs.nhsx.core.events.UnprocessableJson;
import uk.nhs.nhsx.core.routing.Routing;
import uk.nhs.nhsx.core.routing.RoutingHandler;

import java.time.Instant;
import java.util.function.Supplier;

import static uk.nhs.nhsx.core.auth.StandardAuthentication.awsAuthentication;
import static uk.nhs.nhsx.core.routing.Routing.Method.POST;
import static uk.nhs.nhsx.core.routing.Routing.path;
import static uk.nhs.nhsx.core.routing.Routing.routes;
import static uk.nhs.nhsx.core.routing.StandardHandlers.authorisedBy;
import static uk.nhs.nhsx.core.routing.StandardHandlers.withoutSignedResponses;

public class AnalyticsSubmissionHandler extends RoutingHandler {

    private static final EnvironmentKey<Boolean> S3_INGEST_ENABLED = EnvironmentKey.bool("s3_ingest_enabled");
    private static final EnvironmentKey<Boolean> FIREHOSE_INGEST_ENABLED = EnvironmentKey.bool("firehose_ingest_enabled");
    private static final EnvironmentKey<String> FIREHOSE_STREAM = EnvironmentKey.string("firehose_stream_name");

    private final Routing.Handler handler;

    @SuppressWarnings("unused")
    public AnalyticsSubmissionHandler() {
        this(Environment.fromSystem(), SystemClock.CLOCK, new PrintingJsonEvents(SystemClock.CLOCK));
    }

    public AnalyticsSubmissionHandler(Environment environment, Supplier<Instant> clock, Events events) {
        this(
            environment,
            awsAuthentication(ApiName.Health, events),
            awsAuthentication(ApiName.Mobile, events),
            new AwsS3Client(events),
            AmazonKinesisFirehoseClientBuilder.defaultClient(),
            new UniqueObjectKeyNameProvider(clock, UniqueId.ID),
            analyticsConfig(environment),
            events
        );
    }

    public AnalyticsSubmissionHandler(Environment environment,
                                      Authenticator healthAuthenticator,
                                      Authenticator mobileAuthenticator,
                                      S3Storage s3Storage,
                                      AmazonKinesisFirehose kinesisFirehose,
                                      ObjectKeyNameProvider objectKeyNameProvider,
                                      AnalyticsConfig analyticsConfig,
                                      Events events) {

        var service = new AnalyticsSubmissionService(
            analyticsConfig, s3Storage, objectKeyNameProvider, kinesisFirehose,
                events);

        this.handler = withoutSignedResponses(
            events,
            environment,
            routes(
                authorisedBy(mobileAuthenticator,
                    path(POST, "/submission/mobile-analytics", r -> {
                        events.emit(getClass(), new MobileAnalyticsSubmission());
                        return Jackson.readMaybe(r.getBody(), ClientAnalyticsSubmissionPayload.class, e -> events.emit(getClass(), new UnprocessableJson(e)))
                            .map(it -> {
                                service.accept(it);
                                return HttpResponses.ok();
                            })
                            .orElse(HttpResponses.badRequest());
                    })
                ),
                authorisedBy(healthAuthenticator,
                    path(POST, "/submission/mobile-analytics/health", r ->
                        HttpResponses.ok()
                    )
                )
            )
        );
    }

    private static AnalyticsConfig analyticsConfig(Environment environment) {
        return new AnalyticsConfig(
            environment.access.required(FIREHOSE_STREAM),
            environment.access.required(S3_INGEST_ENABLED),
            environment.access.required(FIREHOSE_INGEST_ENABLED),
            environment.access.required(EnvironmentKeys.SUBMISSION_STORE)
        );
    }

    @Override
    public Routing.Handler handler() {
        return handler;
    }
}
