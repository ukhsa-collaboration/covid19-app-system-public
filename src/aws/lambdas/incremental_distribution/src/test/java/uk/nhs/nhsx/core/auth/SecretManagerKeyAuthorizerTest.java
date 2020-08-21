package uk.nhs.nhsx.core.auth;

import org.junit.Test;
import uk.nhs.nhsx.core.aws.secretsmanager.SecretManager;
import uk.nhs.nhsx.core.aws.secretsmanager.SecretName;
import uk.nhs.nhsx.core.aws.secretsmanager.SecretValue;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class SecretManagerKeyAuthorizerTest {

    private final String keyName = "test-api";
    private final SecretName secretName = SecretName.of("/" + ApiName.TestResultUpload.name + "/" + keyName);
    //multiple lines on purpose.
    private final String secretHash = "$2a$12$HyJCSwpZ9yoarm9JjlxH2OJQoMl3Mrtf5rIJNFVhBdvNp5LYsrjcq";
    private final String keyValue = "13f17391-0c22-4a6a-badf-99c18a455ada";

    private final SecretManager secretManager = mock(SecretManager.class);
    private final SecretManagerKeyAuthorizer authenticator = new SecretManagerKeyAuthorizer(ApiName.TestResultUpload, secretManager);
    
    @Test
    public void authenticationSucceedsIfSecretValueMatches() {
        secretManagerContainsSecretWithValue(secretName, SecretValue.of(secretHash));

        assertThat(authenticator.authorize(ApiKey.of(keyName, keyValue))).isTrue();
        verify(secretManager, times(1)).getSecret(secretName);
    }

    @Test
    public void authenticationSucceedsIfSecretValueMatches_valuesFromGenerationScript() {
        secretManagerContainsSecretWithValue(SecretName.of("/testResultUpload/test-dev-20210119"), SecretValue.of("$2b$12$8FtS0UMWyhaSXPjMsjC54e1Uvp0bjt2Du6/J/JTUNVG4L4dERtgzS"));

        assertThat(authenticator.authorize(ApiKey.of("test-dev-20210119", "b2458c9462892c49ba3ba4ef5b2b6e8bf3b0ab090d3f3d8837506ddeda10370b"))).isTrue();
    }

      @Test
    public void authenticationSucceedsIfSecretValueMatches_valuesFromGenerationScript2() {
        secretManagerContainsSecretWithValue(SecretName.of("/testResultUpload/test-dev-20210119"), SecretValue.of("$2b$12$dMYIPxF6DODdfebKEHWRoO/a8JoQ1EVFPjN.GBb2TZUq99qfOFWpW"));

        assertThat(authenticator.authorize(ApiKey.of("test-dev-20210119", "ce5fcad42b300e2ddeb1c03e2364db456b8d3c321d8528be0b7b917df61df997"))).isTrue();
    }

    @Test
    public void authenticationFailsIfSecretValueDoesNotMatch() {
        secretManagerContainsSecretWithValue(secretName, SecretValue.of("$2a$12$vPJgAeuBHRiqVZWxRqp4X.6brfzVuYeG.lowrQltdptOVUzyPtFtm"));

        assertThat(authenticator.authorize(ApiKey.of(keyName, keyValue))).isFalse();
        verify(secretManager, times(1)).getSecret(secretName);
    }

    @Test
    public void authenticationFailsIfSecretValueWasEmpty() {
        when(secretManager.getSecret(secretName)).thenReturn(Optional.empty());

        assertThat(authenticator.authorize(ApiKey.of(keyName, "v"))).isFalse();
        verify(secretManager, times(1)).getSecret(secretName);
    }

    private void secretManagerContainsSecretWithValue(SecretName name, SecretValue secretValue) {
        when(secretManager.getSecret(name)).thenReturn(Optional.of(secretValue));
    }

}