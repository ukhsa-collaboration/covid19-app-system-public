package uk.nhs.nhsx.core.auth;

import com.amazonaws.util.Base64;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.nhs.nhsx.core.auth.ApiKeyAuthenticator.authenticatingWithApiKey;

public class ApiKeyAuthenticatorTest {

    private static final String valid = "Bearer " + Base64.encodeAsString("k:v".getBytes(StandardCharsets.UTF_8));

    private final ApiKeyAuthenticator authenticator = authenticatingWithApiKey(k -> true);

    @Test
    public void usesAllAuthorizers() throws Exception {
        assertThat(authenticatingWithApiKey((k) -> true, (k) -> true).isAuthenticated(valid), is(true));
        assertThat(authenticatingWithApiKey((k) -> true, (k) -> false).isAuthenticated(valid), is(false));
        assertThat(authenticatingWithApiKey((k) -> false, (k) -> true).isAuthenticated(valid), is(false));
        assertThat(authenticatingWithApiKey((k) -> false, (k) -> false).isAuthenticated(valid), is(false));
    }

    @Test
    public void lazy() throws Exception {
        assertThat(authenticatingWithApiKey((k) -> false, (k) -> {
            throw new IllegalArgumentException();
        }).isAuthenticated(valid), is(false));
    }

    @Test
    public void handlesEmptyAuthHeader() {
        String authorizationHeader = "";

        assertThat(authenticator.isAuthenticated(authorizationHeader), is(false));
    }

    @Test
    public void handlesEmptyApiKey() {
        String apiKey = "";
        String authorizationHeader = "Bearer " + apiKey;

        assertThat(authenticator.isAuthenticated(authorizationHeader), is(false));
    }

    @Test
    public void handlesApiKeyWithEmptyKeyNameAndKeyValue() {
        String apiKey = apiKeyFrom("", "");
        String authorizationHeader = "Bearer " + apiKey;

        assertThat(authenticator.isAuthenticated(authorizationHeader), is(false));
    }

    @Test
    public void handlesApiKeyWithEmptyKeyName() {
        String apiKey = apiKeyFrom("", "value");
        String authorizationHeader = "Bearer " + apiKey;

        assertThat(authenticator.isAuthenticated(authorizationHeader), is(false));
    }

    @Test
    public void handlesApiKeyWithEmptyKeyValue() {
        String apiKey = apiKeyFrom("name", "");
        String authorizationHeader = "Bearer " + apiKey;

        assertThat(authenticator.isAuthenticated(authorizationHeader), is(false));
    }

    @Test
    public void handlesApiKeyWithNonBase64Encoding() {
        String apiKey = "name:value";
        String authorizationHeader = "Bearer " + apiKey;

        assertThat(authenticator.isAuthenticated(authorizationHeader), is(false));
    }

    @Test
    public void doesRightThingWhenKeyContainsAColon() {
        String apiKey = "name:value:blah";
        String authorizationHeader = "Bearer " + apiKey;

        ApiKeyAuthenticator authenticator = authenticatingWithApiKey(k -> {
            assertThat(k.keyName, equalTo(apiKey));
            return true;
        });

        assertThat(authenticator.isAuthenticated(authorizationHeader), is(false));
    }

    
    
    private String apiKeyFrom(String keyName, String keyValue) {
        String keyNameValue = keyName + ":" + keyValue;
        return java.util.Base64.getEncoder().encodeToString(keyNameValue.getBytes());
    }
}