package uk.nhs.nhsx.virology.tokengen

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.virology.TestKit
import uk.nhs.nhsx.virology.VirologyService
import uk.nhs.nhsx.virology.result.VirologyTokenGenRequest
import uk.nhs.nhsx.virology.result.VirologyTokenGenResponse
import java.time.Instant
import java.util.function.Supplier

class VirologyProcessorServiceTest {

    private val clock = Supplier { Instant.ofEpochSecond(0) }
    private val virologyService = mockk<VirologyService>()
    private val store = mockk<VirologyProcessorStore>()
    private val maxRetryCount: Int = 3
    private val processorService = VirologyProcessorService(virologyService, store, clock, maxRetryCount, RecordingEvents())

    private val r1 = VirologyTokenGenResponse.of("pesddgrq")
    private val r2 = VirologyTokenGenResponse.of("gve9v72v")
    private val r3 = VirologyTokenGenResponse.of("fveeqkrn")

    private val csvSlot = slot<CtaTokensCsv>()
    private val zipSlot = slot<CtaTokensZip>()
    private val tokenGenRequest = VirologyTokenGenRequest("POSITIVE", "2020-10-06T00:00:00Z", TestKit.LAB_RESULT)
    private val multiTokenEvent = CtaProcessorRequest("POSITIVE", "2020-10-06T00:00:00Z", "LAB_RESULT",3)
    private val singleTokenEvent = CtaProcessorRequest("POSITIVE", "2020-10-06T00:00:00Z", "LAB_RESULT",1)

    @Test
    fun `generates tokens`() {
        every { virologyService.acceptTestResultGeneratingTokens(any()) } returnsMany listOf(r1, r2, r3)
        every { store.storeCsv(capture(csvSlot)) } just Runs
        every { store.storeZip(capture(zipSlot)) } just Runs

        processorService.generateAndStoreTokens(multiTokenEvent)

        verify(exactly = 3) { virologyService.acceptTestResultGeneratingTokens(tokenGenRequest) }
        verify(exactly = 1) { store.storeCsv(any()) }
        assertThat(csvSlot.captured.filename).isEqualTo("1970-01-01T00:00:00Z.csv")
        assertThat(csvSlot.captured.content).contains("pesddgrq")
        assertThat(csvSlot.captured.content).contains("gve9v72v")
        assertThat(csvSlot.captured.content).contains("fveeqkrn")

        verify(exactly = 1) { store.storeZip(any()) }
        assertThat(zipSlot.captured.filename).isEqualTo("1970-01-01T00:00:00Z.zip")
    }

    @Test
    fun `single token - no tokens stored when service throws exception everytime it is called (with retries)`() {
        every { virologyService.acceptTestResultGeneratingTokens(any()) } throws Exception()

        processorService.generateAndStoreTokens(singleTokenEvent)

        verify(exactly = 3) { virologyService.acceptTestResultGeneratingTokens(tokenGenRequest) }
        verify(exactly = 0) { store.storeCsv(any()) }
        verify(exactly = 0) { store.storeZip(any()) }
    }

    @Test
    fun `multi token - no tokens stored when service throws exception everytime it is called (with retries)`() {
        every { virologyService.acceptTestResultGeneratingTokens(any()) } throws Exception()

        processorService.generateAndStoreTokens(multiTokenEvent)

        verify(exactly = 9) { virologyService.acceptTestResultGeneratingTokens(tokenGenRequest) }
        verify(exactly = 0) { store.storeCsv(any()) }
        verify(exactly = 0) { store.storeZip(any()) }
    }

    @Test
    fun `single token - retries when exception is thrown`() {
        every {
            virologyService.acceptTestResultGeneratingTokens(any())
        } throws IllegalStateException("Error 1!") andThenThrows IllegalStateException("Error 2!") andThen r1

        every { store.storeCsv(capture(csvSlot)) } just Runs
        every { store.storeZip(capture(zipSlot)) } just Runs

        processorService.generateAndStoreTokens(singleTokenEvent)

        verify(exactly = 3) { virologyService.acceptTestResultGeneratingTokens(tokenGenRequest) }
        verify(exactly = 1) { store.storeCsv(any()) }
        assertThat(csvSlot.captured.filename).isEqualTo("1970-01-01T00:00:00Z.csv")
        assertThat(csvSlot.captured.content).contains("pesddgrq")

        verify(exactly = 1) { store.storeZip(any()) }
        assertThat(zipSlot.captured.filename).isEqualTo("1970-01-01T00:00:00Z.zip")
    }

    @Test
    fun `multi token - retries when exception is thrown`() {
        every {
            virologyService.acceptTestResultGeneratingTokens(any())
        } returns r1 andThenThrows IllegalStateException("something bad happened!") andThen r3

        every { store.storeCsv(capture(csvSlot)) } just Runs
        every { store.storeZip(capture(zipSlot)) } just Runs

        processorService.generateAndStoreTokens(multiTokenEvent)

        verify(exactly = 4) { virologyService.acceptTestResultGeneratingTokens(tokenGenRequest) }
        verify(exactly = 1) { store.storeCsv(any()) }
        assertThat(csvSlot.captured.filename).isEqualTo("1970-01-01T00:00:00Z.csv")
        assertThat(csvSlot.captured.content).contains("pesddgrq")
        assertThat(csvSlot.captured.content).contains("fveeqkrn")
        assertThat(csvSlot.captured.content).doesNotContain("gve9v72v")

        verify(exactly = 1) { store.storeZip(any()) }
        assertThat(zipSlot.captured.filename).isEqualTo("1970-01-01T00:00:00Z.zip")
    }
}
