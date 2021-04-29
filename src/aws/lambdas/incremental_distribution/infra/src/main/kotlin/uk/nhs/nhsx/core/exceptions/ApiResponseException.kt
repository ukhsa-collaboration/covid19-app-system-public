package uk.nhs.nhsx.core.exceptions

class ApiResponseException(val statusCode: HttpStatusCode, message: String?) : RuntimeException(message)
