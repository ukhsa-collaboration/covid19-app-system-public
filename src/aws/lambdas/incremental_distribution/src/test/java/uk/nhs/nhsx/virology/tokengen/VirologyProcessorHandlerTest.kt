package uk.nhs.nhsx.virology.tokengen

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.virology.VirologyProcessorHandler

class VirologyProcessorHandlerTest {

    private val service = mockk<VirologyProcessorService>()
    private val handler = VirologyProcessorHandler(service)

    @Test
    fun `event is of accepted type`() {
        val input = mapOf(
            "testResult" to "POSITIVE",
            "testEndDate" to "2020-10-06T00:00:00Z",
            "numberOfTokens" to "1000"
        )
        every { service.generateAndStoreTokens(any()) } returns CtaProcessorResult.Success("some-file.zip", "some-message")

        val json = handler.handleRequest(input, mockk())
        assertThat(json).containsSubsequence("some-file.zip")

        val expectedEvent = CtaProcessorRequest("POSITIVE", "2020-10-06T00:00:00Z", 1000)
        verify { service.generateAndStoreTokens(expectedEvent) }
    }

    @Test
    fun `throws when test end date is invalid`() {
        val input = mapOf(
            "testResult" to "POSITIVE",
            "testEndDate" to "invalid",
            "numberOfTokens" to "1000"
        )
        assertThatThrownBy { handler.handleRequest(input, mockk()) }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `throws when number of tokens is invalid`() {
        val input = mapOf(
            "testResult" to "POSITIVE",
            "testEndDate" to "2020-10-06T00:00:00Z",
            "numberOfTokens" to "invalid"
        )
        assertThatThrownBy { handler.handleRequest(input, mockk()) }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `throws when number of tokens is null`() {
        val input = mapOf(
            "testResult" to "POSITIVE",
            "testEndDate" to "2020-10-06T00:00:00Z",
            "numberOfTokens" to null
        )
        assertThatThrownBy { handler.handleRequest(input, mockk()) }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `throws when test result is invalid`() {
        val input = mapOf(
            "testResult" to "INVALID",
            "testEndDate" to "2020-10-06T00:00:00Z",
            "numberOfTokens" to "100"
        )
        assertThatThrownBy { handler.handleRequest(input, mockk()) }.isInstanceOf(IllegalArgumentException::class.java)
    }
}

