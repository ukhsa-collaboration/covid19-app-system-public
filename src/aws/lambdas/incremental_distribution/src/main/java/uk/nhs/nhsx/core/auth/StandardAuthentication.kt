package uk.nhs.nhsx.core.auth

import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder
import uk.nhs.nhsx.core.aws.secretsmanager.AwsSecretManager
import uk.nhs.nhsx.core.aws.secretsmanager.CachingSecretManager
import uk.nhs.nhsx.core.events.Events
import java.util.regex.Pattern

object StandardAuthentication {
    private val pattern = Pattern.compile("^[\\w_-]{6,50}")

    fun awsAuthentication(apiName: ApiName, events: Events): Authenticator {
        val cachingApiKeyAuthorizer = CachingApiKeyAuthorizer(
            SecretManagerKeyAuthorizer(
                apiName,
                CachingSecretManager(AwsSecretManager(AWSSecretsManagerClientBuilder.defaultClient())),
                events
            )
        )

        return ApiKeyAuthenticator.authenticatingWithApiKey(
            events,
            CompositeApiKeyAuthorizer(
                apiKeyNameValidator(),
                TracingApiKeyAuthorizer(cachingApiKeyAuthorizer)
            )
        )
    }

    fun apiKeyNameValidator(): ApiKeyAuthorizer = ApiKeyAuthorizer { pattern.matcher(it.keyName).matches() }
}
