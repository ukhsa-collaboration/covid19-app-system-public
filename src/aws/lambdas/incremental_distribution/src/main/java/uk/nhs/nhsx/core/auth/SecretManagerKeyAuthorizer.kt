package uk.nhs.nhsx.core.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import uk.nhs.nhsx.core.aws.secretsmanager.SecretManager
import uk.nhs.nhsx.core.aws.secretsmanager.SecretValue
import uk.nhs.nhsx.core.events.Events

class SecretManagerKeyAuthorizer(
    private val apiName: ApiName,
    private val secretManager: SecretManager,
    private val events: Events
) : ApiKeyAuthorizer {

    override fun authorize(key: ApiKey): Boolean =
        secretManager.getSecret(key.secretNameFrom(apiName))
            .map { verifyKeyValue(key, it) }
            .orElse(false)

    private fun verifyKeyValue(apiKey: ApiKey, secret: SecretValue): Boolean {
        val verified = BCrypt.verifyer()
            .verify(apiKey.keyValue.toByteArray(), secret.value.toByteArray())
            .verified

        if (!verified) {
            events(BCryptKeyVerificationFailure(apiKey.keyName))
        }

        return verified
    }
}
