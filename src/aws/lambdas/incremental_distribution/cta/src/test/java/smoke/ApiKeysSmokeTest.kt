package smoke

import org.apache.commons.lang3.RandomStringUtils
import org.http4k.cloudnative.env.Environment
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.FORBIDDEN
import org.junit.jupiter.api.Test
import smoke.actors.createHandler
import smoke.env.SmokeTests
import strikt.api.expectThat
import strikt.assertions.isEmpty
import uk.nhs.nhsx.testhelper.assertions.bodyString
import uk.nhs.nhsx.testhelper.assertions.hasStatus
import java.util.*

class ApiKeysSmokeTest {

    private val config = SmokeTests.loadConfig()
    private val client = createHandler(Environment.ENV)

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
        val uri = "${config.virology_kit_endpoint}/home-kit/order"
        val request = Request(Method.POST, uri)
            .header("Authorization", authHeader)

        return client(request)
    }

    private fun toAuthHeader(apiKey: String) = """Bearer ${Base64.getEncoder().encodeToString(apiKey.toByteArray())}"""

    private fun assertUnAuthorized(response: Response) {
        expectThat(response).hasStatus(FORBIDDEN).and {
            bodyString.isEmpty()
        }
    }
}
