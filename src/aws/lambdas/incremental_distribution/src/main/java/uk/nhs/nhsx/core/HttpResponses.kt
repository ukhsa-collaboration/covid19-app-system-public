package uk.nhs.nhsx.core

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import org.apache.http.entity.ContentType.APPLICATION_JSON
import uk.nhs.nhsx.core.exceptions.HttpStatusCode
import uk.nhs.nhsx.core.exceptions.HttpStatusCode.ACCEPTED_202
import uk.nhs.nhsx.core.exceptions.HttpStatusCode.BAD_REQUEST_400
import uk.nhs.nhsx.core.exceptions.HttpStatusCode.CONFLICT_409
import uk.nhs.nhsx.core.exceptions.HttpStatusCode.CREATED_201
import uk.nhs.nhsx.core.exceptions.HttpStatusCode.FORBIDDEN_403
import uk.nhs.nhsx.core.exceptions.HttpStatusCode.INTERNAL_SERVER_ERROR_500
import uk.nhs.nhsx.core.exceptions.HttpStatusCode.METHOD_NOT_ALLOWED_405
import uk.nhs.nhsx.core.exceptions.HttpStatusCode.NOT_FOUND_404
import uk.nhs.nhsx.core.exceptions.HttpStatusCode.NO_CONTENT_204
import uk.nhs.nhsx.core.exceptions.HttpStatusCode.OK_200
import uk.nhs.nhsx.core.exceptions.HttpStatusCode.SERVICE_UNAVAILABLE_503
import uk.nhs.nhsx.core.exceptions.HttpStatusCode.UNPROCESSABLE_ENTITY_422
import java.lang.String.CASE_INSENSITIVE_ORDER
import java.util.*

/*
 use this class to create responses so you know that headers will not be null...
 */
object HttpResponses {
    fun withStatusCode(code: HttpStatusCode) = APIGatewayProxyResponseEvent().apply {
        statusCode = code.code
        headers = TreeMap(CASE_INSENSITIVE_ORDER)
    }

    fun withStatusCodeAndBody(code: HttpStatusCode, body: String?) = withStatusCode(code).apply {
        this.body = body
    }

    fun ok() = withStatusCode(OK_200)

    fun ok(jsonBody: String?): APIGatewayProxyResponseEvent = ok().apply {
        body = jsonBody
        headers["content-type"] = APPLICATION_JSON.mimeType
    }

    fun created(jsonBody: String?) = withStatusCode(CREATED_201).apply {
        body = jsonBody
        headers["content-type"] = APPLICATION_JSON.mimeType
    }

    fun accepted(body: String?): APIGatewayProxyResponseEvent = withStatusCode(ACCEPTED_202).apply {
        this.body = body
    }

    fun noContent() = withStatusCode(NO_CONTENT_204)

    fun forbidden() = withStatusCode(FORBIDDEN_403)

    fun notFound() = withStatusCode(NOT_FOUND_404)

    fun notFound(body: String?) = withStatusCodeAndBody(NOT_FOUND_404, body)

    fun methodNotAllowed() = withStatusCode(METHOD_NOT_ALLOWED_405)

    fun unprocessableEntity() = withStatusCode(UNPROCESSABLE_ENTITY_422)

    fun unprocessableEntity(body: String?) = withStatusCode(UNPROCESSABLE_ENTITY_422).apply {
        this.body = body
    }

    fun unprocessableEntityWithJson(jsonBody: String?) = withStatusCode(UNPROCESSABLE_ENTITY_422).apply {
        headers["content-type"] = APPLICATION_JSON.mimeType
        body = jsonBody
    }

    fun internalServerError() = withStatusCode(INTERNAL_SERVER_ERROR_500)

    fun badRequest() = withStatusCode(BAD_REQUEST_400)

    fun conflict() = withStatusCode(CONFLICT_409)

    fun serviceUnavailable() = withStatusCode(SERVICE_UNAVAILABLE_503)
}
