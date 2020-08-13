package uk.nhs.nhsx.activationsubmission;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.natpryce.snodge.JsonMutator;
import org.junit.Test;
import uk.nhs.nhsx.ContextBuilder;
import uk.nhs.nhsx.ProxyRequestBuilder;
import uk.nhs.nhsx.activationsubmission.persist.Environment;
import uk.nhs.nhsx.activationsubmission.validate.ActivationCodeValidator;
import uk.nhs.nhsx.core.SystemClock;
import uk.nhs.nhsx.core.exceptions.HttpStatusCode;

import java.util.HashMap;
import java.util.function.Supplier;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.nhs.nhsx.matchers.ProxyResponseAssertions.*;

public class ActivationSubmissionHandlerTest {

    private boolean validates = true;

    private final ActivationCodeValidator validator = new FixedActivationCodeValidator(ActivationCode.of("1234"), () -> validates);
    private final Handler handler = new Handler((s) -> true, validator, (r, response) -> {
        response.getHeaders().put("signature", "sig");});

    @Test
    public void aCodeThatIsValid() throws Exception {

        validates = true;

        assertThat(
            responseFor(
                activationCodeRequest()
                    .withJson("{ \"activationCode\" : \"1234\"}")
                    .build()),
            hasStatus(HttpStatusCode.OK_200)
        );
    }

    @Test
    public void aCodeThatIsValidIsSigned() throws Exception {

        validates = true;

        assertThat(
            responseFor(
                activationCodeRequest()
                    .withJson("{ \"activationCode\" : \"1234\"}")
                    .build()),
            hasHeader("signature", equalTo("sig"))
        );
    }

    @Test
    public void aCodeThatIsNotValid() throws Exception {

        validates = false;

        assertThat(responseFor(
            activationCodeRequest()
                .withJson("{ \"activationCode\" : \"1234\"}")
                .build()),
            allOf(
                    hasStatus(HttpStatusCode.BAD_REQUEST_400),
                    hasHeader("signature", equalTo("sig"))
            )
        );
    }

    @Test
    public void invalidJson() throws Exception {
        assertThat(responseFor(
            activationCodeRequest()
                .withJson(" ")
                .build()),
            hasStatus(HttpStatusCode.BAD_REQUEST_400)
        );
    }

    @Test
    public void invalidContentType() throws Exception {
        assertThat(responseFor(
            activationCodeRequest()
                .withBody("{ \"activationCode\" : \"1234\"}")
                .build()),
            hasStatus(HttpStatusCode.BAD_REQUEST_400)
        );
    }
    @Test
    public void badRequestWhenEmptyBody() {
        APIGatewayProxyResponseEvent responseEvent = responseFor( activationCodeRequest()
                .withJson("{}").build());

        assertThat(responseEvent, hasStatus(HttpStatusCode.BAD_REQUEST_400));
        assertThat(responseEvent, hasBody(equalTo(null)));
    }

    @Test
    public void badRequestWhenInvalidJson() {
        APIGatewayProxyResponseEvent responseEvent = responseFor( activationCodeRequest()
                .withJson("{").build());

        assertThat(responseEvent, hasStatus(HttpStatusCode.BAD_REQUEST_400));
        assertThat(responseEvent, hasBody(equalTo(null)));
    }

    @Test
    public void badRequestWhenEmptyJson() {
        APIGatewayProxyResponseEvent responseEvent = responseFor( activationCodeRequest()
                .withJson("").build());

        assertThat(responseEvent, hasStatus(HttpStatusCode.BAD_REQUEST_400));
        assertThat(responseEvent, hasBody(equalTo(null)));
    }

    @Test
    public void purturbInputs() throws Exception {

        JsonMutator mutator = new JsonMutator();

        String originalJson = "{\"activationCode\":\"1234\"}";

        mutator.forStrings().mutate(originalJson, 100)
            .forEach(
                (j) -> {
                    if (!j.equals(originalJson)) {
                        assertThat("For Input " + j,
                            responseFor(activationCodeRequest()
                                .withJson(j)
                                .build()
                            ),
                            hasStatus(HttpStatusCode.BAD_REQUEST_400)
                        );
                    }
                });
    }

    @Test
    public void canAtLeastConstructMain() throws Exception {
        new Handler(Environment.fromEnvironment(Environment.Access.TEST.apply(
            new HashMap<String, String>() {{
                put("WORKSPACE", "something");
                put("SSM_KEY_ID_PARAMETER_NAME", "something");
        }})), SystemClock.CLOCK);
    }

    private ProxyRequestBuilder activationCodeRequest() {
        return ProxyRequestBuilder.request()
            .withPath("/activation/request")
            .withMethod(HttpMethod.POST)
            .withBearerToken("anything");
    }

    private APIGatewayProxyResponseEvent responseFor(APIGatewayProxyRequestEvent request) {
        return handler.handleRequest(
            request,
            ContextBuilder.context().build()
        );
    }

    public static class FixedActivationCodeValidator implements ActivationCodeValidator {

        private final ActivationCode expected;
        private final Supplier<Boolean> response;

        public FixedActivationCodeValidator(ActivationCode expected, Supplier<Boolean> response) {
            this.expected = expected;
            this.response = response;
        }

        public boolean validate(ActivationCode code) {
            if (code.equals(expected)) {
                return response.get();
            }
            return false;
        }
    }

}