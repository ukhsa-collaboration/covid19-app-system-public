package uk.nhs.nhsx.isolationpayment.model

enum class TokenStateExternal(val value: String) {
    EXT_VALID("valid"), EXT_INVALID("invalid"), EXT_CONSUMED("consumed");
}
