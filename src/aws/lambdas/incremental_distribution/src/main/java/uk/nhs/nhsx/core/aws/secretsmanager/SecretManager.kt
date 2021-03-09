package uk.nhs.nhsx.core.aws.secretsmanager

import java.util.*

interface SecretManager {
    fun getSecret(secretName: SecretName): Optional<SecretValue>
    fun getSecretBinary(secretName: SecretName): ByteArray
}
