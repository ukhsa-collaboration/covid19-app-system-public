package uk.nhs.nhsx.isolationpayment.model

enum class TokenStateInternal(val value: String) {
    INT_CREATED("created"), INT_UPDATED("valid");
}

fun IsolationToken.isStateEqual(state: TokenStateInternal) = tokenStatus == state.value
fun IsolationToken.isStateNotEqual(state: TokenStateInternal) = !isStateEqual(state)
