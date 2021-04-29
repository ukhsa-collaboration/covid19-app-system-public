package uk.nhs.nhsx.virology.tokengen

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.Json
import uk.nhs.nhsx.core.handler.DirectRequestCompleted
import uk.nhs.nhsx.core.handler.DirectRequestStarted
import uk.nhs.nhsx.core.events.CtaTokensGenerated
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.testhelper.ContextBuilder.TestContext
import uk.nhs.nhsx.virology.CtaTokensGenerationComplete
import uk.nhs.nhsx.domain.TestKit.LAB_RESULT
import uk.nhs.nhsx.virology.VirologyProcessorHandler
import uk.nhs.nhsx.domain.TestEndDate
import uk.nhs.nhsx.domain.TestResult.Positive
import uk.nhs.nhsx.virology.tokengen.CtaProcessorResult.Success
import java.io.ByteArrayOutputStream
import java.lang.Exception

class VirologyProcessorHandlerTest {

    private val service = mockk<VirologyProcessorService>()
    private val events = RecordingEvents()
    private val handler = VirologyProcessorHandler(events, service)

    @Test
    fun `event is of accepted type`() {
        val input = Json.toJson(
            mapOf(
                "testResult" to "POSITIVE",
                "testEndDate" to "2020-10-06T00:00:00Z",
                "testKit" to "LAB_RESULT",
                "numberOfTokens" to "1000"
            )
        ).byteInputStream()

        every { service.generateAndStoreTokens(any()) } returns Success(
            "some-file.zip",
            "some-message"
        )

        val out = ByteArrayOutputStream()

        handler.handleRequest(input, out, TestContext())

        assertThat(
            Json.readJsonOrNull(String(out.toByteArray())),
            equalTo(mapOf("result" to "success", "message" to "some-message", "filename" to "some-file.zip"))
        )

        val expectedEvent = CtaProcessorRequest(
            Positive,
            TestEndDate.of(2020, 10, 6),
            LAB_RESULT,
            1000
        )

        verify { service.generateAndStoreTokens(expectedEvent) }

        events.containsExactly(
            DirectRequestStarted::class,
            CtaTokensGenerated::class,
            CtaTokensGenerationComplete::class,
            DirectRequestCompleted::class
        )
    }

    @Test
    fun `throws when test end date is invalid`() {
        val input = Json.toJson(
            mapOf(
                "testResult" to "POSITIVE",
                "testEndDate" to "invalid",
                "numberOfTokens" to "1000"
            )
        ).byteInputStream()

        assertThat({ handler.handleRequest(input, ByteArrayOutputStream(), TestContext()) }, throws<Exception>())
    }

    @Test
    fun `throws when number of tokens is invalid`() {
        val input = Json.toJson(
            mapOf(
                "testResult" to "POSITIVE",
                "testEndDate" to "2020-10-06T00:00:00Z",
                "numberOfTokens" to "invalid"
            )
        ).byteInputStream()

        assertThat({ handler.handleRequest(input, ByteArrayOutputStream(), TestContext()) }, throws<Exception>())
    }

    @Test
    fun `throws when number of tokens is null`() {
        val input = Json.toJson(
            mapOf(
                "testResult" to "POSITIVE",
                "testEndDate" to "2020-10-06T00:00:00Z"
            )
        ).byteInputStream()

        assertThat({ handler.handleRequest(input, ByteArrayOutputStream(), TestContext()) }, throws<Exception>())
    }

    @Test
    fun `throws when test result is invalid`() {
        val input = Json.toJson(
            mapOf(
                "testResult" to "INVALID",
                "testEndDate" to "2020-10-06T00:00:00Z",
                "numberOfTokens" to "100"
            )
        ).byteInputStream()

        assertThat({ handler.handleRequest(input, ByteArrayOutputStream(), TestContext()) }, throws<Exception>())
    }
}

