package uk.nhs.nhsx.core.auth

import uk.nhs.nhsx.core.aws.secretsmanager.AwsSecretManager
import uk.nhs.nhsx.core.aws.secretsmanager.CachingSecretManager
import uk.nhs.nhsx.core.aws.xray.Tracing
import uk.nhs.nhsx.core.events.Events
import java.util.regex.Pattern

object StandardAuthentication {
    private val pattern = Pattern.compile("^[\\w_-]{6,50}")

    @JvmStatic
    fun awsAuthentication(apiName: ApiName, events: Events): Authenticator {
        val cachingApiKeyAuthorizer = CachingApiKeyAuthorizer(
            SecretManagerKeyAuthorizer(
                apiName,
                CachingSecretManager(AwsSecretManager()),
                events
            )
        )

        return ApiKeyAuthenticator.authenticatingWithApiKey(
            events,
            CompositeApiKeyAuthorizer(
                apiKeyNameValidator(),
                Tracing.tracing("authentication", ApiKeyAuthorizer::class.java, cachingApiKeyAuthorizer)
            )
        )
    }

    fun apiKeyNameValidator(): ApiKeyAuthorizer = ApiKeyAuthorizer { pattern.matcher(it.keyName).matches() }
}
