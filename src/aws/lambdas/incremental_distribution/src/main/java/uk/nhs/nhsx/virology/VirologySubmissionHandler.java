package uk.nhs.nhsx.virology;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import uk.nhs.nhsx.core.Environment;
import uk.nhs.nhsx.core.HttpResponses;
import uk.nhs.nhsx.core.Jackson;
import uk.nhs.nhsx.core.SystemClock;
import uk.nhs.nhsx.core.auth.ApiName;
import uk.nhs.nhsx.core.auth.Authenticator;
import uk.nhs.nhsx.core.auth.ResponseSigner;
import uk.nhs.nhsx.core.routing.Routing;
import uk.nhs.nhsx.core.routing.Routing.*;
import uk.nhs.nhsx.core.routing.RoutingHandler;
import uk.nhs.nhsx.virology.exchange.CtaExchangeRequest;
import uk.nhs.nhsx.virology.lookup.VirologyLookupRequest;
import uk.nhs.nhsx.virology.order.VirologyRequestType;
import uk.nhs.nhsx.virology.order.TokensGenerator;
import uk.nhs.nhsx.virology.order.VirologyWebsiteConfig;
import uk.nhs.nhsx.virology.persistence.VirologyDynamoService;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

import static uk.nhs.nhsx.core.Jackson.deserializeMaybe;
import static uk.nhs.nhsx.core.Jackson.deserializeMaybeLogInfo;
import static uk.nhs.nhsx.core.StandardSigning.signResponseWithKeyGivenInSsm;
import static uk.nhs.nhsx.core.auth.StandardAuthentication.awsAuthentication;
import static uk.nhs.nhsx.core.routing.Routing.*;
import static uk.nhs.nhsx.core.routing.StandardHandlers.withSignedResponses;

/**
 * Test kit order Lambda and test result polling Lambda.
 * <p>
 * see /doc/design/api-contracts/virology-testing-api.md
 * see /doc/design/details/testkit-order-test-result-key-upload.md
 * <p>
 * Sample:
 * <pre>
 * $ rake secret:createmobile
 * ...
 * "Authorization": "Bearer [token]"
 * ...
 *
 * $ curl -v -H "Content-Type: application/json"  -H "Authorization: Bearer [token]" -d '' https://w9z3i7j656.execute-api.eu-west-2.amazonaws.com/virology-test/home-kit/order
 * {"websiteUrlWithQuery":"https://self-referral.test-for-coronavirus.service.gov.uk/cta-start?ctaToken=620466","tokenParameterValue":"620466","testResultPollingToken":"98cff3dd-882c-417b-a00a-350a205378c7","diagnosisKeySubmissionToken":"cf492966-756a-4ae0-b66e-bf728d72aa43"}* Closing connection 0
 *
 *
 * $ curl -v -H "Authorization: Bearer [token]" -H "Content-Type: application/json" -d '{"testResultPollingToken":"98cff3dd-882c-417b-a00a-350a205378c7"}' https://w9z3i7j656.execute-api.eu-west-2.amazonaws.com/virology-test/results
 * HTTP/2 204
 *
 * test result upload (see uk.nhs.nhsx.virology.VirologyUploadHandler for sample)
 *
 * $ curl -v -H "Authorization: Bearer [token] -H "Content-Type: application/json" -d '{"testResultPollingToken":"98cff3dd-882c-417b-a00a-350a205378c7"}' https://w9z3i7j656.execute-api.eu-west-2.amazonaws.com/virology-test/results
 * {"testEndDate":"2020-04-23T18:34:03Z","testResult":"POSITIVE"}
 * </pre>
 */
public class VirologySubmissionHandler extends RoutingHandler {

    private final Routing.Handler handler;
    private static final Duration defaultDelayDuration = Duration.ofSeconds(1);

    public VirologySubmissionHandler() {
        this(Environment.fromSystem(), SystemClock.CLOCK, defaultDelayDuration);
    }

    public VirologySubmissionHandler(Environment environment, Supplier<Instant> clock, Duration throttleDuration) {
        this(
            environment, 
            awsAuthentication(ApiName.Mobile),
            signResponseWithKeyGivenInSsm(clock, environment),
            virologyService(clock, environment),
            websiteConfig(environment),
            throttleDuration
        );
    }

    public VirologySubmissionHandler(Environment environment,
                                     Authenticator authenticator,
                                     ResponseSigner signer,
                                     VirologyService service,
                                     VirologyWebsiteConfig websiteConfig,
                                     Duration delayDuration) {
        handler = withSignedResponses(
            environment,
            authenticator,
            signer,
            routes(
                path(Method.POST, "/virology-test/home-kit/order", (r) ->
                    handleVirologyOrder(service, websiteConfig, VirologyRequestType.ORDER)),
                path(Method.POST, "/virology-test/home-kit/register", (r) ->
                    handleVirologyOrder(service, websiteConfig, VirologyRequestType.REGISTER)),
                path(Method.POST, "/virology-test/results", (r) ->
                    deserializeMaybe(r.getBody(), VirologyLookupRequest.class)
                        .map(it -> service.virologyLookupFor(it).toHttpResponse())
                        .orElse(HttpResponses.unprocessableEntity())),
                path(Method.POST, "/virology-test/cta-exchange", (r) ->
                    throttlingResponse(
                        delayDuration,
                        () -> deserializeMaybeLogInfo(r.getBody(), CtaExchangeRequest.class)
                            .map(it -> service.exchangeCtaToken(it).toHttpResponse())
                            .orElseGet(HttpResponses::badRequest)
                    )
                ),
                path(Method.POST, "/virology-test/health", (r) ->
                    HttpResponses.ok()
                )
            )
        );
    }

    private APIGatewayProxyResponseEvent handleVirologyOrder(VirologyService service,
                                                             VirologyWebsiteConfig websiteConfig,
                                                             VirologyRequestType order) {
        var response = service.handleTestOrderRequest(websiteConfig, order);
        return HttpResponses.ok(Jackson.toJson(response));
    }

    private static VirologyService virologyService(Supplier<Instant> clock, Environment environment) {
        return new VirologyService(
            new VirologyDynamoService(
                AmazonDynamoDBClientBuilder.defaultClient(),
                virologyConfig(environment)
            ),
            new TokensGenerator(),
            clock
        );
    }

    private static VirologyConfig virologyConfig(Environment environment) {
        return new VirologyConfig(
            environment.access.required("test_orders_table"),
            environment.access.required("test_results_table"),
            environment.access.required("submission_tokens_table"),
            environment.access.required("test_orders_index"),
            VirologyConfig.MAX_TOKEN_PERSISTENCE_RETRY_COUNT
        );
    }

    private static VirologyWebsiteConfig websiteConfig(Environment environment) {
        return new VirologyWebsiteConfig(
            environment.access.required("order_website"),
            environment.access.required("register_website")
        );
    }

    @Override
    public Routing.Handler handler() {
        return handler;
    }
}
