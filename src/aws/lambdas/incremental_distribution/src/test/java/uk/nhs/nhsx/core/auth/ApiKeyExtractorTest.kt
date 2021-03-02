package uk.nhs.nhsx.core.auth

import org.assertj.core.api.Assertions.assertThat
import org.http4k.base64Encode
import org.junit.jupiter.api.Test

class ApiKeyExtractorTest {

    @Test
    fun `handles api key with non base 64 encoding`() {
        assertThat(ApiKeyExtractor("Bearer name:value")).isNull()
    }

    @Test
    fun `handles empty auth header`() {
        assertThat(ApiKeyExtractor("")).isNull()
    }

    @Test
    fun `handles empty api key`() {
        assertThat(ApiKeyExtractor("Bearer ")).isNull()
    }

    @Test
    fun `handles api key with empty key name and key value`() {
        assertThat(ApiKeyExtractor("Bearer ${":".base64Encode()}")).isNull()
    }

    @Test
    fun `handles api key with empty key name`() {
        assertThat(ApiKeyExtractor("Bearer ${":value".base64Encode()}")).isNull()
    }

    @Test
    fun `handles api key with empty key value`() {
        assertThat(ApiKeyExtractor("Bearer ${"name:".base64Encode()}")).isNull()
    }

    @Test
    fun `handles api key with value containing colon`() {
        val authorizationHeader = "Bearer ${"name:value:blah".base64Encode()}"

        assertThat(ApiKeyExtractor(authorizationHeader)).isEqualTo(ApiKey("name", "value:blah"))
    }
}
