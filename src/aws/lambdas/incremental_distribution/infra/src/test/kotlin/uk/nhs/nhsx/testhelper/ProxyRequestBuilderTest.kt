package uk.nhs.nhsx.testhelper

import com.amazonaws.HttpMethod.GET
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.testhelper.ProxyRequestBuilder.request

class ProxyRequestBuilderTest {

    @Test
    fun `headers are case-insensitive`() {
        val request = request()
            .withMethod(GET)
            .withCustomOai("OAI")
            .withRequestId("REQUEST-ID")
            .withPath("/circuit-breaker/venue/resolution/abc123")
            .withBearerToken("anything")

        assertThat(request.headers["request-id"]).isEqualTo("REQUEST-ID")
        assertThat(request.headers["REQUEST-ID"]).isEqualTo("REQUEST-ID")

    }
}
