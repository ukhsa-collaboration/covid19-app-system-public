package uk.nhs.nhsx.core.routing;

import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.Test;
import uk.nhs.nhsx.core.HttpResponses;
import uk.nhs.nhsx.core.TestEnvironments;
import uk.nhs.nhsx.core.exceptions.HttpStatusCode;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.nhs.nhsx.ProxyRequestBuilder.request;
import static uk.nhs.nhsx.matchers.ProxyResponseAssertions.hasStatus;

public class StandardHandlersTest {

    @Test
    public void oaiHeaderIsRequiredToMatchWhenEnvironmentVariableIsSet_Set() throws Exception {

        Map<String, String> env = new HashMap<>() {{
            put("custom_oai", "bob");
        }};

        Routing.Handler handler = StandardHandlers.requiringCustomAccessIdentity(TestEnvironments.TEST.apply(env), r -> HttpResponses.ok());

        assertThat(handler.handle(request().build()), hasStatus(HttpStatusCode.FORBIDDEN_403));
        assertThat(handler.handle(request().withHeader("x-custom-oai", "bob").build()), hasStatus(HttpStatusCode.OK_200));
        assertThat(handler.handle(request().withHeader("x-custom-oai", "jim").build()), hasStatus(HttpStatusCode.FORBIDDEN_403));
        assertThat(handler.handle(request().withHeader("x-custom-oai", "").build()), hasStatus(HttpStatusCode.FORBIDDEN_403));
        assertThat(handler.handle(request().withHeader("x-custom-oai-something", "bob").build()), hasStatus(HttpStatusCode.FORBIDDEN_403));
    }

    @Test
    public void oaiHeaderIsRequiredToMatchWhenEnvironmentVariableIsSet_NotSet() throws Exception {

        Routing.Handler handler = StandardHandlers.requiringCustomAccessIdentity(TestEnvironments.EMPTY, r -> HttpResponses.ok());

        assertThat(handler.handle(request().build()), hasStatus(HttpStatusCode.OK_200));
        assertThat(handler.handle(request().withHeader("x-custom-oai", "bob").build()), hasStatus(HttpStatusCode.OK_200));
    }

    @Test
    public void requiringAuthorizationHeader_supplied() throws Exception {
        Routing.Handler handler = StandardHandlers.requiringAuthorizationHeader(r -> HttpResponses.ok());
        assertThat(handler.handle(request().withHeader("authorization", "something").build()), hasStatus(HttpStatusCode.OK_200));
    }

    @Test
    public void requiringAuthorizationHeader_notSupplied() throws Exception {
        Routing.Handler handler = StandardHandlers.requiringAuthorizationHeader(r -> HttpResponses.ok());
        assertThat(handler.handle(request().build()), hasStatus(HttpStatusCode.FORBIDDEN_403));
    }

    @Test
    public void authorisedBy_notSupplied() throws Exception {
        Routing.Handler handler = StandardHandlers.authorisedBy(authorizationHeader -> true, r -> HttpResponses.ok());
        assertThat(handler.handle(request().build()), hasStatus(HttpStatusCode.FORBIDDEN_403));
    }

    @Test
    public void authorisedBy_authorised() throws Exception {
        Routing.Handler handler = StandardHandlers.authorisedBy(authorizationHeader -> true, r -> HttpResponses.ok());
        assertThat(handler.handle(request().withHeader("authorization", "something").build()), hasStatus(HttpStatusCode.OK_200));
    }

    @Test
    public void authorisedBy_notAuthorised() throws Exception {
        Routing.Handler handler = StandardHandlers.authorisedBy(authorizationHeader -> false, r -> HttpResponses.ok());
        assertThat(handler.handle(request().withHeader("authorization", "something").build()), hasStatus(HttpStatusCode.FORBIDDEN_403));
    }

    @Test
    public void expectingContentType_suppliedCorrect() throws Exception {
        Routing.Handler handler = StandardHandlers.expectingContentType(ContentType.APPLICATION_JSON, r -> HttpResponses.ok());
        assertThat(handler.handle(request().withHeader("Content-Type", "application/json").build()), hasStatus(HttpStatusCode.OK_200));
        assertThat(handler.handle(request().withHeader("Content-Type", "application/json;charset=utf-8").build()), hasStatus(HttpStatusCode.OK_200));
    }

    @Test
    public void expectingContentType_suppliedIncorrect() throws Exception {
        Routing.Handler handler = StandardHandlers.expectingContentType(ContentType.APPLICATION_JSON, r -> HttpResponses.ok());
        assertThat(handler.handle(request().withHeader("Content-Type", "text/html").build()), hasStatus(HttpStatusCode.UNPROCESSABLE_ENTITY_422));
        assertThat(handler.handle(request().withHeader("Content-Type", "application/vnd.ms-excel").build()), hasStatus(HttpStatusCode.UNPROCESSABLE_ENTITY_422));
    }

    @Test
    public void expectingContentType_notSupplied() throws Exception {
        Routing.Handler handler = StandardHandlers.expectingContentType(ContentType.APPLICATION_JSON, r -> HttpResponses.ok());
        assertThat(handler.handle(request().build()), hasStatus(HttpStatusCode.BAD_REQUEST_400));
    }

    @Test
    public void filteringWhileMaintenanceModeEnabled_suppliedTrue() throws Exception {
        Map<String, String> env = Map.of("MAINTENANCE_MODE", "TRUE");
        Routing.Handler handler = StandardHandlers.filteringWhileMaintenanceModeEnabled(TestEnvironments.TEST.apply(env), r -> HttpResponses.ok());
        assertThat(handler.handle(request().build()), hasStatus(HttpStatusCode.SERVICE_UNAVAILABLE_503));
    }

    @Test
    public void filteringWhileMaintenanceModeEnabled_suppliedFalse() throws Exception {
        Map<String, String> env = Map.of("MAINTENANCE_MODE", "FALSE");
        Routing.Handler handler = StandardHandlers.filteringWhileMaintenanceModeEnabled(TestEnvironments.TEST.apply(env), r -> HttpResponses.ok());
        assertThat(handler.handle(request().build()), hasStatus(HttpStatusCode.OK_200));
    }

    @Test
    public void filteringWhileMaintenanceModeEnabled_suppliedEmpty() throws Exception {
        Map<String, String> env = Map.of("MAINTENANCE_MODE", "");
        Routing.Handler handler = StandardHandlers.filteringWhileMaintenanceModeEnabled(TestEnvironments.TEST.apply(env), r -> HttpResponses.ok());
        assertThat(handler.handle(request().build()), hasStatus(HttpStatusCode.OK_200));
    }

    @Test
    public void handlesEmptyAuthHeader() {
        String authorizationHeader = "";
        assertThat(StandardHandlers.apiKeyNameFrom(authorizationHeader), is(Optional.empty()));
    }

    @Test
    public void handlesEmptyApiKey() {
        String apiKey = "";
        String authorizationHeader = "Bearer " + apiKey;
        assertThat(StandardHandlers.apiKeyNameFrom(authorizationHeader), is(Optional.empty()));
    }

    @Test
    public void handlesApiKeyWithNonBase64Encoding() {
        String apiKey = "name:value";
        String authorizationHeader = "Bearer " + apiKey;
        assertThat(StandardHandlers.apiKeyNameFrom(authorizationHeader), is(Optional.empty()));
    }



}