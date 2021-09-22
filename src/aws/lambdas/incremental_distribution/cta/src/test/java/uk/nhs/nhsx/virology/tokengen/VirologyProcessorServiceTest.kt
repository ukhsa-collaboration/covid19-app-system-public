@file:Suppress("TestFunctionName")

package uk.nhs.nhsx.virology.tokengen

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.assertions.all
import strikt.assertions.contains
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.map
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.domain.CtaToken
import uk.nhs.nhsx.domain.TestEndDate
import uk.nhs.nhsx.domain.TestKit.LAB_RESULT
import uk.nhs.nhsx.domain.TestResult.Positive
import uk.nhs.nhsx.testhelper.assertions.captured
import uk.nhs.nhsx.testhelper.assertions.withCaptured
import uk.nhs.nhsx.virology.VirologyService
import uk.nhs.nhsx.virology.result.VirologyTokenGenRequestV2
import uk.nhs.nhsx.virology.result.VirologyTokenGenResponse
import java.io.File
import java.net.URL
import java.nio.file.Path
import java.time.Instant
import java.time.ZonedDateTime
import java.util.*

class VirologyProcessorServiceTest {

    private val clock = { Instant.ofEpochSecond(0) }
    private val events = RecordingEvents()

    private val virologyService = mockk<VirologyService>()
    private val store = mockk<VirologyProcessorStore>()
    private val maxRetryCount = 3

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
    fun `generates tokens`(@TempDir tempDir: Path) {
        every { virologyService.acceptTestResultGeneratingTokens(any()) } returnsMany listOf(r1, r2, r3)
        every { store.storeCsv(capture(csvSlot)) } just runs
        every { store.storeZip(capture(zipSlot)) } just runs

        VirologyProcessorService(VirologyProcessorExports(tempDir))
            .generateAndStoreTokens(multiTokenEvent)

        verify(exactly = 3) { virologyService.acceptTestResultGeneratingTokens(tokenGenRequest) }
        verify(exactly = 1) { store.storeCsv(any()) }
        verify(exactly = 1) { store.storeZip(any()) }

        expectThat(csvSlot).withCaptured {
            get(CtaTokensCsv::filename).isEqualTo("1970-01-01T000000Z.csv")
            with(CtaTokensCsv::content) {
                contains("pesddgrq")
                contains("gve9v72v")
                contains("fveeqkrn")
            }
        }

        expectThat(zipSlot).captured.get(CtaTokensZip::filename).isEqualTo("1970-01-01T000000Z.zip")
    }

    @Test
    fun `single token - no tokens stored when service throws exception everytime it is called (with retries)`(@TempDir tempDir: Path) {
        every { virologyService.acceptTestResultGeneratingTokens(any()) } throws Exception()

        VirologyProcessorService(VirologyProcessorExports(tempDir))
            .generateAndStoreTokens(singleTokenEvent)

        verify(exactly = 3) { virologyService.acceptTestResultGeneratingTokens(tokenGenRequest) }
        verify(exactly = 0) { store.storeCsv(any()) }
        verify(exactly = 0) { store.storeZip(any()) }
    }

    @Test
    fun `multi token - no tokens stored when service throws exception everytime it is called (with retries)`(@TempDir tempDir: Path) {
        every { virologyService.acceptTestResultGeneratingTokens(any()) } throws Exception()

        VirologyProcessorService(VirologyProcessorExports(tempDir))
            .generateAndStoreTokens(multiTokenEvent)

        verify(exactly = 9) { virologyService.acceptTestResultGeneratingTokens(tokenGenRequest) }
        verify(exactly = 0) { store.storeCsv(any()) }
        verify(exactly = 0) { store.storeZip(any()) }
    }

    @Test
    fun `single token - retries when exception is thrown`(@TempDir tempDir: Path) {
        every {
            virologyService.acceptTestResultGeneratingTokens(any())
        } throws IllegalStateException("Error 1!") andThenThrows IllegalStateException("Error 2!") andThen r1

        every { store.storeCsv(capture(csvSlot)) } just runs
        every { store.storeZip(capture(zipSlot)) } just runs

        VirologyProcessorService(VirologyProcessorExports(tempDir))
            .generateAndStoreTokens(singleTokenEvent)

        verify(exactly = 3) { virologyService.acceptTestResultGeneratingTokens(tokenGenRequest) }
        verify(exactly = 1) { store.storeCsv(any()) }
        verify(exactly = 1) { store.storeZip(any()) }

        expectThat(csvSlot).withCaptured {
            get(CtaTokensCsv::filename).isEqualTo("1970-01-01T000000Z.csv")
            with(CtaTokensCsv::content) {
                contains("pesddgrq")
            }
        }

        expectThat(zipSlot).captured.get(CtaTokensZip::filename).isEqualTo("1970-01-01T000000Z.zip")
    }

