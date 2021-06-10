package uk.nhs.nhsx.highriskvenuesupload

sealed class VenuesParsingResult {
    data class Success(val json: String) : VenuesParsingResult()
    data class Failure(val message: String) : VenuesParsingResult()
    companion object {
        fun ok(json: String) = Success(json)
        fun failure(message: String) = Failure(message)
    }
}
