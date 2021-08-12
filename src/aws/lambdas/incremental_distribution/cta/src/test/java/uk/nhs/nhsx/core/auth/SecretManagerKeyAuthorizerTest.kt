package uk.nhs.nhsx.core.auth

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import uk.nhs.nhsx.core.aws.secretsmanager.SecretManager
import uk.nhs.nhsx.core.aws.secretsmanager.SecretName
import uk.nhs.nhsx.core.aws.secretsmanager.SecretValue
import uk.nhs.nhsx.core.events.RecordingEvents
import java.util.*

class SecretManagerKeyAuthorizerTest {

    private val keyName = "test-api"
    private val secretName = SecretName.of("/" + ApiName.TestResultUpload.keyName + "/" + keyName)

    //multiple lines on purpose.
    private val secretHash = "$2a$12\$HyJCSwpZ9yoarm9JjlxH2OJQoMl3Mrtf5rIJNFVhBdvNp5LYsrjcq"
    private val keyValue = "13f17391-0c22-4a6a-badf-99c18a455ada"
    private val secretManager = mockk<SecretManager>()
    private val events = RecordingEvents()
    private val authenticator = SecretManagerKeyAuthorizer(ApiName.TestResultUpload, secretManager, events)

    @Test
    fun `authentication succeeds if secret value matches`() {
        secretManagerContainsSecretWithValue(secretName, SecretValue.of(secretHash))
        expectThat(authenticator.authorize(ApiKey(keyName, keyValue))).isTrue()
        verify(exactly = 1) { secretManager.getSecret(secretName) }
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "$2b$12$8FtS0UMWyhaSXPjMsjC54e1Uvp0bjt2Du6/J/JTUNVG4L4dERtgzS,b2458c9462892c49ba3ba4ef5b2b6e8bf3b0ab090d3f3d8837506ddeda10370b",
            "$2b$12\$dMYIPxF6DODdfebKEHWRoO/a8JoQ1EVFPjN.GBb2TZUq99qfOFWpW,ce5fcad42b300e2ddeb1c03e2364db456b8d3c321d8528be0b7b917df61df997"
        ]
    )
    fun `authentication succeeds if secret value matches values from generation script`(
        secretValue: String,
        expected: String
    ) {
        secretManagerContainsSecretWithValue(
            SecretName.of("/testResultUpload/test-dev-20210119"),
            SecretValue.of(secretValue)
        )

        expectThat(authenticator.authorize(ApiKey("test-dev-20210119", expected))).isTrue()
    }

    @Test
    fun `authentication fails if secret value does not match`() {
        secretManagerContainsSecretWithValue(
            secretName,
            SecretValue.of("$2a$12\$vPJgAeuBHRiqVZWxRqp4X.6brfzVuYeG.lowrQltdptOVUzyPtFtm")
        )

        expectThat(authenticator.authorize(ApiKey(keyName, keyValue))).isFalse()
        verify(exactly = 1) { secretManager.getSecret(secretName) }
    }

    @Test
    fun `authentication fails if secret value was empty`() {
        every { secretManager.getSecret(secretName) } returns Optional.empty()
        expectThat(authenticator.authorize(ApiKey(keyName, "v"))).isFalse()
        verify(exactly = 1) { secretManager.getSecret(secretName) }
    }

    private fun secretManagerContainsSecretWithValue(name: SecretName, secretValue: SecretValue) {
        every { secretManager.getSecret(name) } returns Optional.of(secretValue)
    }
}
