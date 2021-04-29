package uk.nhs.nhsx.core.exceptions

enum class HttpStatusCode(val code: Int) {
    OK_200(200), CREATED_201(201), ACCEPTED_202(202), NO_CONTENT_204(204), FORBIDDEN_403(403), NOT_FOUND_404(404), METHOD_NOT_ALLOWED_405(
        405
    ),
    BAD_REQUEST_400(400), CONFLICT_409(409), UNPROCESSABLE_ENTITY_422(422), INTERNAL_SERVER_ERROR_500(500), SERVICE_UNAVAILABLE_503(
        503
    );

    override fun toString(): String {
        return String.format("HttpStatusCode{name=%s,code=%d}", name, code)
    }
}
