package uk.nhs.nhsx.isolationpayment;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import uk.nhs.nhsx.core.Environment;
import uk.nhs.nhsx.core.SystemClock;
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

    public IsolationPaymentVerifyHandler() {
        this(Environment.fromSystem(), SystemClock.CLOCK);
    }

    public IsolationPaymentVerifyHandler(Environment environment, Supplier<Instant> clock) {
        this(isolationPaymentService(clock, environment));
    }

    public IsolationPaymentVerifyHandler(IsolationPaymentGatewayService service) {
        this.service = service;
    }

    private static IsolationPaymentGatewayService isolationPaymentService(Supplier<Instant> clock, Environment environment) {
        var persistence = new IsolationPaymentPersistence(
            AmazonDynamoDBClientBuilder.defaultClient(),
            environment.access.required(ISOLATION_TOKEN_TABLE)
        );
        var auditLogPrefix = environment.access.required(AUDIT_LOG_PREFIX);
        return new IsolationPaymentGatewayService(clock, persistence, auditLogPrefix);
    }

    @Override
    public IsolationResponse handleRequest(IsolationRequest input, Context context) {
        return Optional.ofNullable(input)
            .map(v -> v.ipcToken)
            .map(service::verifyIsolationToken)
            .orElseThrow(RuntimeException::new);
    }
}
