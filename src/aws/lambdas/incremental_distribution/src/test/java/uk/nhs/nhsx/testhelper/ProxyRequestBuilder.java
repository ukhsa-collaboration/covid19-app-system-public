package uk.nhs.nhsx.testhelper;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;

import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;

public class ProxyRequestBuilder {

    private final Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private HttpMethod method;
    private String path;
    private String body;
    private byte[] byteBody;

    public static ProxyRequestBuilder request() {
        return new ProxyRequestBuilder();
    }

    public ProxyRequestBuilder withMethod(HttpMethod method) {
        this.method = method;
        return this;
    }

    public ProxyRequestBuilder withPath(String path) {
        this.path = path;
        return this;
    }

    public ProxyRequestBuilder withJson(String json) {
        return withBody(json)
            .withHeader("Content-Type", "application/json");
    }

    public ProxyRequestBuilder withCsv(String csv) {
        return withBody(csv)
            .withHeader("Content-Type", "text/csv");
    }

    public ProxyRequestBuilder withBearerToken(String token) {
        return withHeader("Authorization", String.format("Bearer %s", token));
    }

    public ProxyRequestBuilder withHeader(String name, String value) {
        headers.put(name, value);
        return this;
    }

    public ProxyRequestBuilder withBody(String body) {
        this.body = body;
        return this;
    }

    public ProxyRequestBuilder withBody(byte[] bytes) {
        this.byteBody = bytes;
        return this;
    }

    public APIGatewayProxyRequestEvent build() {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();

        if (method != null)
            event.setHttpMethod(method.name());

        event.setPath(path);

        if (byteBody != null) {
            event.setBody(Base64.getEncoder().encodeToString(byteBody));
            event.setIsBase64Encoded(true);
        } else {
            event.setBody(body);
            event.setIsBase64Encoded(false);
        }
        event.setHeaders(headers);
        return event;
    }

    public ProxyRequestBuilder withRequestId(String id) {
        headers.put("request-id", id);
        return this;
    }

    public ProxyRequestBuilder withRandomRequestId() {
        headers.put("request-Id", "sdlkfjsdlkfjdflk");
        return this;
    }
}