package uk.nhs.nhsx.core.auth

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

class CachingApiKeyAuthorizerTest {

    private var result = true
    private val count = AtomicInteger()
    private var expected = "name"
    private val delegate = ApiKeyAuthorizer { k: ApiKey ->
        count.incrementAndGet()
        assertThat(k.keyName, equalTo(expected))
        result
    }
    private val authorizer: ApiKeyAuthorizer = CachingApiKeyAuthorizer(delegate)

    @Test
    fun `caches positive result`() {
        assertThat(authorizer.authorize(ApiKey("name", "value")), equalTo(true))
        assertThat(count.get(), equalTo(1))
        assertThat(authorizer.authorize(ApiKey("name", "value")), equalTo(true))
        assertThat(count.get(), equalTo(1))
    }

    @Test
    fun `caches negative result`() {
        result = false
        assertThat(authorizer.authorize(ApiKey("name", "value")), equalTo(false))
        assertThat(count.get(), equalTo(1))
        assertThat(authorizer.authorize(ApiKey("name", "value")), equalTo(false))
        assertThat(count.get(), equalTo(1))
    }

    @Test
    fun `does not mix up results by key name`() {
        assertThat(authorizer.authorize(ApiKey("name", "value")), equalTo(true))
        assertThat(count.get(), equalTo(1))
        result = false
        expected = "somekey"
        assertThat(authorizer.authorize(ApiKey("somekey", "somevalue")), equalTo(false))
        assertThat(count.get(), equalTo(2))
    }

    @Test
    fun `does not mix up results by key value`() {
        assertThat(authorizer.authorize(ApiKey("name", "value")), equalTo(true))
        assertThat(count.get(), equalTo(1))
        result = false
        assertThat(authorizer.authorize(ApiKey("name", "othervalue")), equalTo(false))
        assertThat(authorizer.authorize(ApiKey("name", "value")), equalTo(true))
        assertThat(count.get(), equalTo(2))
    }
}
