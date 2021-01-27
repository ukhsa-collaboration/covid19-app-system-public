package uk.nhs.nhsx.core.auth

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
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
    fun cachesPositiveResult() {
        assertThat(authorizer.authorize(ApiKey.of("name", "value")), `is`(true))
        assertThat(count.get(), `is`(1))
        assertThat(authorizer.authorize(ApiKey.of("name", "value")), `is`(true))
        assertThat(count.get(), `is`(1))
    }

    @Test
    fun cachesNegativeResult() {
        result = false
        assertThat(authorizer.authorize(ApiKey.of("name", "value")), `is`(false))
        assertThat(count.get(), `is`(1))
        assertThat(authorizer.authorize(ApiKey.of("name", "value")), `is`(false))
        assertThat(count.get(), `is`(1))
    }

    @Test
    fun doesNotMixUpResultsByKeyName() {
        assertThat(authorizer.authorize(ApiKey.of("name", "value")), `is`(true))
        assertThat(count.get(), `is`(1))
        result = false
        expected = "somekey"
        assertThat(authorizer.authorize(ApiKey.of("somekey", "somevalue")), `is`(false))
        assertThat(count.get(), `is`(2))
    }

    @Test
    fun doesNotMixUpResultsByKeyValue() {
        assertThat(authorizer.authorize(ApiKey.of("name", "value")), `is`(true))
        assertThat(count.get(), `is`(1))
        result = false
        assertThat(authorizer.authorize(ApiKey.of("name", "othervalue")), `is`(false))
        assertThat(authorizer.authorize(ApiKey.of("name", "value")), `is`(true))
        assertThat(count.get(), `is`(2))
    }
}