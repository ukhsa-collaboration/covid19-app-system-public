package uk.nhs.nhsx.highriskvenuesupload;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import uk.nhs.nhsx.core.ContentTypes;
import uk.nhs.nhsx.core.Environment;
import uk.nhs.nhsx.core.EnvironmentKeys;
import uk.nhs.nhsx.core.HttpResponses;
import uk.nhs.nhsx.core.SystemClock;
import uk.nhs.nhsx.core.auth.ApiName;
import uk.nhs.nhsx.core.auth.Authenticator;
import uk.nhs.nhsx.core.aws.cloudfront.AwsCloudFrontClient;
import uk.nhs.nhsx.core.aws.s3.AwsS3Client;
import uk.nhs.nhsx.core.aws.s3.ObjectKey;
import uk.nhs.nhsx.core.events.Events;
import uk.nhs.nhsx.core.events.PrintingJsonEvents;
import uk.nhs.nhsx.core.events.RiskyVenuesUpload;
import uk.nhs.nhsx.core.routing.Routing;
import uk.nhs.nhsx.core.routing.RoutingHandler;

import java.time.Instant;
import java.util.function.Supplier;

import static uk.nhs.nhsx.core.StandardSigning.datedSigner;
import static uk.nhs.nhsx.core.auth.StandardAuthentication.awsAuthentication;
import static uk.nhs.nhsx.core.routing.Routing.Method.POST;
import static uk.nhs.nhsx.core.routing.Routing.path;
import static uk.nhs.nhsx.core.routing.Routing.routes;
import static uk.nhs.nhsx.core.routing.StandardHandlers.authorisedBy;
import static uk.nhs.nhsx.core.routing.StandardHandlers.withoutSignedResponses;

public class HighRiskVenuesUploadHandler extends RoutingHandler {

    private final Routing.Handler handler;
    private final Events events;

    @SuppressWarnings("unused")
    public HighRiskVenuesUploadHandler() {
        this(Environment.fromSystem(), SystemClock.CLOCK, new PrintingJsonEvents(SystemClock.CLOCK));
    }

    public HighRiskVenuesUploadHandler(Environment environment, Supplier<Instant> clock, Events events) {
        this(
            environment,
            awsAuthentication(ApiName.HighRiskVenuesUpload, events),
            createUploadService(clock, environment, events),
            awsAuthentication(ApiName.Health, events),
            events
        );
    }

    HighRiskVenuesUploadHandler(Environment environment,
                                Authenticator authenticator,
                                HighRiskVenuesUploadService service,
                                Authenticator healthAuthenticator,
                                Events events) {
        this.events = events;
        this.handler = withoutSignedResponses(
            events,
            environment,
            routes(
                authorisedBy(authenticator,
                    path(POST, "/upload/identified-risk-venues", r -> {
                            events.emit(getClass(), new RiskyVenuesUpload());
                            if (!ContentTypes.isTextCsv(r)) {
                                return HttpResponses.unprocessableEntity("validation error: Content type is not text/csv");
                            }

                            VenuesUploadResult result = service.upload(r.getBody());

                            return mapResultToResponse(result);
                        }
                    )
                ),
                authorisedBy(healthAuthenticator,
                    path(POST, "/upload/identified-risk-venues/health", r ->
                        HttpResponses.ok()
                    )
                )
            )
        );
    }

    private APIGatewayProxyResponseEvent mapResultToResponse(VenuesUploadResult result) {
        if (result.type == VenuesUploadResult.ResultType.ValidationError) {
            events.emit(getClass(), HighRiskVenueUploadFileInvalid.INSTANCE);
            return HttpResponses.unprocessableEntity(result.message);
        }
        return HttpResponses.accepted(result.message);
    }

    @Override
    public Routing.Handler handler() {
        return handler;
    }

    private static final Environment.EnvironmentKey<Boolean> SHOULD_PARSE_ADDITIONAL_FIELDS = Environment.EnvironmentKey.bool("should_parse_additional_fields");

    private static HighRiskVenuesUploadService createUploadService(Supplier<Instant> clock, Environment environment, Events events) {
        boolean shouldParseAdditionalFields = environment.access.required(SHOULD_PARSE_ADDITIONAL_FIELDS);
        return new HighRiskVenuesUploadService(
            new HighRiskVenuesUploadConfig(
                environment.access.required(EnvironmentKeys.BUCKET_NAME),
                ObjectKey.of("distribution/risky-venues"),
                environment.access.required(EnvironmentKeys.DISTRIBUTION_ID),
                environment.access.required(EnvironmentKeys.DISTRIBUTION_INVALIDATION_PATTERN)
            ),
            datedSigner(environment, clock),
            new AwsS3Client(events),
            new AwsCloudFrontClient(events),
            new HighRiskVenueCsvParser(shouldParseAdditionalFields)
        );
    }
}
