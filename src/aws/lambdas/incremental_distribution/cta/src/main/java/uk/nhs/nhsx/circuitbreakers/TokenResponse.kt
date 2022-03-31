package uk.nhs.nhsx.circuitbreakers

data class TokenResponse(
    val approvalToken: String,
    val approval: String
)
