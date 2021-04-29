package uk.nhs.nhsx.core.aws.secretsmanager

import com.amazonaws.services.secretsmanager.AWSSecretsManager
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest
import com.amazonaws.services.secretsmanager.model.AWSSecretsManagerException
import java.lang.RuntimeException
import java.util.*

class AwsSecretManager(private val client: AWSSecretsManager) : SecretManager {

    override fun getSecret(secretName: SecretName): Optional<SecretValue> = try {
        Optional
            .ofNullable(client.getSecretValue(GetSecretValueRequest().withSecretId(secretName.value)))
            .map { it.secretString }
            .map(SecretValue::of)
    } catch (e: AWSSecretsManagerException) {
        Optional.empty()
    }

    override fun getSecretBinary(secretName: SecretName): ByteArray = try {
        val getSecretValueResult = client.getSecretValue(GetSecretValueRequest().withSecretId(secretName.value))
        val buffer = ByteArray(getSecretValueResult.secretBinary.remaining())
        getSecretValueResult.secretBinary[buffer]
        buffer
    } catch (e: AWSSecretsManagerException) {
        throw RuntimeException(e)
    }
}
