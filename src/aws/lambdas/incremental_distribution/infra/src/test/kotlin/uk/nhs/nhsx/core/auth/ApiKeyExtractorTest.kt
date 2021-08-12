package uk.nhs.nhsx.core.auth

import org.http4k.base64Encode
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNull

class ApiKeyExtractorTest {

    @Test
    fun `handles api key with non base 64 encoding`() {
        expectThat(ApiKeyExtractor("Bearer name:value")).isNull()
    }

    @Test
    fun `handles empty auth header`() {
        expectThat(ApiKeyExtractor("")).isNull()
    }

    @Test
    fun `handles empty api key`() {
        expectThat(ApiKeyExtractor("Bearer ")).isNull()
    }

    @Test
    fun `handles api key with empty key name and key value`() {
        expectThat(ApiKeyExtractor("Bearer ${":".base64Encode()}")).isNull()
    }

    @Test
    fun `handles api key with empty key name`() {
        expectThat(ApiKeyExtractor("Bearer ${":value".base64Encode()}")).isNull()
    }

    @Test
    fun `handles api key with empty key value`() {
        expectThat(ApiKeyExtractor("Bearer ${"name:".base64Encode()}")).isNull()
    }

    @Test
    fun `handles api key with value containing colon`() {
        val authorizationHeader = "Bearer ${"name:value:blah".base64Encode()}"

        expectThat(ApiKeyExtractor(authorizationHeader)).isEqualTo(ApiKey("name", "value:blah"))
    }
}
