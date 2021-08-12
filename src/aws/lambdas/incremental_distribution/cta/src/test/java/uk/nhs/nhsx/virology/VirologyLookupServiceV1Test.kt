@file:Suppress("TestFunctionName")

package uk.nhs.nhsx.virology

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import io.mockk.verifySequence
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.domain.TestResultPollingToken
import uk.nhs.nhsx.testhelper.data.TestData
import uk.nhs.nhsx.virology.lookup.VirologyLookupRequestV1
import uk.nhs.nhsx.virology.lookup.VirologyLookupResponseV1
import uk.nhs.nhsx.virology.lookup.VirologyLookupResult.Available
import uk.nhs.nhsx.virology.lookup.VirologyLookupResult.NotFound
import uk.nhs.nhsx.virology.lookup.VirologyLookupResult.Pending
import uk.nhs.nhsx.virology.lookup.VirologyLookupService
import uk.nhs.nhsx.virology.persistence.VirologyDataTimeToLive
import uk.nhs.nhsx.virology.persistence.VirologyPersistenceService
import uk.nhs.nhsx.virology.policy.VirologyPolicyConfig
import java.time.Duration
import java.time.Instant
import java.util.*

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
        every { persistenceService.markForDeletion(any(), any()) } just runs
        every { virologyPolicyConfig.shouldBlockV1TestResultQueries(any()) } returns false

        val service = VirologyLookupService()
        val pollingToken = TestResultPollingToken.of("98cff3dd-882c-417b-a00a-350a205378c7")
        val request = VirologyLookupRequestV1(pollingToken)
        val lookupResult = service.lookup(request)

        verifySequence {
            persistenceService.getTestResult(pollingToken)
            persistenceService.markForDeletion(
                testResult = testResult,
                virologyTimeToLive = VirologyDataTimeToLive(
                    testDataExpireAt = now.plus(Duration.ofHours(4)),
                    submissionDataExpireAt = now.plus(Duration.ofDays(4))
                )
            )
        }

        expectThat(lookupResult).isA<Available>().and {
            with(Available::response) {
                get(VirologyLookupResponseV1::testEndDate).isEqualTo(testResult.testEndDate)
                get(VirologyLookupResponseV1::testResult).isEqualTo(testResult.testResult)
            }
        }
    }

    @Test
    fun `virology lookup with result pending`() {
        every { persistenceService.getTestResult(any()) } returns Optional.of(TestData.pendingTestResult)
        every { virologyPolicyConfig.shouldBlockV1TestResultQueries(any()) } returns false

        val service = VirologyLookupService()
        val pollingToken = TestResultPollingToken.of("98cff3dd-882c-417b-a00a-350a205378c7")
        val request = VirologyLookupRequestV1(pollingToken)

        val lookupResult = service.lookup(request)

        expectThat(lookupResult).isA<Pending>()
    }

    @Test
    fun `virology lookup with no match`() {
        every { persistenceService.getTestResult(any()) } returns Optional.empty()

        val service = VirologyLookupService()
        val pollingToken = TestResultPollingToken.of("98cff3dd-882c-417b-a00a-350a205378c7")
        val request = VirologyLookupRequestV1(pollingToken)

        val lookupResult = service.lookup(request)

        expectThat(lookupResult).isA<NotFound>()
    }

    @Test
    fun `virology lookup for rapid result returns pending result`() {
        every { persistenceService.getTestResult(any()) } returns Optional.of(TestData.positiveRapidResult)
        every { virologyPolicyConfig.shouldBlockV1TestResultQueries(any()) } returns true

        val service = VirologyLookupService()

        val pollingToken = TestResultPollingToken.of("98cff3dd-882c-417b-a00a-350a205378c7")
        val request = VirologyLookupRequestV1(pollingToken)
        val lookupResult = service.lookup(request)

        verifySequence {
            persistenceService.getTestResult(pollingToken)
        }

        expectThat(lookupResult).isA<Pending>()
    }

    @Test
    fun `virology lookup for v1 with lfd test type is found`() {
        val testResult = TestData.positiveRapidResult
        every { persistenceService.getTestResult(any()) } returns Optional.of(testResult)
        every { persistenceService.markForDeletion(any(), any()) } just runs
        every { virologyPolicyConfig.shouldBlockV1TestResultQueries(any()) } returns false

        val service = VirologyLookupService()
        val pollingToken = TestResultPollingToken.of("98cff3dd-882c-417b-a00a-350a205378c7")
        val request = VirologyLookupRequestV1(pollingToken)
        val lookupResult = service.lookup(request)

        verify {
            persistenceService.getTestResult(pollingToken)
        }

        expectThat(lookupResult).isA<Available>()
    }

    private fun VirologyLookupService() = VirologyLookupService(
        persistence = persistenceService,
        clock = clock,
        policyConfig = virologyPolicyConfig,
        events = events
    )
}
