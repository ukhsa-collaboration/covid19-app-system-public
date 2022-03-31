package uk.nhs.nhsx.circuitbreakers

import uk.nhs.nhsx.circuitbreakers.CircuitBreakerResult.ResultType.MissingPollingTokenError
import uk.nhs.nhsx.circuitbreakers.CircuitBreakerResult.ResultType.Ok
import uk.nhs.nhsx.circuitbreakers.CircuitBreakerResult.ResultType.ValidationError

class CircuitBreakerResult private constructor(
    val type: ResultType,
    val message: String,
    val responseBody: String
) {
    enum class ResultType {
        Ok, ValidationError, MissingPollingTokenError
    }

    companion object {
        fun ok(responseBody: String) = CircuitBreakerResult(
            type = Ok,
            message = "",
            responseBody = responseBody
        )

        fun missingApprovalTokenError() = CircuitBreakerResult(
            type = MissingPollingTokenError,
            message = "Circuit Breaker request submitted without approval token",
            responseBody = ""
        )

        fun validationError() = CircuitBreakerResult(
            type = ValidationError,
            message = "Circuit breaker request submitted was invalid",
            responseBody = ""
        )
    }
}
