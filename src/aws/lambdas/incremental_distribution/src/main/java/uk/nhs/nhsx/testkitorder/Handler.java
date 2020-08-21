package uk.nhs.nhsx.testkitorder;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import uk.nhs.nhsx.activationsubmission.persist.Environment;
import uk.nhs.nhsx.core.HttpResponses;
import uk.nhs.nhsx.core.SystemClock;
import uk.nhs.nhsx.core.auth.ApiName;
import uk.nhs.nhsx.core.auth.Authenticator;
import uk.nhs.nhsx.core.auth.ResponseSigner;
import uk.nhs.nhsx.core.routing.Routing;
import uk.nhs.nhsx.core.routing.Routing.Method;
import uk.nhs.nhsx.core.routing.RoutingHandler;
import uk.nhs.nhsx.testkitorder.lookup.TestLookupRequest;
import uk.nhs.nhsx.testkitorder.order.TestOrderResponseFactory;
import uk.nhs.nhsx.testkitorder.order.TestOrderResponseFactory.TestKitRequestType;
import uk.nhs.nhsx.testkitorder.order.TokensGenerator;

import java.time.Instant;
import java.util.function.Supplier;

import static uk.nhs.nhsx.core.Jackson.deserializeMaybe;
import static uk.nhs.nhsx.core.StandardSigning.signResponseWithKeyGivenInSsm;
import static uk.nhs.nhsx.core.auth.StandardAuthentication.awsAuthentication;
import static uk.nhs.nhsx.core.routing.Routing.path;
import static uk.nhs.nhsx.core.routing.Routing.routes;
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
 * test result upload (see uk.nhs.nhsx.testresultsupload.Handler for sample)
 *
 * $ curl -v -H "Authorization: Bearer [token] -H "Content-Type: application/json" -d '{"testResultPollingToken":"98cff3dd-882c-417b-a00a-350a205378c7"}' https://w9z3i7j656.execute-api.eu-west-2.amazonaws.com/virology-test/results
 * {"testEndDate":"2020-04-23T18:34:03Z","testResult":"POSITIVE"}
 * </pre>
 */
public class Handler extends RoutingHandler {

    private static final int MAX_TOKEN_PERSISTENCE_RETRY_COUNT = 3;

    private final Routing.Handler handler;

    public Handler() {
        this(Environment.fromSystem(), SystemClock.CLOCK);
    }

    public Handler(Environment environment, Supplier<Instant> clock) {
        this(
            awsAuthentication(ApiName.Mobile),
            signResponseWithKeyGivenInSsm(clock, environment),
            testKitOrderService(clock, environment)
        );
    }

    public Handler(Authenticator authenticator, ResponseSigner signer, TestKitOrderService service) {
        this.handler = withSignedResponses(
            authenticator,
            signer,
            routes(
                path(Method.POST, "/virology-test/results", (r) ->
                    deserializeMaybe(r.getBody(), TestLookupRequest.class)
                        .map(service::handleTestResultRequest)
                        .orElse(HttpResponses.unprocessableEntity())),
                path(Method.POST, "/virology-test/home-kit/order", (r) ->
                    service.handleTestOrderRequest(TestKitRequestType.ORDER)),
                path(Method.POST, "/virology-test/home-kit/register", (r) ->
                    service.handleTestOrderRequest(TestKitRequestType.REGISTER))
            )
        );
    }

    @Override
    public Routing.Handler handler() {
        return handler;
    }

    private static TestKitOrderService testKitOrderService(Supplier<Instant> clock, Environment environment) {
        var config = testOrderConfig(environment);
        return new TestKitOrderService(
            new TestKitOrderDynamoService(
                AmazonDynamoDBClientBuilder.defaultClient(),
                config
            ),
            new TestOrderResponseFactory(config.orderWebsite, config.registerWebsite),
            new TokensGenerator(),
            clock
        );
    }

    private static TestKitOrderConfig testOrderConfig(Environment environment) {
        return new TestKitOrderConfig(
            environment.access.required("test_orders_table"),
            environment.access.required("test_results_table"),
            environment.access.required("virology_submission_tokens_table"),
            environment.access.required("order_website"),
            environment.access.required("register_website"),
            MAX_TOKEN_PERSISTENCE_RETRY_COUNT
        );
    }
}
