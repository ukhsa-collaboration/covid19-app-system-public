package uk.nhs.nhsx.highriskvenuesupload;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.nhs.nhsx.core.*;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import uk.nhs.nhsx.core.auth.ApiName;
import uk.nhs.nhsx.core.auth.Authenticator;
import uk.nhs.nhsx.core.aws.cloudfront.AwsCloudFrontClient;
import uk.nhs.nhsx.core.aws.s3.*;
import uk.nhs.nhsx.core.routing.Routing;
import uk.nhs.nhsx.core.routing.RoutingHandler;

import java.time.Instant;
import java.util.function.Supplier;

import static uk.nhs.nhsx.core.StandardSigning.datedSigner;
import static uk.nhs.nhsx.core.auth.StandardAuthentication.awsAuthentication;
import static uk.nhs.nhsx.core.routing.Routing.Method.POST;
import static uk.nhs.nhsx.core.routing.Routing.path;
import static uk.nhs.nhsx.core.routing.Routing.routes;
import static uk.nhs.nhsx.core.routing.StandardHandlers.withoutSignedResponses;

public class HighRiskVenuesUploadHandler extends RoutingHandler {

    private static final Logger logger = LogManager.getLogger(HighRiskVenuesUploadHandler.class);

    private final Routing.Handler handler;

    @SuppressWarnings("unused")
    public HighRiskVenuesUploadHandler() {
        this(Environment.fromSystem(), SystemClock.CLOCK);
    }

    public HighRiskVenuesUploadHandler(Environment environment, Supplier<Instant> clock) {
        this(
            environment,
            awsAuthentication(ApiName.HighRiskVenuesUpload),
            createUploadService(clock, environment)
        );
    }

    HighRiskVenuesUploadHandler(Environment environment, Authenticator authenticator, HighRiskVenuesUploadService service) {
        this.handler = withoutSignedResponses(
            environment,
            authenticator,
            routes(
                path(POST, "/upload/identified-risk-venues",
                    (r) -> {
                        if (!ContentTypes.isTextCsv(r)) {
                            return HttpResponses.unprocessableEntity("validation error: Content type is not text/csv");
                        }

                        VenuesUploadResult result = service.upload(r.getBody());

                        return mapResultToResponse(result);
                    }
                ),
                path(POST, "/upload/identified-risk-venues/health", (r) ->
                    HttpResponses.ok()
                )
            )
        );
    }

    private APIGatewayProxyResponseEvent mapResultToResponse(VenuesUploadResult result) {
        if (result.type == VenuesUploadResult.ResultType.ValidationError) {
            logger.error("Upload of high risk venue file failed validation: " + result.message);
            return HttpResponses.unprocessableEntity(result.message);
        }
        return HttpResponses.accepted(result.message);
    }

    @Override
    public Routing.Handler handler() {
        return handler;
    }

    private static final Environment.EnvironmentKey<Boolean> SHOULD_PARSE_ADDITIONAL_FIELDS = Environment.EnvironmentKey.bool("should_parse_additional_fields");

    private static HighRiskVenuesUploadService createUploadService(Supplier<Instant> clock, Environment environment) {
        boolean shouldParseAdditionalFields = environment.access.required(SHOULD_PARSE_ADDITIONAL_FIELDS);
        return new HighRiskVenuesUploadService(
            new HighRiskVenuesUploadConfig(
                environment.access.required(EnvironmentKeys.BUCKET_NAME),
                ObjectKey.of("distribution/risky-venues"),
                environment.access.required(EnvironmentKeys.DISTRIBUTION_ID),
                environment.access.required(EnvironmentKeys.DISTRIBUTION_INVALIDATION_PATTERN)
            ),
            datedSigner(environment, clock),
            new AwsS3Client(),
            new AwsCloudFrontClient(),
            new HighRiskVenueCsvParser(shouldParseAdditionalFields)
        );
    }
}
