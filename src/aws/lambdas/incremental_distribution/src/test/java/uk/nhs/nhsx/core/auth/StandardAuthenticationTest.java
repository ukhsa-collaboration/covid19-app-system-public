package uk.nhs.nhsx.core.auth;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class StandardAuthenticationTest {


    @Test
    public void anApi() throws Exception {
        assertSomeKeyNames(api());
    }

    @Test
    public void mobileApi() throws Exception {
        assertSomeKeyNames(api());
    }

    private void assertSomeKeyNames(ApiKeyAuthorizer authorizer) {
        assertThat(authorizer.authorize(ApiKey.of("mobile", "")), is(true));
        assertThat(authorizer.authorize(ApiKey.of("blah_c-_134", "")), is(true));
        assertThat(authorizer.authorize(ApiKey.of("third-party-integration-2020102", "")), is(true));
        assertThat(authorizer.authorize(ApiKey.of("-/_+=.@!", "")), is(false));
        assertThat(authorizer.authorize(ApiKey.of("©∞", "")), is(false));
        assertThat(authorizer.authorize(ApiKey.of("0123456789012345678900123456789012345678901234567890", "")), is(false));
    }

    private ApiKeyAuthorizer api() {
        return StandardAuthentication.apiKeyNameValidator();
    }
}