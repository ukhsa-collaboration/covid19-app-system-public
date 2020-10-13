package uk.nhs.nhsx.analyticssubmission;

import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose;
import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehoseClientBuilder;
import uk.nhs.nhsx.core.Environment;
import uk.nhs.nhsx.analyticssubmission.model.ClientAnalyticsSubmissionPayload;
import uk.nhs.nhsx.core.HttpResponses;
import uk.nhs.nhsx.core.SystemClock;
import uk.nhs.nhsx.core.UniqueId;
import uk.nhs.nhsx.core.auth.ApiName;
import uk.nhs.nhsx.core.auth.Authenticator;
import uk.nhs.nhsx.core.aws.s3.AwsS3Client;
import uk.nhs.nhsx.core.aws.s3.ObjectKeyNameProvider;
import uk.nhs.nhsx.core.aws.s3.S3Storage;
import uk.nhs.nhsx.core.aws.s3.UniqueObjectKeyNameProvider;
import uk.nhs.nhsx.core.routing.Routing;
import uk.nhs.nhsx.core.routing.RoutingHandler;

import java.time.Instant;
import java.util.function.Supplier;

import static uk.nhs.nhsx.core.Jackson.deserializeMaybe;
import static uk.nhs.nhsx.core.auth.StandardAuthentication.awsAuthentication;
import static uk.nhs.nhsx.core.routing.Routing.*;
import static uk.nhs.nhsx.core.routing.StandardHandlers.withoutSignedResponses;

/**
 * Daily mobile-analytics submission API
 * <p>
 * doc/design/api-contracts/analytics-submission.md
 */
public class Handler extends RoutingHandler {

    private static final AmazonKinesisFirehose kinesisFirehose = AmazonKinesisFirehoseClientBuilder.defaultClient();
    
    private final Routing.Handler handler;

    public Handler() {
        this(Environment.fromSystem(), SystemClock.CLOCK);
    }

    public Handler(Environment environment, Supplier<Instant> clock) {
        this(
            environment, awsAuthentication(ApiName.Mobile),
            new AwsS3Client(), 
            new UniqueObjectKeyNameProvider(clock, UniqueId.ID),
            analyticsConfig(environment)
        );
    }

    private static AnalyticsConfig analyticsConfig(Environment environment) {
        return new AnalyticsConfig(
            environment.access.required("firehose_stream_name"),
            !"false".equals(environment.access.required("s3_ingest_enabled")),
            "true".equals(environment.access.required("firehose_ingest_enabled")),
            environment.access.required("SUBMISSION_STORE")
        );
    }

    public Handler(Environment environment,
                   Authenticator authenticator,
                   S3Storage s3Storage,
                   ObjectKeyNameProvider objectKeyNameProvider,
                   AnalyticsConfig analyticsConfig) {
        
        AnalyticsSubmissionService service = new AnalyticsSubmissionService(
            analyticsConfig, s3Storage, objectKeyNameProvider, kinesisFirehose
        );
        this.handler = withoutSignedResponses(
            environment, authenticator,
            routes(
                path(Method.POST, "/submission/mobile-analytics", (r) ->
                    deserializeMaybe(r.getBody(), ClientAnalyticsSubmissionPayload.class)
                        .map(it -> {
                            service.accept(it);
                            return HttpResponses.ok();
                        })
                        .orElse(HttpResponses.badRequest())),
                path(Routing.Method.POST, "/submission/mobile-analytics/health", (r) ->
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
