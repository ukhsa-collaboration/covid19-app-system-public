package uk.nhs.nhsx.core;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.apache.http.entity.ContentType;
import uk.nhs.nhsx.core.exceptions.HttpStatusCode;

import java.util.TreeMap;

/*
 use this class to create responses so you know that headers will not be null...
 */
public class HttpResponses {

    public static APIGatewayProxyResponseEvent withStatusCode(HttpStatusCode code) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(code.code);
        response.setHeaders(new TreeMap<>(String.CASE_INSENSITIVE_ORDER));
        return response;
    }

    public static APIGatewayProxyResponseEvent withStatusCodeAndBody(HttpStatusCode code, String body) {
        APIGatewayProxyResponseEvent response = withStatusCode(code);
        response.setBody(body);
        return response;
    }

    public static APIGatewayProxyResponseEvent ok() {
        return withStatusCode(HttpStatusCode.OK_200);
    }

    public static APIGatewayProxyResponseEvent ok(String jsonBody) {
        APIGatewayProxyResponseEvent response = ok();
        response.setBody(jsonBody);
        response.getHeaders().put("content-type", ContentType.APPLICATION_JSON.getMimeType());
        return response;
    }

    public static APIGatewayProxyResponseEvent created(String jsonBody) {
        APIGatewayProxyResponseEvent response = withStatusCode(HttpStatusCode.CREATED_201);
        response.setBody(jsonBody);
        response.getHeaders().put("content-type", ContentType.APPLICATION_JSON.getMimeType());
        return response;
    }

    public static APIGatewayProxyResponseEvent accepted() {
        return withStatusCode(HttpStatusCode.ACCEPTED_202);
    }

    public static APIGatewayProxyResponseEvent accepted(String body) {
        APIGatewayProxyResponseEvent response = withStatusCode(HttpStatusCode.ACCEPTED_202);
        response.setBody(body);
        return response;
    }

    public static APIGatewayProxyResponseEvent noContent() {
        return withStatusCode(HttpStatusCode.NO_CONTENT_204);
    }

    public static APIGatewayProxyResponseEvent forbidden() {
        return withStatusCode(HttpStatusCode.FORBIDDEN_403);
    }

    public static APIGatewayProxyResponseEvent forbidden(String body) {
        APIGatewayProxyResponseEvent response = withStatusCode(HttpStatusCode.FORBIDDEN_403);
        response.setBody(body);
        return response;
    }

    public static APIGatewayProxyResponseEvent notFound() {
        return withStatusCode(HttpStatusCode.NOT_FOUND_404);
    }

    public static APIGatewayProxyResponseEvent notFound(String body) {
        return withStatusCodeAndBody(HttpStatusCode.NOT_FOUND_404, body);
    }

    public static APIGatewayProxyResponseEvent methodNotAllowed() {
        return withStatusCode(HttpStatusCode.METHOD_NOT_ALLOWED_405);
    }

    public static APIGatewayProxyResponseEvent unprocessableEntity() {
        return withStatusCode(HttpStatusCode.UNPROCESSABLE_ENTITY_422);
    }

    public static APIGatewayProxyResponseEvent unprocessableEntity(String body) {
        APIGatewayProxyResponseEvent response = withStatusCode(HttpStatusCode.UNPROCESSABLE_ENTITY_422);
        response.setBody(body);
        return response;
    }


    public static APIGatewayProxyResponseEvent internalServerError() {
        return withStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR_500);
    }

    public static APIGatewayProxyResponseEvent badRequest() {
        return withStatusCode(HttpStatusCode.BAD_REQUEST_400);
    }

    public static APIGatewayProxyResponseEvent conflict() {
        return withStatusCode(HttpStatusCode.CONFLICT_409);
    }

    public static APIGatewayProxyResponseEvent serviceUnavailable() {
        return withStatusCode(HttpStatusCode.SERVICE_UNAVAILABLE_503);
    }
}
