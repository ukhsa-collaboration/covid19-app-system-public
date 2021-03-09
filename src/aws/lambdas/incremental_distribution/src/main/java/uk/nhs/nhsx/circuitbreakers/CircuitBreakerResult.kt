package uk.nhs.nhsx.circuitbreakers

class CircuitBreakerResult private constructor(val type: ResultType, val message: String, val responseBody: String) {
    enum class ResultType {
        Ok, ValidationError, MissingPollingTokenError
    }

    companion object {
        @JvmStatic
        fun ok(responseBody: String) = CircuitBreakerResult(ResultType.Ok, "", responseBody)

        @JvmStatic
        fun missingPollingTokenError() = CircuitBreakerResult(
            ResultType.MissingPollingTokenError,
            "Circuit Breaker request submitted without approval token",
            ""
        )

        fun validationError() =
            CircuitBreakerResult(ResultType.ValidationError, "Circuit breaker request submitted was invalid", "")
    }
}
