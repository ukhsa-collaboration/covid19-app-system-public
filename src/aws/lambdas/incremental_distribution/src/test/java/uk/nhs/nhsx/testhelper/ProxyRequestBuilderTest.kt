package uk.nhs.nhsx.testhelper

import com.amazonaws.HttpMethod
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ProxyRequestBuilderTest {

    @Test
    fun `headers are case-insensitive`() {
        val request = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.GET)
            .withCustomOai("OAI")
            .withRequestId("REQUEST-ID")
            .withPath("/circuit-breaker/venue/resolution/abc123")
            .withBearerToken("anything")
            .build()


        assertThat(request.headers["request-id"]).isEqualTo("REQUEST-ID")
        assertThat(request.headers["REQUEST-ID"]).isEqualTo("REQUEST-ID")

    }
}
