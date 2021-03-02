package uk.nhs.nhsx.highriskpostcodesupload;

import uk.nhs.nhsx.core.Environment;
import uk.nhs.nhsx.core.EnvironmentKeys;
import uk.nhs.nhsx.core.HttpResponses;
import uk.nhs.nhsx.core.auth.ApiName;
import uk.nhs.nhsx.core.auth.Authenticator;
import uk.nhs.nhsx.core.aws.cloudfront.AwsCloudFront;
import uk.nhs.nhsx.core.aws.cloudfront.AwsCloudFrontClient;
import uk.nhs.nhsx.core.aws.s3.AwsS3;
import uk.nhs.nhsx.core.aws.s3.AwsS3Client;
import uk.nhs.nhsx.core.aws.s3.ObjectKey;
import uk.nhs.nhsx.core.events.Events;
import uk.nhs.nhsx.core.events.PrintingJsonEvents;
import uk.nhs.nhsx.core.events.RiskyPostDistrictUpload;
import uk.nhs.nhsx.core.routing.Routing;
import uk.nhs.nhsx.core.routing.RoutingHandler;
import uk.nhs.nhsx.core.signature.DatedSigner;

import static uk.nhs.nhsx.core.StandardSigning.datedSigner;
import static uk.nhs.nhsx.core.SystemClock.CLOCK;
import static uk.nhs.nhsx.core.auth.StandardAuthentication.awsAuthentication;
import static uk.nhs.nhsx.core.routing.Routing.Method.POST;
import static uk.nhs.nhsx.core.routing.Routing.path;
import static uk.nhs.nhsx.core.routing.Routing.routes;
import static uk.nhs.nhsx.core.routing.StandardHandlers.authorisedBy;
import static uk.nhs.nhsx.core.routing.StandardHandlers.withoutSignedResponses;

public class HighRiskPostcodesUploadHandler extends RoutingHandler {

    private static final String DISTRIBUTION_OBJ_KEY_NAME = "distribution/risky-post-districts";
    private static final String DISTRIBUTION_OBJ_V2_KEY_NAME = "distribution/risky-post-districts-v2";
    private static final String BACKUP_JSON_KEY_NAME = "backup/api-payload";
    private static final String RAW_CSV_KEY_NAME = "raw/risky-post-districts";
    private static final String METADATA_OBJ_KEY_NAME = "tier-metadata";

    private final Routing.Handler handler;

    @SuppressWarnings("unused")
    public HighRiskPostcodesUploadHandler() {
        this(
            Environment.fromSystem(),
            awsAuthentication(ApiName.HighRiskPostCodeUpload, new PrintingJsonEvents(CLOCK)),
            datedSigner(Environment.fromSystem(), CLOCK),
            new AwsS3Client(new PrintingJsonEvents(CLOCK)),
            new AwsCloudFrontClient(new PrintingJsonEvents(CLOCK)),
            new PrintingJsonEvents(CLOCK),
            awsAuthentication(ApiName.Health, new PrintingJsonEvents(CLOCK))
        );
    }

    public HighRiskPostcodesUploadHandler(Environment environment,
                                          Authenticator authenticator,
                                          DatedSigner signer,
                                          AwsS3 s3Storage,
                                          AwsCloudFront awsCloudFront,
                                          Events events,
                                          Authenticator healthAuthenticator) {
        var persistence = new RiskyPostCodesPersistence(
            environment.access.required(EnvironmentKeys.BUCKET_NAME),
            ObjectKey.of(DISTRIBUTION_OBJ_KEY_NAME),
            ObjectKey.of(DISTRIBUTION_OBJ_V2_KEY_NAME),
            ObjectKey.of(BACKUP_JSON_KEY_NAME),
            ObjectKey.of(RAW_CSV_KEY_NAME),
            ObjectKey.of(METADATA_OBJ_KEY_NAME),
            signer,
            s3Storage
        );

        var service = new RiskyPostCodesUploadService(
            persistence,
            awsCloudFront,
            environment.access.required(EnvironmentKeys.DISTRIBUTION_ID),
            environment.access.required(EnvironmentKeys.DISTRIBUTION_INVALIDATION_PATTERN),
            events
        );

        handler = withoutSignedResponses(
            events,
            environment,
            routes(
                authorisedBy(authenticator,
                    path(POST, "/upload/high-risk-postal-districts", r -> {
                        events.emit(getClass(), new RiskyPostDistrictUpload());
                        return service.upload(r.getBody());
                    })),
                authorisedBy(healthAuthenticator,
                    path(POST, "/upload/high-risk-postal-districts/health", r -> HttpResponses.ok()))
            )
        );
    }

    @Override
    public Routing.Handler handler() {
        return handler;
    }

}
