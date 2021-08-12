package uk.nhs.nhsx.testhelper

import com.amazonaws.HttpMethod.GET
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.hasEntry
import uk.nhs.nhsx.testhelper.ProxyRequestBuilder.request
import uk.nhs.nhsx.testhelper.assertions.AwsRuntimeAssertions.ProxyRequest.headers

class ProxyRequestBuilderTest {

    @Test
    fun `headers are case-insensitive`() {
        val request = request()
            .withMethod(GET)
            .withCustomOai("OAI")
            .withRequestId("REQUEST-ID")
            .withPath("/circuit-breaker/venue/resolution/abc123")
            .withBearerToken("anything")

        expectThat(request){
            headers.hasEntry("request-id", "REQUEST-ID")
            headers.hasEntry("REQUEST-ID", "REQUEST-ID")
        }
    }
}
