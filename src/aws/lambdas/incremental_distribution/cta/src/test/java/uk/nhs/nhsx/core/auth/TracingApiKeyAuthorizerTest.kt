package uk.nhs.nhsx.core.auth

import com.amazonaws.xray.AWSXRay
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.io.FileNotFoundException

class TracingApiKeyAuthorizerTest {

    @Test
    fun `invokes delegate and returns correct result`() {
        inDummySegment {
            val outcome: Boolean = TracingApiKeyAuthorizer { true }
                .authorize(ApiKey("name", "value"))
            assertThat(outcome, equalTo(true))
        }
    }

    @Test
    fun `throws correct exception`() {
        assertThrows(FileNotFoundException::class.java) {
            inDummySegment {
                TracingApiKeyAuthorizer {
                    throw FileNotFoundException("should not get converted to invocation/undeclared throwable exception")
                }.authorize(ApiKey("name", "value"))
            }
        }
    }

    private fun inDummySegment(runnable: Runnable) {
        try {
            AWSXRay.beginDummySegment().use { runnable.run() }
        } finally {
            AWSXRay.endSegment()
        }
    }
}
