package uk.nhs.nhsx.core.auth

import com.amazonaws.xray.AWSXRay
import java.util.function.Supplier

class TracingApiKeyAuthorizer(private val delegate: ApiKeyAuthorizer) : ApiKeyAuthorizer {
    override fun authorize(key: ApiKey): Boolean {
        val delegate: Supplier<Boolean> = Supplier { delegate.authorize(key) }
        return AWSXRay.createSubsegment("authentication", delegate) ?: false
    }
}
