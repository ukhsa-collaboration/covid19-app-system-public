package uk.nhs.nhsx.isolationpayment;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import uk.nhs.nhsx.core.Environment;
import uk.nhs.nhsx.core.SystemClock;
import uk.nhs.nhsx.core.events.Events;
import uk.nhs.nhsx.core.events.PrintingJsonEvents;
import uk.nhs.nhsx.isolationpayment.model.IsolationRequest;
import uk.nhs.nhsx.isolationpayment.model.IsolationResponse;

import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;

import static uk.nhs.nhsx.core.Environment.EnvironmentKey.string;

public class IsolationPaymentVerifyHandler implements RequestHandler<IsolationRequest, IsolationResponse> {

    private static final Environment.EnvironmentKey<String> ISOLATION_TOKEN_TABLE = string("ISOLATION_PAYMENT_TOKENS_TABLE");
    private static final Environment.EnvironmentKey<String> AUDIT_LOG_PREFIX = string("AUDIT_LOG_PREFIX");
    private final IsolationPaymentGatewayService service;
    private final Events events;

    @SuppressWarnings("unused")
    public IsolationPaymentVerifyHandler() {
        this(Environment.fromSystem(), SystemClock.CLOCK, new PrintingJsonEvents(SystemClock.CLOCK));
    }

    public IsolationPaymentVerifyHandler(Environment environment, Supplier<Instant> clock, Events events) {
        this(isolationPaymentService(environment, clock, events), events);
    }

    public IsolationPaymentVerifyHandler(IsolationPaymentGatewayService service, Events events) {
        this.service = service;
        this.events = events;
    }

    private static IsolationPaymentGatewayService isolationPaymentService(Environment environment, Supplier<Instant> clock, Events events) {
        var persistence = new IsolationPaymentPersistence(
            AmazonDynamoDBClientBuilder.defaultClient(),
            environment.access.required(ISOLATION_TOKEN_TABLE)
        );
        var auditLogPrefix = environment.access.required(AUDIT_LOG_PREFIX);
        return new IsolationPaymentGatewayService(clock, persistence, auditLogPrefix, events);
    }

    @Override
    public IsolationResponse handleRequest(IsolationRequest input, Context context) {
        return Optional.ofNullable(input)
            .map(v -> v.ipcToken)
            .map(service::verifyIsolationToken)
            .orElseThrow(RuntimeException::new);
    }
}
