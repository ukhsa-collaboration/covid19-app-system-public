package uk.nhs.nhsx.testresultsupload;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import uk.nhs.nhsx.core.HttpResponses;
import uk.nhs.nhsx.core.SystemClock;
import uk.nhs.nhsx.core.auth.ApiName;
import uk.nhs.nhsx.core.auth.Authenticator;
import uk.nhs.nhsx.core.routing.Routing;
import uk.nhs.nhsx.core.routing.RoutingHandler;

import static uk.nhs.nhsx.core.Jackson.deserializeMaybe;
import static uk.nhs.nhsx.core.auth.StandardAuthentication.awsAuthentication;
import static uk.nhs.nhsx.core.routing.Routing.*;
import static uk.nhs.nhsx.core.routing.StandardHandlers.withoutSignedResponses;

public class Handler extends RoutingHandler {

    private final Routing.Handler handler;
    private final NpexTestResultUploadService uploadService;

    public Handler() {
        this(
            awsAuthentication(ApiName.TestResultUpload),
            npexService()
        );
    }

    public Handler(Authenticator authenticator, NpexTestResultUploadService uploadService) {
        this.uploadService = uploadService;
        this.handler = withoutSignedResponses(
            authenticator,
            routes(
                path(Method.POST, "/upload/virology-test/npex-result",
                    (request) ->
                        deserializeMaybe(request.getBody(), NpexTestResult.class)
                            .map(testResult -> {
                                this.uploadService.accept(testResult);
                                return HttpResponses.accepted("successfully processed");
                            })
                            .orElse(HttpResponses.unprocessableEntity())
                )
            )
        );

    }

    @Override
    public Routing.Handler handler() {
        return handler;
    }

    private static NpexTestResultUploadService npexService() {
        return new NpexTestResultUploadService(
            new NpexTestResultPersistenceService(
                AmazonDynamoDBClientBuilder.defaultClient(),
                npexTestResultConfig()
            ),
            SystemClock.CLOCK
        );
    }

    private static NpexTestResultConfig npexTestResultConfig() {
        return new NpexTestResultConfig(
            System.getenv("submission_tokens_table"),
            System.getenv("test_results_table"),
            System.getenv("test_orders_table")
        );
    }
}
