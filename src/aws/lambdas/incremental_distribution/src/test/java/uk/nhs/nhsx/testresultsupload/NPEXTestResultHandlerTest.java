package uk.nhs.nhsx.testresultsupload;


import com.amazonaws.HttpMethod;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.natpryce.snodge.JsonMutator;
import org.junit.Test;
import uk.nhs.nhsx.ProxyRequestBuilder;
import uk.nhs.nhsx.core.exceptions.HttpStatusCode;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;
import static uk.nhs.nhsx.ContextBuilder.aContext;
import static uk.nhs.nhsx.matchers.ProxyResponseAssertions.hasBody;
import static uk.nhs.nhsx.matchers.ProxyResponseAssertions.hasStatus;

public class NPEXTestResultHandlerTest {


    private final String payloadJson = "{\n" +
        "    \"ctaToken\": \"40500002323\",\n" +
        "    \"testEndDate\": \"2020-04-23T00:00:00Z\",\n" +
        "    \"testResult\": \"NEGATIVE\"\n" +
        "}";
    private final String correctPath = "/upload/virology-test/npex-result";
    private final String correctToken = "anything";

    @Test
    public void acceptsPayloadReturns202(){
        NPEXTestResultUploadService uploadService = mock(NPEXTestResultUploadService.class);
        Handler handler = new Handler((e) -> true, uploadService);
        APIGatewayProxyRequestEvent requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath(correctPath)
            .withBearerToken(correctToken)
            .withJson(payloadJson)
            .build();

        APIGatewayProxyResponseEvent responseEvent = handler.handleRequest(requestEvent, aContext());
        assertThat(responseEvent, hasStatus(HttpStatusCode.ACCEPTED_202));
        assertThat(responseEvent, hasBody(equalTo("successfully processed")));
    }

    @Test
    public void invalidPathReturns404(){
        NPEXTestResultUploadService uploadService = mock(NPEXTestResultUploadService.class);
        Handler handler = new Handler((e) -> true, uploadService);
        APIGatewayProxyRequestEvent requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath("/upload/incorrect/npex-result")
            .withBearerToken(correctToken)
            .withJson(payloadJson)
            .build();

        APIGatewayProxyResponseEvent responseEvent = handler.handleRequest(requestEvent, aContext());
        assertThat(responseEvent, hasStatus(HttpStatusCode.NOT_FOUND_404));
        assertThat(responseEvent, hasBody(equalTo(null)));
    }

    @Test
    public void invalidMethodReturns405(){
        NPEXTestResultUploadService uploadService = mock(NPEXTestResultUploadService.class);
        Handler handler = new Handler((e) -> true, uploadService);
        APIGatewayProxyRequestEvent requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.GET)
            .withPath(correctPath)
            .withBearerToken(correctToken)
            .withJson(payloadJson)
            .build();

        APIGatewayProxyResponseEvent responseEvent = handler.handleRequest(requestEvent, aContext());
        assertThat(responseEvent, hasStatus(HttpStatusCode.METHOD_NOT_ALLOWED_405));
        assertThat(responseEvent, hasBody(equalTo(null)));
    }

    @Test
    public void emptyBodyReturns400(){
        NPEXTestResultUploadService uploadService = mock(NPEXTestResultUploadService.class);
        Handler handler = new Handler((e) -> true, uploadService);
        APIGatewayProxyRequestEvent requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath(correctPath)
            .withBearerToken(correctToken)
            .withJson("")
            .build();

        APIGatewayProxyResponseEvent responseEvent = handler.handleRequest(requestEvent, aContext());
        assertThat(responseEvent, hasStatus(HttpStatusCode.UNPROCESSABLE_ENTITY_422));
        assertThat(responseEvent, hasBody(equalTo(null)));
    }

    @Test
    public void randomPayloadDoesnCause500(){
        NPEXTestResultUploadService uploadService = mock(NPEXTestResultUploadService.class);
        Handler handler = new Handler((e) -> true, uploadService);
        String originalJson = payloadJson;
        new JsonMutator()
            .forStrings()
            .mutate(originalJson, 100)
            .forEach(json -> {
                if (!json.equals(originalJson)) {
                    APIGatewayProxyRequestEvent request = requestEventWithPayload(json);
                    APIGatewayProxyResponseEvent response = handler.handleRequest(request, aContext());
                    assertThat(response, not(hasStatus(HttpStatusCode.INTERNAL_SERVER_ERROR_500))
                    );
                }
            });
    }

    private APIGatewayProxyRequestEvent requestEventWithPayload(String requestPayload){
        return ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath(correctPath)
            .withBearerToken(correctToken)
            .withJson(payloadJson)
            .build();
    }
}
