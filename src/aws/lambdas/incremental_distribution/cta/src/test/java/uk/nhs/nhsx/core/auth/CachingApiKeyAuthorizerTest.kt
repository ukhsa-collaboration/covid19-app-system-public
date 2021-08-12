package uk.nhs.nhsx.core.auth

import org.junit.jupiter.api.Test
import strikt.api.expect
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import java.util.concurrent.atomic.AtomicInteger

class CachingApiKeyAuthorizerTest {

    private var result = true
    private val count = AtomicInteger()
    private var expected = "name"

    private val delegate = ApiKeyAuthorizer {
        count.incrementAndGet()
        expectThat(it.keyName).isEqualTo(expected)
        result
    }

    private val authorizer = CachingApiKeyAuthorizer(delegate)

    @Test
    fun `caches positive result`() {
        expect {
            repeat(5) {
                that(authorizer.authorize(ApiKey("name", "value")))
                    .describedAs("authorize")
                    .isTrue()

                that(count)
                    .get("cacheMiss", AtomicInteger::get)
                    .isEqualTo(1)
            }
        }
    }

    @Test
    fun `caches negative result`() {
        result = false

        expect {
            repeat(5) {
                that(authorizer.authorize(ApiKey("name", "value")))
                    .describedAs("authorize")
                    .isFalse()

                that(count)
                    .get("cacheMiss", AtomicInteger::get)
                    .isEqualTo(1)
            }
        }
    }

    @Test
    fun `does not mix up results by key name`() {
        expect {
            that(authorizer.authorize(ApiKey("name", "value")))
                .describedAs("authorize")
                .isTrue()

            that(count)
                .get("cacheMiss", AtomicInteger::get)
                .isEqualTo(1)

            result = false
            expected = "somekey"

            that(authorizer.authorize(ApiKey("somekey", "somevalue")))
                .describedAs("authorize")
                .isFalse()

            that(count)
                .get("cacheMiss", AtomicInteger::get)
                .isEqualTo(2)
        }
    }

    @Test
    fun `does not mix up results by key value`() {
        expect {
            that(authorizer.authorize(ApiKey("name", "value")))
                .describedAs("authorize")
                .isTrue()

            that(count)
                .get("cacheMiss", AtomicInteger::get)
                .isEqualTo(1)

            result = false

            that(authorizer.authorize(ApiKey("name", "othervalue")))
                .describedAs("authorize")
                .isFalse()

            that(authorizer.authorize(ApiKey("name", "value")))
                .describedAs("authorize")
                .isTrue()

            that(count)
                .get("cacheMiss", AtomicInteger::get)
                .isEqualTo(2)

        }
    }
}
