package uk.nhs.nhsx.core.auth

import com.amazonaws.xray.AWSXRay
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isTrue
import java.io.FileNotFoundException

class TracingApiKeyAuthorizerTest {

    @Test
    fun `invokes delegate and returns correct result`() {
        inDummySegment {
            val outcome = TracingApiKeyAuthorizer { true }
                .authorize(ApiKey("name", "value"))

            expectThat(outcome).isTrue()
        }
    }

    @Test
    fun `throws correct exception`() {
        expectThrows<FileNotFoundException> {
            inDummySegment {
                TracingApiKeyAuthorizer { throw FileNotFoundException("should not get converted to invocation/undeclared throwable exception") }
                    .authorize(ApiKey("name", "value"))
            }
        }
    }

    private fun inDummySegment(runnable: Runnable) {
        try {
            AWSXRay.getGlobalRecorder().beginNoOpSegment().use { runnable.run() }
        } finally {
            AWSXRay.endSegment()
        }
    }
}
