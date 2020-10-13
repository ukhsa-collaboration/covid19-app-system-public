package uk.nhs.nhsx.highriskvenuesupload;

import uk.nhs.nhsx.core.Environment;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import uk.nhs.nhsx.core.ContentTypes;
import uk.nhs.nhsx.core.HttpResponses;
import uk.nhs.nhsx.core.SystemClock;
import uk.nhs.nhsx.core.auth.ApiName;
import uk.nhs.nhsx.core.auth.Authenticator;
import uk.nhs.nhsx.core.aws.cloudfront.AwsCloudFrontClient;
import uk.nhs.nhsx.core.aws.s3.*;
import uk.nhs.nhsx.core.aws.s3.AwsS3Client;
import uk.nhs.nhsx.core.routing.Routing;
import uk.nhs.nhsx.core.routing.RoutingHandler;

import java.time.Instant;
import java.util.function.Supplier;

import static uk.nhs.nhsx.core.StandardSigning.datedSigner;
import static uk.nhs.nhsx.core.auth.StandardAuthentication.awsAuthentication;
import static uk.nhs.nhsx.core.routing.Routing.path;
import static uk.nhs.nhsx.core.routing.Routing.routes;
import static uk.nhs.nhsx.core.routing.StandardHandlers.withoutSignedResponses;

/**
 * High risk venue upload lambda
 * <p>
 * doc/design/api-contracts/risky-venue-upload.md
 */
public class Handler extends RoutingHandler {

    private final Routing.Handler handler;

    public Handler() {
        this(Environment.fromSystem(), SystemClock.CLOCK);
    }

    public Handler(Environment environment, Supplier<Instant> clock) {
        this(
            environment, 
            awsAuthentication(ApiName.HighRiskVenuesUpload),
            createUploadService(clock, environment)
        );
    }

    Handler(Environment environment, Authenticator authenticator, HighRiskVenuesUploadService service) {
        this.handler = withoutSignedResponses(
            environment, 
            authenticator,
            routes(
                path(Routing.Method.POST, "/upload/identified-risk-venues",
                    (r) -> { 
                        if (!ContentTypes.isTextCsv(r))
                            return HttpResponses.unprocessableEntity("validation error: Content type is not text/csv");

                        VenuesUploadResult result = service.upload(r.getBody());

                        return mapResultToResponse(result);
                    }
                ),
                path(Routing.Method.POST, "/upload/identified-risk-venues/health", (r) ->
                    HttpResponses.ok()
                )
            )
        );
    }

    private APIGatewayProxyResponseEvent mapResultToResponse(VenuesUploadResult result) {
        if (result.type == VenuesUploadResult.ResultType.ValidationError)
            return HttpResponses.unprocessableEntity(result.message);
        
        return HttpResponses.accepted(result.message);
    }

    @Override
    public Routing.Handler handler() {
        return handler;
    }

    private static HighRiskVenuesUploadService createUploadService(Supplier<Instant> clock, Environment environment) {
        boolean shouldParseAdditionalFields = Boolean.parseBoolean(environment.access.required("should_parse_additional_fields"));
        return new HighRiskVenuesUploadService(
            new HighRiskVenuesUploadConfig(
                BucketName.of(environment.access.required("BUCKET_NAME")),
                ObjectKey.of("distribution/risky-venues"),
                environment.access.required("DISTRIBUTION_ID"),
                environment.access.required("DISTRIBUTION_INVALIDATION_PATTERN")
            ),
            datedSigner(clock, environment),
            new AwsS3Client(),
            new AwsCloudFrontClient(),
            new HighRiskVenueCsvParser(shouldParseAdditionalFields)
        );
    }

}
