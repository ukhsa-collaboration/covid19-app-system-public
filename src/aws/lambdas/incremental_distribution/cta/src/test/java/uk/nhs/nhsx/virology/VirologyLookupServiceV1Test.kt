package uk.nhs.nhsx.virology

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifySequence
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.domain.TestResultPollingToken
import uk.nhs.nhsx.testhelper.data.TestData
import uk.nhs.nhsx.virology.lookup.VirologyLookupRequestV1
import uk.nhs.nhsx.virology.lookup.VirologyLookupResult
import uk.nhs.nhsx.virology.lookup.VirologyLookupService
import uk.nhs.nhsx.virology.persistence.VirologyDataTimeToLive
import uk.nhs.nhsx.virology.persistence.VirologyPersistenceService
import uk.nhs.nhsx.virology.policy.VirologyPolicyConfig
import java.time.Duration
import java.time.Instant
import java.util.Optional

class VirologyLookupServiceV1Test {

    private val events = RecordingEvents()
    private val now = Instant.EPOCH
    private val clock = { now }
    private val persistenceService = mockk<VirologyPersistenceService>()
    private val virologyPolicyConfig = mockk<VirologyPolicyConfig>()

    @Test
    fun `virology lookup with result available`() {
        val testResult = TestData.positiveLabResult
        every { persistenceService.getTestResult(any()) } returns Optional.of(testResult)
        every { persistenceService.markForDeletion(any(), any()) } just Runs
        every { virologyPolicyConfig.shouldBlockV1TestResultQueries(any()) } returns false
        val service = virologyLookup()
        val pollingToken = TestResultPollingToken.of("98cff3dd-882c-417b-a00a-350a205378c7")
        val request = VirologyLookupRequestV1(pollingToken)
        val lookupResult = service.lookup(request)

        assertThat(lookupResult).isInstanceOf(VirologyLookupResult.Available::class.java)
        val resultAvailable = lookupResult as VirologyLookupResult.Available
        assertThat(resultAvailable.response.testEndDate).isEqualTo(testResult.testEndDate)
        assertThat(resultAvailable.response.testResult).isEqualTo(testResult.testResult)

        verifySequence {
            persistenceService.getTestResult(pollingToken)
            persistenceService.markForDeletion(
                testResult, VirologyDataTimeToLive(
                    now.plus(Duration.ofHours(4)),
                    now.plus(Duration.ofDays(4))
                )
            )
        }
    }

    @Test
    fun `virology lookup with result pending`() {
        every { persistenceService.getTestResult(any()) } returns Optional.of(TestData.pendingTestResult)
        every { virologyPolicyConfig.shouldBlockV1TestResultQueries(any()) } returns false

        val service = virologyLookup()
        val pollingToken = TestResultPollingToken.of("98cff3dd-882c-417b-a00a-350a205378c7")
        val request = VirologyLookupRequestV1(pollingToken)

        val lookupResult = service.lookup(request)
        assertThat(lookupResult).isInstanceOf(VirologyLookupResult.Pending::class.java)
    }

    @Test
    fun `virology lookup with no match`() {
        every { persistenceService.getTestResult(any()) } returns Optional.empty()

        val service = virologyLookup()
        val pollingToken = TestResultPollingToken.of("98cff3dd-882c-417b-a00a-350a205378c7")
        val request = VirologyLookupRequestV1(pollingToken)

        val lookupResult = service.lookup(request)
        assertThat(lookupResult).isInstanceOf(VirologyLookupResult.NotFound::class.java)
    }

    @Test
    fun `virology lookup for rapid result returns pending result`() {
        every { persistenceService.getTestResult(any()) } returns Optional.of(TestData.positiveRapidResult)
        every { virologyPolicyConfig.shouldBlockV1TestResultQueries(any()) } returns true

        val service = virologyLookup()

        val pollingToken = TestResultPollingToken.of("98cff3dd-882c-417b-a00a-350a205378c7")
        val request = VirologyLookupRequestV1(pollingToken)
        val lookupResult = service.lookup(request)

        assertThat(lookupResult).isInstanceOf(VirologyLookupResult.Pending::class.java)

        verifySequence {
            persistenceService.getTestResult(pollingToken)
        }
    }

    @Test
    fun `virology lookup for v1 with lfd test type is found`() {
        val testResult = TestData.positiveRapidResult
        every { persistenceService.getTestResult(any()) } returns Optional.of(testResult)
        every { persistenceService.markForDeletion(any(), any()) } just Runs
        every { virologyPolicyConfig.shouldBlockV1TestResultQueries(any()) } returns false
        val service = virologyLookup()
        val pollingToken = TestResultPollingToken.of("98cff3dd-882c-417b-a00a-350a205378c7")
        val request = VirologyLookupRequestV1(pollingToken)
        val lookupResult = service.lookup(request)

        assertThat(lookupResult).isInstanceOf(VirologyLookupResult.Available::class.java)

        verify {
            persistenceService.getTestResult(pollingToken)
        }
    }

    private fun virologyLookup() = VirologyLookupService(persistenceService, clock, virologyPolicyConfig, events)
}
