package uk.nhs.nhsx.testresultsupload

import io.mockk.*
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import uk.nhs.nhsx.core.exceptions.ApiResponseException
import java.time.Duration
import java.time.Instant
import java.util.function.Supplier

class NpexTestResultUploadServiceTest {

    private val persistenceService = mockk<NpexTestResultPersistenceService>()
    private val now = Instant.ofEpochSecond(0)
    private val clock = Supplier { now }
    private val uploadService = NpexTestResultUploadService(persistenceService, clock)

    @Test
    fun `persists result correctly for positive result`() {
        every { persistenceService.persistPositiveTestResult(any(), any()) } just Runs

        val testResult = npexTestResultWith("POSITIVE")

        uploadService.accept(testResult)

        verify(exactly = 1) {
            persistenceService.persistPositiveTestResult(
                NpexTestResult.Positive.from(testResult),
                Duration.ofDays(4 * 7.toLong()).seconds
            )
        }
    }

    private fun npexTestResultWith(testResult: String) =
        NpexTestResult("405002323", "2020-04-23T00:00:00Z", testResult)

    @Test
    fun `persists result correctly for negative result`() {
        every { persistenceService.persistNonPositiveTestResult(any()) } just Runs

        val testResult = npexTestResultWith("NEGATIVE")

        uploadService.accept(testResult)

        verify(exactly = 1) {
            persistenceService.persistNonPositiveTestResult(NpexTestResult.NonPositive.from(testResult))
        }
    }

    @Test
    fun `persists result correctly for void result`() {
        every { persistenceService.persistNonPositiveTestResult(any()) } just Runs

        val testResult = npexTestResultWith("VOID")

        uploadService.accept(testResult)

        verify(exactly = 1) {
            persistenceService.persistNonPositiveTestResult(NpexTestResult.NonPositive.from(testResult))
        }
    }

    @Test
    fun `throws exception with invalid test result`() {
        val testResult = npexTestResultWith("ORANGE")

        assertThatThrownBy { uploadService.accept(testResult) }
            .isInstanceOf(ApiResponseException::class.java)
            .hasMessage("validation error: Invalid test result value")
    }
}