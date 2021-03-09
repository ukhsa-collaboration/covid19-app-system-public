package uk.nhs.nhsx.highriskvenuesupload

sealed class VenuesUploadResult {
    data class Success(val message: String) : VenuesUploadResult()
    data class ValidationError(val message: String) : VenuesUploadResult()
    companion object {
        fun ok(): VenuesUploadResult = Success("successfully uploaded")
        fun validationError(message: String): VenuesUploadResult = ValidationError(message)
    }
}
