@file:Suppress("BlockingMethodInNonBlockingContext")

package uk.nhs.nhsx.virology.tokengen

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isFailure
import uk.nhs.nhsx.core.Json
import uk.nhs.nhsx.core.events.CtaTokensGenerated
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.core.handler.DirectRequestCompleted
import uk.nhs.nhsx.core.handler.DirectRequestStarted
import uk.nhs.nhsx.domain.TestEndDate
import uk.nhs.nhsx.domain.TestKit.LAB_RESULT
import uk.nhs.nhsx.domain.TestResult.Positive
import uk.nhs.nhsx.testhelper.ContextBuilder.TestContext
import uk.nhs.nhsx.testhelper.assertions.containsExactly
import uk.nhs.nhsx.testhelper.assertions.isEqualToJson
import uk.nhs.nhsx.virology.CtaTokensGenerationComplete
import uk.nhs.nhsx.virology.VirologyProcessorHandler
import uk.nhs.nhsx.virology.tokengen.CtaProcessorResult.Success
import java.io.ByteArrayOutputStream

class VirologyProcessorHandlerTest {

    private val service = mockk<VirologyProcessorService> {
        every { generateAndStoreTokens(any()) } returns Success(
            "some-file.zip",
            "some-message"
        )
    }

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

        val out = ByteArrayOutputStream().also {
            handler.handleRequest(input, it, TestContext())
        }

        expectThat(out)
            .get(ByteArrayOutputStream::toByteArray)
            .get(::String)
            .isEqualToJson("""{"result":"success","message":"some-message","filename":"some-file.zip"}""")

        verify {
            service.generateAndStoreTokens(
                CtaProcessorRequest(
                    testResult = Positive,
                    testEndDate = TestEndDate.of(2020, 10, 6),
                    testKit = LAB_RESULT,
                    numberOfTokens = 1000
                )
            )
        }

        expectThat(events).containsExactly(
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

        expectCatching { handler.handleRequest(input, ByteArrayOutputStream(), TestContext()) }.isFailure()
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

        expectCatching { handler.handleRequest(input, ByteArrayOutputStream(), TestContext()) }.isFailure()
    }

    @Test
    fun `throws when number of tokens is null`() {
        val input = Json.toJson(
            mapOf(
                "testResult" to "POSITIVE",
                "testEndDate" to "2020-10-06T00:00:00Z"
            )
        ).byteInputStream()

        expectCatching { handler.handleRequest(input, ByteArrayOutputStream(), TestContext()) }.isFailure()
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

        expectCatching { handler.handleRequest(input, ByteArrayOutputStream(), TestContext()) }.isFailure()
    }
}