    @Test
    fun `multi token - retries when exception is thrown`(@TempDir tempDir: Path) {
        every {
            virologyService.acceptTestResultGeneratingTokens(any())
        } returns r1 andThenThrows IllegalStateException("something bad happened!") andThen r3

        every { store.storeCsv(capture(csvSlot)) } just runs
        every { store.storeZip(capture(zipSlot)) } just runs

        VirologyProcessorService(VirologyProcessorExports(tempDir))
            .generateAndStoreTokens(multiTokenEvent)

        verify(exactly = 4) { virologyService.acceptTestResultGeneratingTokens(tokenGenRequest) }
        verify(exactly = 1) { store.storeCsv(any()) }
        verify(exactly = 1) { store.storeZip(any()) }

        expectThat(csvSlot).withCaptured {
            get(CtaTokensCsv::filename).isEqualTo("1970-01-01T000000Z.csv")
            with(CtaTokensCsv::content) {
                contains("pesddgrq")
                contains("fveeqkrn")
                not().contains("gve9v72v")
            }
        }

        expectThat(zipSlot).captured.get(CtaTokensZip::filename).isEqualTo("1970-01-01T000000Z.zip")
    }

    @Test
    fun `generates multiple token requests and return signed url`() {
        val requestSignedUrlFilename = slot<String>()
        val requestSignedUrlValidityDate = slot<Date>()
        val ctaTokens = slot<List<CtaToken>>()
        val ctaTokenCSVs = slot<List<CtaTokensCsv>>()
        val virologyProcessorExportsMock = mockk<VirologyProcessorExports>()

        every { virologyService.acceptTestResultGeneratingTokens(any()) } returnsMany listOf(
            r1,
            r2,
            r3,
            r1,
            r2,
            r3,
            r1,
            r2,
            r3
        )
        every { virologyProcessorExportsMock.csvFrom(capture(ctaTokens), Positive, any()) } returns "Content"
        every {
            virologyProcessorExportsMock.zipFrom(
                any(),
                capture(ctaTokenCSVs),
                "password"
            )
        } returns File("1970-01-01T000000Z.zip")
        every {
            store.generateSignedURL(
                capture(requestSignedUrlFilename),
                capture(requestSignedUrlValidityDate)
            )
        } returns URL("https://example.com")
        every { store.storeCsv(capture(csvSlot)) } just runs
        every { store.storeZip(capture(zipSlot)) } just runs

        val requests = listOf(
            CtaTokenZipFileEntryRequest(
                testResult = Positive,
                testEndDate = TestEndDate.of(2020, 10, 6),
                testKit = LAB_RESULT,
                filename = "20201006",
                numberOfTokens = 3
            ),
            CtaTokenZipFileEntryRequest(
                testResult = Positive,
                testEndDate = TestEndDate.of(2020, 10, 7),
                testKit = LAB_RESULT,
                filename = "20201007",
                numberOfTokens = 3
            ),
            CtaTokenZipFileEntryRequest(
                testResult = Positive,
                testEndDate = TestEndDate.of(2020, 10, 8),
                testKit = LAB_RESULT,
                filename = "20201008",
                numberOfTokens = 3
            )
        )

        val result = VirologyProcessorService(virologyProcessorExportsMock)
            .generateStoreTokensAndReturnSignedUrl(
                requests = requests,
                zipFilePassword = "password",
                linkExpirationDate = ZonedDateTime.now()
            )

        expectThat(result).isEqualTo(
            CtaTokenZipFile(
                url = URL("https://example.com"),
                fileName = "1970-01-01T000000Z.zip",
                password = "password"
            )
        )

        expectThat(ctaTokens)
            .captured
            .map(CtaToken::value)
            .containsExactlyInAnyOrder("pesddgrq", "gve9v72v", "fveeqkrn")

        expectThat(ctaTokenCSVs).withCaptured {
            hasSize(3)
            all { get(CtaTokensCsv::content).isEqualTo("Content") }
            map(CtaTokensCsv::filename).containsExactlyInAnyOrder("20201006.csv", "20201007.csv", "20201008.csv")
        }
    }

    private fun VirologyProcessorService(exports: VirologyProcessorExports) = VirologyProcessorService(
        virologyService = virologyService,
        virologyProcessorStore = store,
        clock = clock,
        virologyProcessorExports = exports,
        maxRetryCount = maxRetryCount,
        events = events
    )

}
