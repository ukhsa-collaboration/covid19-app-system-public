package uk.nhs.nhsx.circuitbreakers;

public class CircuitBreakerResult {

    enum ResultType {Ok, ValidationError, MissingPollingTokenError}

    public final ResultType type;
    public final String message;
    public final String responseBody;

    private CircuitBreakerResult(ResultType type, String message, String responseBody) {
        this.type = type;
        this.message = message;
        this.responseBody = responseBody;
    }

    public static CircuitBreakerResult ok(String responseBody) {
        return new CircuitBreakerResult(ResultType.Ok, "", responseBody);
    }

    public static CircuitBreakerResult missingPollingTokenError() {
        return new CircuitBreakerResult(ResultType.MissingPollingTokenError, "Circuit Breaker request submitted without approval token", "");
    }

    public static CircuitBreakerResult validationError() {
        return new CircuitBreakerResult(ResultType.ValidationError, "Circuit breaker request submitted was invalid", "");
    }
}
