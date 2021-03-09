package uk.nhs.nhsx.virology.tokengen

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.direct.DirectRequestCompleted
import uk.nhs.nhsx.core.direct.DirectRequestStarted
import uk.nhs.nhsx.core.events.CtaTokensGenerated
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.testhelper.ContextBuilder.TestContext
import uk.nhs.nhsx.testhelper.data.asInstant
import uk.nhs.nhsx.virology.CtaTokensGenerationComplete
import uk.nhs.nhsx.virology.TestKit.LAB_RESULT
import uk.nhs.nhsx.virology.VirologyProcessorHandler
import uk.nhs.nhsx.virology.result.TestEndDate
import uk.nhs.nhsx.virology.result.TestResult.*
import uk.nhs.nhsx.virology.tokengen.CtaProcessorResult.Success

class VirologyProcessorHandlerTest {

    private val service = mockk<VirologyProcessorService>()
    private val events = RecordingEvents()
    private val handler = VirologyProcessorHandler(service, events)

    @Test
    fun `event is of accepted type`() {
        val input = mapOf(
            "testResult" to "POSITIVE",
            "testEndDate" to "2020-10-06T00:00:00Z",
            "testKit" to "LAB_RESULT",
            "numberOfTokens" to "1000"
        )

        every { service.generateAndStoreTokens(any()) } returns Success(
            "some-file.zip",
            "some-message"
        )

        val result = handler.handleRequest(input, TestContext())

        assertThat(
            result,
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
        val input = mapOf(
            "testResult" to "POSITIVE",
            "testEndDate" to "invalid",
            "numberOfTokens" to "1000"
        )

        assertThatThrownBy { handler.handleRequest(input, TestContext()) }
            .isInstanceOf(RuntimeException::class.java)
    }

    @Test
    fun `throws when number of tokens is invalid`() {
        val input = mapOf(
            "testResult" to "POSITIVE",
            "testEndDate" to "2020-10-06T00:00:00Z",
            "numberOfTokens" to "invalid"
        )

        assertThatThrownBy { handler.handleRequest(input, TestContext()) }
            .isInstanceOf(RuntimeException::class.java)
    }

    @Test
    fun `throws when number of tokens is null`() {
        val input = mapOf(
            "testResult" to "POSITIVE",
            "testEndDate" to "2020-10-06T00:00:00Z"
        )

        assertThatThrownBy { handler.handleRequest(input, TestContext()) }
            .isInstanceOf(RuntimeException::class.java)
    }

    @Test
    fun `throws when test result is invalid`() {
        val input = mapOf(
            "testResult" to "INVALID",
            "testEndDate" to "2020-10-06T00:00:00Z",
            "numberOfTokens" to "100"
        )

        assertThatThrownBy { handler.handleRequest(input, TestContext()) }
            .isInstanceOf(RuntimeException::class.java)
    }
}

