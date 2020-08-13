package uk.nhs.nhsx.analyticssubmission;

import uk.nhs.nhsx.activationsubmission.persist.Environment;
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

    private final static String bucketName = System.getenv("SUBMISSION_STORE");
    private final Routing.Handler handler;

    public Handler() {
        this(Environment.fromSystem(), SystemClock.CLOCK);
    }

    public Handler(Environment environment, Supplier<Instant> clock) {
        this(
            awsAuthentication(ApiName.Mobile),
            new AwsS3Client(), 
            new UniqueObjectKeyNameProvider(clock, UniqueId.ID),
            bucketName
        );
    }

    public Handler(Authenticator authenticator, 
                   S3Storage s3Storage,
                   ObjectKeyNameProvider objectKeyNameProvider,
                   String bucketName) {
        
        AnalyticsSubmissionService service = new AnalyticsSubmissionService(
            bucketName, s3Storage, objectKeyNameProvider
        );
        this.handler = withoutSignedResponses(
            authenticator,
            routes(path(Method.POST, "/submission/mobile-analytics", (r) ->
                deserializeMaybe(r.getBody(), ClientAnalyticsSubmissionPayload.class)
                    .map(it -> {
                        service.accept(it);
                        return HttpResponses.ok();
                    })
                    .orElse(HttpResponses.badRequest()))
            )
        );
    }

    @Override
    public Routing.Handler handler() {
        return handler;
    }
}
