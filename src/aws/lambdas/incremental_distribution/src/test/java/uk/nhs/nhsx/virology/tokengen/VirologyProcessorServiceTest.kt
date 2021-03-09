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
import uk.nhs.nhsx.virology.CtaToken
import uk.nhs.nhsx.testhelper.data.asInstant
import uk.nhs.nhsx.virology.TestKit.*
import uk.nhs.nhsx.virology.VirologyService
import uk.nhs.nhsx.virology.result.TestEndDate
import uk.nhs.nhsx.virology.result.TestResult.*
import uk.nhs.nhsx.virology.result.VirologyTokenGenRequestV2
import uk.nhs.nhsx.virology.result.VirologyTokenGenResponse
import java.io.File
import java.net.URL
import java.time.Instant
import java.time.ZonedDateTime
import java.util.*
import java.util.function.Supplier

class VirologyProcessorServiceTest {

    private val clock = Supplier { Instant.ofEpochSecond(0) }
    private val virologyService = mockk<VirologyService>()
    private val store = mockk<VirologyProcessorStore>()
    private val virologyProcessorExportsMock = mockk<VirologyProcessorExports>()

    private val maxRetryCount: Int = 3
    private val virologyProcessorExports = VirologyProcessorExports(System.getProperty("java.io.tmpdir") + File.separator)

    private val processorService = VirologyProcessorService(virologyService, store, clock, virologyProcessorExports, maxRetryCount, RecordingEvents())
    private val processorServiceWithExportMock = VirologyProcessorService(virologyService, store, clock, virologyProcessorExportsMock, maxRetryCount, RecordingEvents())


    private val r1 = VirologyTokenGenResponse(CtaToken.of("pesddgrq"))
    private val r2 = VirologyTokenGenResponse(CtaToken.of("gve9v72v"))
    private val r3 = VirologyTokenGenResponse(CtaToken.of("fveeqkrn"))

    private val csvSlot = slot<CtaTokensCsv>()
    private val zipSlot = slot<CtaTokensZip>()

    private val tokenGenRequest = VirologyTokenGenRequestV2(
        TestEndDate.of(2020, 10, 6),
        Positive,
        LAB_RESULT
    )

    private val multiTokenEvent = CtaProcessorRequest(
        Positive,
        TestEndDate.of(2020, 10, 6),
        LAB_RESULT,
        3
    )

    private val singleTokenEvent = CtaProcessorRequest(
        Positive,
        TestEndDate.of(2020, 10, 6),
        LAB_RESULT,
        1
    )

    @Test
    fun `generates tokens`() {
        every { virologyService.acceptTestResultGeneratingTokens(any()) } returnsMany listOf(r1, r2, r3)
        every { store.storeCsv(capture(csvSlot)) } just Runs
        every { store.storeZip(capture(zipSlot)) } just Runs

        processorService.generateAndStoreTokens(multiTokenEvent)

        verify(exactly = 3) { virologyService.acceptTestResultGeneratingTokens(tokenGenRequest) }
        verify(exactly = 1) { store.storeCsv(any()) }
        assertThat(csvSlot.captured.filename).isEqualTo("1970-01-01T000000Z.csv")
        assertThat(csvSlot.captured.content).contains("pesddgrq")
        assertThat(csvSlot.captured.content).contains("gve9v72v")
        assertThat(csvSlot.captured.content).contains("fveeqkrn")

        verify(exactly = 1) { store.storeZip(any()) }
        assertThat(zipSlot.captured.filename).isEqualTo("1970-01-01T000000Z.zip")
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
        assertThat(csvSlot.captured.filename).isEqualTo("1970-01-01T000000Z.csv")
        assertThat(csvSlot.captured.content).contains("pesddgrq")

        verify(exactly = 1) { store.storeZip(any()) }
        assertThat(zipSlot.captured.filename).isEqualTo("1970-01-01T000000Z.zip")
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
        assertThat(csvSlot.captured.filename).isEqualTo("1970-01-01T000000Z.csv")
        assertThat(csvSlot.captured.content).contains("pesddgrq")
        assertThat(csvSlot.captured.content).contains("fveeqkrn")
        assertThat(csvSlot.captured.content).doesNotContain("gve9v72v")

        verify(exactly = 1) { store.storeZip(any()) }
        assertThat(zipSlot.captured.filename).isEqualTo("1970-01-01T000000Z.zip")
    }
    @Test
    fun `generates multiple token requests and return signed url`() {
        val requestSignedUrlFilename = slot<String>()
        val requestSignedUrlValidityDate = slot<Date>()
        val ctaTokens = slot<List<CtaToken>>()
        val ctaTokenCsvs = slot<List<CtaTokensCsv>>()

        every { virologyService.acceptTestResultGeneratingTokens(any()) } returnsMany listOf(r1, r2, r3,r1, r2, r3,r1, r2, r3)
        every { virologyProcessorExportsMock.csvFrom(capture(ctaTokens),Positive,any()) } returns "Content"

        every { virologyProcessorExportsMock.zipFrom(any(),capture(ctaTokenCsvs) ,"password")} returns File("1970-01-01T000000Z.zip")

        every { store.generateSignedURL(capture(requestSignedUrlFilename),capture(requestSignedUrlValidityDate)) } answers {(_,_) -> URL("https://example.com")}
        every { store.storeCsv(capture(csvSlot)) } just Runs
        every { store.storeZip(capture(zipSlot)) } just Runs

        val expirationDate = ZonedDateTime.now()
        val requests = listOf(
            CtaTokenZipFileEntryRequest(Positive,TestEndDate.of(2020,10,6),LAB_RESULT,"20201006",3),
            CtaTokenZipFileEntryRequest(Positive,TestEndDate.of(2020,10,7),LAB_RESULT,"20201007",3),
            CtaTokenZipFileEntryRequest(Positive,TestEndDate.of(2020,10,8),LAB_RESULT,"20201008",3)
        )

        val result = processorServiceWithExportMock.generateStoreTokensAndReturnSignedUrl(requests,"password",expirationDate)
        assertThat(result).isEqualTo(CtaTokenZipFile(URL("https://example.com"),"1970-01-01T000000Z.zip","password"))

        assertThat(ctaTokens.captured).contains(CtaToken.of("pesddgrq"))
        assertThat(ctaTokens.captured).contains(CtaToken.of("fveeqkrn"))
        assertThat(ctaTokens.captured).contains(CtaToken.of("gve9v72v"))

        assertThat(ctaTokenCsvs.captured.size).isEqualTo(3)

        assertThat(ctaTokenCsvs.captured[0].content).isEqualTo("Content")
        assertThat(ctaTokenCsvs.captured[0].filename).isEqualTo("20201006.csv")

        assertThat(ctaTokenCsvs.captured[1].content).isEqualTo("Content")
        assertThat(ctaTokenCsvs.captured[1].filename).isEqualTo("20201007.csv")

        assertThat(ctaTokenCsvs.captured[2].content).isEqualTo("Content")
        assertThat(ctaTokenCsvs.captured[2].filename).isEqualTo("20201008.csv")
    }
}
