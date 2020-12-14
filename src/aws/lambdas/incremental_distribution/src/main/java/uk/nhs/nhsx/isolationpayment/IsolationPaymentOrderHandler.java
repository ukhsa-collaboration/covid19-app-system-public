package uk.nhs.nhsx.isolationpayment;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import uk.nhs.nhsx.core.Environment;
import uk.nhs.nhsx.core.HttpResponses;
import uk.nhs.nhsx.core.Jackson;
import uk.nhs.nhsx.core.SystemClock;
import uk.nhs.nhsx.core.auth.ApiName;
import uk.nhs.nhsx.core.auth.Authenticator;
import uk.nhs.nhsx.core.auth.ResponseSigner;
import uk.nhs.nhsx.core.routing.Routing;
import uk.nhs.nhsx.core.routing.RoutingHandler;
import uk.nhs.nhsx.isolationpayment.model.TokenGenerationRequest;
import uk.nhs.nhsx.isolationpayment.model.TokenUpdateRequest;

import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;

import static uk.nhs.nhsx.core.Environment.EnvironmentKey.*;
import static uk.nhs.nhsx.core.HttpResponses.created;
import static uk.nhs.nhsx.core.HttpResponses.ok;
import static uk.nhs.nhsx.core.StandardSigning.signResponseWithKeyGivenInSsm;
import static uk.nhs.nhsx.core.auth.StandardAuthentication.awsAuthentication;
import static uk.nhs.nhsx.core.routing.Routing.Method.POST;
import static uk.nhs.nhsx.core.routing.Routing.path;
import static uk.nhs.nhsx.core.routing.Routing.routes;
import static uk.nhs.nhsx.core.routing.StandardHandlers.withSignedResponses;

public class IsolationPaymentOrderHandler extends RoutingHandler {
    private static final Environment.EnvironmentKey<String> ISOLATION_TOKEN_TABLE = string("ISOLATION_PAYMENT_TOKENS_TABLE");
    private static final Environment.EnvironmentKey<String> ISOLATION_PAYMENT_WEBSITE = string("ISOLATION_PAYMENT_WEBSITE");
    private static final Environment.EnvironmentKey<Integer> TOKEN_EXPIRY_IN_WEEKS = integer("TOKEN_EXPIRY_IN_WEEKS");
    private static final Environment.EnvironmentKey<List<String>> COUNTRIES_WHITELISTED = strings("COUNTRIES_WHITELISTED");
    private static final Environment.EnvironmentKey<Boolean> TOKEN_CREATION_ENABLED = bool("TOKEN_CREATION_ENABLED");
    private static final Environment.EnvironmentKey<String> AUDIT_LOG_PREFIX = string("AUDIT_LOG_PREFIX");

    private final Environment environment;
    private final IsolationPaymentMobileService service;
    private final Routing.Handler handler;

    public IsolationPaymentOrderHandler() {
        this(Environment.fromSystem(), SystemClock.CLOCK);
    }

    public IsolationPaymentOrderHandler(Environment environment, Supplier<Instant> clock) {
        this(
            environment,
            awsAuthentication(ApiName.Mobile),
            signResponseWithKeyGivenInSsm(clock, environment),
            isolationPaymentService(clock, environment)
        );
    }

    public IsolationPaymentOrderHandler(Environment environment, Authenticator authenticator, ResponseSigner signer, IsolationPaymentMobileService service) {
        this.environment = environment;
        this.service = service;

        handler = withSignedResponses(
            environment,
            authenticator,
            signer,
            routes(
                path(POST, "/isolation-payment/ipc-token/create", this::createToken),
                path(POST, "/isolation-payment/ipc-token/update", this::updateToken),
                path(POST, "/isolation-payment/health", r -> ok())
            )
        );
    }

    private APIGatewayProxyResponseEvent createToken(APIGatewayProxyRequestEvent request) {
        boolean tokenCreationEnabled = environment.access.required(TOKEN_CREATION_ENABLED);
        if (!tokenCreationEnabled) {
            return HttpResponses.serviceUnavailable();
        }

        var requestBody = Jackson.deserializeMaybe(request.getBody(), TokenGenerationRequest.class);
        if (requestBody.isPresent()) {
            return created(Jackson.toJson(service.handleIsolationPaymentOrder(requestBody.get())));
        } else {
            return HttpResponses.badRequest();
        }
    }

    private APIGatewayProxyResponseEvent updateToken(APIGatewayProxyRequestEvent request) {
        boolean tokenCreationEnabled = environment.access.required(TOKEN_CREATION_ENABLED);
        if (!tokenCreationEnabled) {
            return HttpResponses.serviceUnavailable();
        }

        var requestBody = Jackson.deserializeMaybeValidating(request.getBody(), TokenUpdateRequest.class, TokenUpdateRequest::validator);
        if (requestBody.isPresent()) {
            return ok(Jackson.toJson(service.handleIsolationPaymentUpdate(requestBody.get())));
        }
        else {
            return HttpResponses.badRequest();
        }
    }

    private static IsolationPaymentMobileService isolationPaymentService(Supplier<Instant> clock, Environment environment) {
        var isolationDynamoService = new IsolationDynamoService(
            AmazonDynamoDBClientBuilder.defaultClient(),
            environment.access.required(ISOLATION_TOKEN_TABLE)
        );

        return new IsolationPaymentMobileService(
            clock,
            isolationDynamoService,
            environment.access.required(ISOLATION_PAYMENT_WEBSITE),
            environment.access.required(TOKEN_EXPIRY_IN_WEEKS),
            environment.access.required(COUNTRIES_WHITELISTED),
            environment.access.required(AUDIT_LOG_PREFIX)
        );
    }

    @Override
    public Routing.Handler handler() {
        return handler;
    }
}
