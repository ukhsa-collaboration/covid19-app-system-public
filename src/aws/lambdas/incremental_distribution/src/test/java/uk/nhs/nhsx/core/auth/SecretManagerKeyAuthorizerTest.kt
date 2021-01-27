package uk.nhs.nhsx.core.auth

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import uk.nhs.nhsx.core.aws.secretsmanager.SecretManager
import uk.nhs.nhsx.core.aws.secretsmanager.SecretName
import uk.nhs.nhsx.core.aws.secretsmanager.SecretValue
import java.util.*

class SecretManagerKeyAuthorizerTest {

    private val keyName = "test-api"
    private val secretName = SecretName.of("/" + ApiName.TestResultUpload.name + "/" + keyName)

    //multiple lines on purpose.
    private val secretHash = "$2a$12\$HyJCSwpZ9yoarm9JjlxH2OJQoMl3Mrtf5rIJNFVhBdvNp5LYsrjcq"
    private val keyValue = "13f17391-0c22-4a6a-badf-99c18a455ada"
    private val secretManager = mock(SecretManager::class.java)
    private val authenticator = SecretManagerKeyAuthorizer(ApiName.TestResultUpload, secretManager)

    @Test
    fun authenticationSucceedsIfSecretValueMatches() {
        secretManagerContainsSecretWithValue(secretName, SecretValue.of(secretHash))
        assertThat(authenticator.authorize(ApiKey.of(keyName, keyValue))).isTrue
        verify(secretManager, times(1)).getSecret(secretName)
    }

    @Test
    fun authenticationSucceedsIfSecretValueMatches_valuesFromGenerationScript() {
        secretManagerContainsSecretWithValue(SecretName.of("/testResultUpload/test-dev-20210119"), SecretValue.of("$2b$12$8FtS0UMWyhaSXPjMsjC54e1Uvp0bjt2Du6/J/JTUNVG4L4dERtgzS"))
        assertThat(authenticator.authorize(ApiKey.of("test-dev-20210119", "b2458c9462892c49ba3ba4ef5b2b6e8bf3b0ab090d3f3d8837506ddeda10370b"))).isTrue
    }

    @Test
    fun authenticationSucceedsIfSecretValueMatches_valuesFromGenerationScript2() {
        secretManagerContainsSecretWithValue(SecretName.of("/testResultUpload/test-dev-20210119"), SecretValue.of("$2b$12\$dMYIPxF6DODdfebKEHWRoO/a8JoQ1EVFPjN.GBb2TZUq99qfOFWpW"))
        assertThat(authenticator.authorize(ApiKey.of("test-dev-20210119", "ce5fcad42b300e2ddeb1c03e2364db456b8d3c321d8528be0b7b917df61df997"))).isTrue
    }

    @Test
    fun authenticationFailsIfSecretValueDoesNotMatch() {
        secretManagerContainsSecretWithValue(secretName, SecretValue.of("$2a$12\$vPJgAeuBHRiqVZWxRqp4X.6brfzVuYeG.lowrQltdptOVUzyPtFtm"))
        assertThat(authenticator.authorize(ApiKey.of(keyName, keyValue))).isFalse
        verify(secretManager, times(1)).getSecret(secretName)
    }

    @Test
    fun authenticationFailsIfSecretValueWasEmpty() {
        `when`(secretManager.getSecret(secretName)).thenReturn(Optional.empty())
        assertThat(authenticator.authorize(ApiKey.of(keyName, "v"))).isFalse
        verify(secretManager, times(1)).getSecret(secretName)
    }

    private fun secretManagerContainsSecretWithValue(name: SecretName, secretValue: SecretValue) {
        `when`(secretManager.getSecret(name)).thenReturn(Optional.of(secretValue))
    }
}