package uk.nhs.nhsx.circuitbreakers

import uk.nhs.nhsx.circuitbreakers.CircuitBreakerResult.Companion.missingPollingTokenError
import uk.nhs.nhsx.circuitbreakers.CircuitBreakerResult.Companion.ok
import uk.nhs.nhsx.circuitbreakers.utils.TokenGenerator.token
import uk.nhs.nhsx.core.Json.toJson
import uk.nhs.nhsx.core.aws.ssm.Parameter
import java.util.Optional
import java.util.function.Predicate
import java.util.regex.Pattern

class CircuitBreakerService(
    private val initial: Parameter<ApprovalStatus>,
    private val poll: Parameter<ApprovalStatus>
) {
    fun getApprovalToken() = ok(toJson(TokenResponse(token, initial.value().statusName)))

    fun getResolution(path: String?) = when {
        extractPollingToken(path).isEmpty -> missingPollingTokenError()
        else -> ok(toJson(ResolutionResponse(poll.value().statusName)))
    }

    companion object {
        private val CIRCUIT_BREAKER_RESOLUTION_PATH_PATTERN =
            Pattern.compile("/circuit-breaker/[\\-\\w]+/resolution/(?<pollingToken>.*)")

        fun extractPollingToken(path: String?): Optional<String> = Optional.ofNullable(path).flatMap {
            val matcher = CIRCUIT_BREAKER_RESOLUTION_PATH_PATTERN.matcher(it)
            if (matcher.matches()) {
                val pollingToken = matcher.group("pollingToken")
                when {
                    pollingToken != null && pollingToken.isNotEmpty() -> Optional.of(pollingToken)
                    else -> Optional.empty()
                }
            } else Optional.empty()
        }

        fun startsWith(path: String?) = Predicate { candidate: String -> candidate.startsWith(path!!) }
    }
}
