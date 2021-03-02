package smoke

import com.natpryce.hamkrest.assertion.assertThat
import org.apache.commons.lang3.RandomStringUtils
import org.http4k.client.JavaHttpClient
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Test
import smoke.env.SmokeTests
import java.util.Base64

class ApiKeysSmokeTest {

    private val config = SmokeTests.loadConfig()
    private val client = JavaHttpClient()

    @Test
    fun `empty auth header`() {
        assertUnAuthorized(sendRequestWith(""))
    }

    @Test
    fun `empty bearer`() {
        assertUnAuthorized(sendRequestWith("Bearer "))
    }

    @Test
    fun `not base 64 encoded auth header`() {
        val apiName = "name"
        val password = "pwd"
        val apiKey = "$apiName:$password"
        val authHeader = toAuthHeader(apiKey)

        assertUnAuthorized(sendRequestWith(authHeader))
    }

    @Test
    fun `long not base 64 encoded auth header`() {
        val authHeader = toAuthHeader(RandomStringUtils.randomAlphabetic(1000))

        assertUnAuthorized(sendRequestWith(authHeader))
    }

    @Test
    fun `long random auth header`() {
        val authHeader = RandomStringUtils.randomAlphabetic(1000)

        assertUnAuthorized(sendRequestWith(authHeader))
    }

    @Test
    fun `long random api name`() {
        val apiName = RandomStringUtils.randomAlphabetic(1000)
        val password = "pwd"
        val apiKey = "$apiName:$password"
        val authHeader = toAuthHeader(apiKey)

        assertUnAuthorized(sendRequestWith(authHeader))
    }

    @Test
    fun `long random pwd`() {
        val apiName = "name"
        val password = RandomStringUtils.randomAlphabetic(1000)
        val apiKey = "$apiName:$password"
        val authHeader = toAuthHeader(apiKey)

        assertUnAuthorized(sendRequestWith(authHeader))
    }

    @Test
    fun `random auth header characters`() {
        val authHeader = toAuthHeader(RandomStringUtils.randomAscii(50))

        assertUnAuthorized(sendRequestWith(authHeader))
    }

    @Test
    fun `random auth header characters not base 64 encoded`() {
        val authHeader = "Bearer " + RandomStringUtils.randomAscii(500)

        assertUnAuthorized(sendRequestWith(authHeader))
    }

    @Test
    fun `random api name characters`() {
        val apiName = RandomStringUtils.randomAscii(50)
        val password = "pwd"
        val apiKey = "$apiName:$password"
        val authHeader = toAuthHeader(apiKey)

        assertUnAuthorized(sendRequestWith(authHeader))
    }

    @Test
    fun `random pwd characters`() {
        val apiName = "name"
        val password = RandomStringUtils.randomAscii(50)
        val apiKey = "$apiName:$password"
        val authHeader = toAuthHeader(apiKey)

        assertUnAuthorized(sendRequestWith(authHeader))
    }

    private fun sendRequestWith(authHeader: String): Response {
        val uri = "${config.virologyKitEndpoint}/home-kit/order"
        val request = Request(Method.POST, uri)
            .header("Authorization", authHeader)

        return client(request)
    }

    private fun toAuthHeader(apiKey: String): String =
        "Bearer " + Base64.getEncoder().encodeToString(apiKey.toByteArray())

    private fun assertUnAuthorized(response: Response) {
        assertThat(response, hasStatus(Status.FORBIDDEN))
//        assertThat(response, !hasHeader("x-amz-meta-signature")) ?? do we need this???
//        assertThat(response, !hasHeader("x-amz-meta-signature-date"))  ?? do we need this???
        assertThat(response, hasBody(""))
    }
}
