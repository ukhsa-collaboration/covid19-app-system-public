package uk.nhs.nhsx.virology;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.nhs.nhsx.core.Environment;
import uk.nhs.nhsx.core.HttpResponses;
import uk.nhs.nhsx.core.Jackson;
import uk.nhs.nhsx.core.SystemClock;
import uk.nhs.nhsx.core.auth.ApiName;
import uk.nhs.nhsx.core.auth.Authenticator;
import uk.nhs.nhsx.core.routing.Routing;
import uk.nhs.nhsx.core.routing.RoutingHandler;
import uk.nhs.nhsx.virology.order.TokensGenerator;
import uk.nhs.nhsx.virology.persistence.VirologyDynamoService;
import uk.nhs.nhsx.virology.result.VirologyResultRequest;
import uk.nhs.nhsx.virology.result.VirologyTokenGenRequest;

import static uk.nhs.nhsx.core.Jackson.deserializeMaybe;
import static uk.nhs.nhsx.core.auth.StandardAuthentication.awsAuthentication;
import static uk.nhs.nhsx.core.routing.Routing.*;
import static uk.nhs.nhsx.core.routing.StandardHandlers.withoutSignedResponses;

public class VirologyUploadHandler extends RoutingHandler {

    private static final Logger logger = LogManager.getLogger(VirologyUploadHandler.class);

    private final Routing.Handler handler;

    public VirologyUploadHandler() {
        this(Environment.fromSystem(), awsAuthentication(ApiName.TestResultUpload), virologyService());
    }

    public VirologyUploadHandler(Environment environment, Authenticator authenticator, VirologyService virologyService) {
        handler = withoutSignedResponses(
            environment,
            authenticator,
            routes(
                path(Method.POST, "/upload/virology-test/npex-result", (r) ->
                    handleTestResult(VirologyResultSource.Npex, virologyService, r)
                ),
                path(Method.POST, "/upload/virology-test/fiorano-result", (r) ->
                    handleTestResult(VirologyResultSource.Fiorano, virologyService, r)
                ),
                path(Method.POST, "/upload/virology-test/eng-result-tokengen", (r) ->
                    handleTokenGen(VirologyTokenExchangeSource.Eng, virologyService, r)
                ),
                path(Method.POST, "/upload/virology-test/wls-result-tokengen", (r) ->
                    handleTokenGen(VirologyTokenExchangeSource.Wls, virologyService, r)
                ),
                path(Method.POST, "/upload/virology-test/health", (r) ->
                    HttpResponses.ok()
                )
            )
        );
    }

    private static APIGatewayProxyResponseEvent handleTestResult(VirologyResultSource source,
                                                                 VirologyService virologyService,
                                                                 APIGatewayProxyRequestEvent request) {
        return deserializeMaybe(request.getBody(), VirologyResultRequest.class)
            .map(it -> {
                logger.info("{} sent virology ctaToken: {}, result: {}", source.name(), it.ctaToken, it.testResult);
                return virologyService.acceptTestResult(it).toHttpResponse();
            })
            .orElseGet(HttpResponses::unprocessableEntity);
    }

    private static APIGatewayProxyResponseEvent handleTokenGen(VirologyTokenExchangeSource source,
                                                               VirologyService virologyService,
                                                               APIGatewayProxyRequestEvent request) {
        return deserializeMaybe(request.getBody(), VirologyTokenGenRequest.class)
            .map(it -> {
                logger.info("{} sent virology token exchange result: {}", source.name(), it.testResult);
                var response = virologyService.acceptTestResultGeneratingTokens(it);
                return HttpResponses.ok(Jackson.toJson(response));
            })
            .orElse(HttpResponses.unprocessableEntity());
    }

    private static VirologyService virologyService() {
        return new VirologyService(
            new VirologyDynamoService(
                AmazonDynamoDBClientBuilder.defaultClient(),
                virologyConfig()
            ),
            new TokensGenerator(),
            SystemClock.CLOCK
        );
    }

    private static VirologyConfig virologyConfig() {
        return new VirologyConfig(
            System.getenv("test_orders_table"),
            System.getenv("test_results_table"),
            System.getenv("submission_tokens_table"),
            System.getenv("test_orders_index"),
            VirologyConfig.MAX_TOKEN_PERSISTENCE_RETRY_COUNT
        );
    }

    @Override
    public Routing.Handler handler() {
        return handler;
    }

    private enum VirologyResultSource {
        Npex, Fiorano
    }

    private enum VirologyTokenExchangeSource {
        Eng, Wls
    }
}
