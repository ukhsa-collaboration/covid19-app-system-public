package uk.nhs.nhsx.circuitbreakers

import uk.nhs.nhsx.circuitbreakers.CircuitBreakerResult.Companion.missingApprovalTokenError
import uk.nhs.nhsx.circuitbreakers.CircuitBreakerResult.Companion.ok
import uk.nhs.nhsx.core.Json.toJson
import uk.nhs.nhsx.core.aws.ssm.Parameter

class CircuitBreakerService(
    private val initial: Parameter<ApprovalStatus>,
    private val poll: Parameter<ApprovalStatus>
) {
    fun getApprovalToken() = ok(toJson(TokenResponse(ApprovalTokenGenerator(), initial.value().statusName)))

    fun getResolution(path: String?) = when (ApprovalTokenExtractor(path)) {
        null -> missingApprovalTokenError()
        else -> ok(toJson(ResolutionResponse(poll.value().statusName)))
    }
}

