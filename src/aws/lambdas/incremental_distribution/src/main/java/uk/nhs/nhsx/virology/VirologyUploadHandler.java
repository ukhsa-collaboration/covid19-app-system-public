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
import uk.nhs.nhsx.virology.persistence.VirologyPersistenceService;
import uk.nhs.nhsx.virology.result.VirologyResultRequest;
import uk.nhs.nhsx.virology.result.VirologyTokenGenRequest;

import static uk.nhs.nhsx.core.Jackson.deserializeMaybeValidating;
import static uk.nhs.nhsx.core.auth.StandardAuthentication.awsAuthentication;
import static uk.nhs.nhsx.core.routing.Routing.Method.POST;
import static uk.nhs.nhsx.core.routing.Routing.*;
import static uk.nhs.nhsx.core.routing.StandardHandlers.withoutSignedResponses;

public class VirologyUploadHandler extends RoutingHandler {

    private static final Logger logger = LogManager.getLogger(VirologyUploadHandler.class);

    private final Routing.Handler handler;

    @SuppressWarnings("unused")
    public VirologyUploadHandler() {
        this(Environment.fromSystem(), awsAuthentication(ApiName.TestResultUpload), virologyService());
    }

    public VirologyUploadHandler(Environment environment, Authenticator authenticator, VirologyService virologyService) {
        handler = withoutSignedResponses(
            environment,
            authenticator,
            routes(
                listOf(
                    path(POST, "/upload/virology-test/health", r ->
                        HttpResponses.ok()
                    ),
                    path(POST, "/upload/virology-test/npex-result", r ->
                        handleV1TestResult(VirologyResultSource.Npex, virologyService, r)
                    ),
                    path(POST, "/upload/virology-test/fiorano-result", r ->
                        handleV1TestResult(VirologyResultSource.Fiorano, virologyService, r)
                    ),
                    path(POST, "/upload/virology-test/eng-result-tokengen", r ->
                        handleV1TokenGen(VirologyTokenExchangeSource.Eng, virologyService, r)
                    ),
                    path(POST, "/upload/virology-test/wls-result-tokengen", r ->
                        handleV1TokenGen(VirologyTokenExchangeSource.Wls, virologyService, r)
                    )
                ),

                includeIf(VirologyV2ApiFeatureFlag.from(environment).isEnabled(),
                    path(POST, "/upload/virology-test/v2/npex-result", r ->
                        handleV2TestResult(VirologyResultSource.Npex, virologyService, r)
                    ),
                    path(POST, "/upload/virology-test/v2/fiorano-result", r ->
                        handleV2TestResult(VirologyResultSource.Fiorano, virologyService, r)
                    ),
                    path(POST, "/upload/virology-test/v2/eng-result-tokengen", r ->
                        handleV2TokenGen(VirologyTokenExchangeSource.Eng, virologyService, r)
                    ),
                    path(POST, "/upload/virology-test/v2/wls-result-tokengen", r ->
                        handleV2TokenGen(VirologyTokenExchangeSource.Wls, virologyService, r)
                    )
                )
            )
        );
    }

    private static APIGatewayProxyResponseEvent handleV1TestResult(VirologyResultSource source,
                                                                   VirologyService virologyService,
                                                                   APIGatewayProxyRequestEvent request) {
        return deserializeMaybeValidating(request.getBody(), VirologyResultRequest.class, VirologyResultRequest::v1TestKitValidator)
            .map(it -> {
                it.testKit = TestKit.LAB_RESULT;
                logger.info("{} sent virology ctaToken: {}, result: {}, testKit: {}",
                    source.name(), it.ctaToken, it.testResult, it.testKit);
                return virologyService.acceptTestResult(it).toHttpResponse();
            })
            .orElseGet(HttpResponses::unprocessableEntity);
    }

    private APIGatewayProxyResponseEvent handleV2TestResult(VirologyResultSource source,
                                                            VirologyService virologyService,
                                                            APIGatewayProxyRequestEvent request) {
        return deserializeMaybeValidating(request.getBody(), VirologyResultRequest.class, VirologyResultRequest::v2TestKitValidator)
            .map(it -> {
                logger.info("{} sent virology ctaToken: {}, result: {}, testKit: {}",
                    source.name(), it.ctaToken, it.testResult, it.testKit);
                return virologyService.acceptTestResult(it).toHttpResponse();
            })
            .orElseGet(HttpResponses::unprocessableEntity);
    }

    private static APIGatewayProxyResponseEvent handleV1TokenGen(VirologyTokenExchangeSource source,
                                                                 VirologyService virologyService,
                                                                 APIGatewayProxyRequestEvent request) {
        return deserializeMaybeValidating(request.getBody(), VirologyTokenGenRequest.class, VirologyTokenGenRequest::v1TestKitValidator)
            .map(it -> {
                it.testKit = TestKit.LAB_RESULT;
                logger.info("{} sent virology token exchange result: {}, testKit: {}",
                    source.name(), it.testResult, it.testKit);
                var response = virologyService.acceptTestResultGeneratingTokens(it);
                return HttpResponses.ok(Jackson.toJson(response));
            })
            .orElse(HttpResponses.unprocessableEntity());
    }

    private APIGatewayProxyResponseEvent handleV2TokenGen(VirologyTokenExchangeSource source,
                                                          VirologyService virologyService,
                                                          APIGatewayProxyRequestEvent request) {
        return deserializeMaybeValidating(request.getBody(), VirologyTokenGenRequest.class, VirologyTokenGenRequest::v2TestKitValidator)
            .map(it -> {
                logger.info("{} sent virology token exchange result: {}, testKit: {}",
                    source.name(), it.testResult, it.testKit);
                var response = virologyService.acceptTestResultGeneratingTokens(it);
                return HttpResponses.ok(Jackson.toJson(response));
            })
            .orElse(HttpResponses.unprocessableEntity());
    }

    private static VirologyService virologyService() {
        return new VirologyService(
            new VirologyPersistenceService(
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

    public enum VirologyResultSource {
        Npex, Fiorano
    }

    public enum VirologyTokenExchangeSource {
        Eng, Wls
    }
}
