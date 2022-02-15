package uk.nhs.nhsx.virology.lookup

import uk.nhs.nhsx.core.Clock
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.exceptions.TransactionException
import uk.nhs.nhsx.core.headers.MobileAppVersion
import uk.nhs.nhsx.domain.Country
import uk.nhs.nhsx.domain.TestJourney.Lookup
import uk.nhs.nhsx.virology.TestResultMarkForDeletionFailure
import uk.nhs.nhsx.virology.lookup.VirologyLookupResult.AvailableV1
import uk.nhs.nhsx.virology.lookup.VirologyLookupResult.AvailableV2
import uk.nhs.nhsx.virology.lookup.VirologyLookupResult.NotFound
import uk.nhs.nhsx.virology.lookup.VirologyLookupResult.Pending
import uk.nhs.nhsx.virology.persistence.TestState
import uk.nhs.nhsx.virology.persistence.TestState.AvailableTestResult
import uk.nhs.nhsx.virology.persistence.TestState.PendingTestResult
import uk.nhs.nhsx.virology.persistence.VirologyDataTimeToLive
import uk.nhs.nhsx.virology.persistence.VirologyDataTimeToLiveCalculator.Companion.DEFAULT_TTL
import uk.nhs.nhsx.virology.persistence.VirologyPersistenceService
import uk.nhs.nhsx.virology.policy.VirologyCriteria
import uk.nhs.nhsx.virology.policy.VirologyPolicyConfig

class VirologyLookupService(
    private val persistence: VirologyPersistenceService,
    private val clock: Clock,
    private val policyConfig: VirologyPolicyConfig,
    private val events: Events
) {
    fun lookup(request: VirologyLookupRequestV1) = persistence
        .getTestResult(request.testResultPollingToken)
        ?.let(::lookupV1)
        ?: NotFound

    private fun lookupV1(testState: TestState) = when (testState) {
        is PendingTestResult -> Pending
        is AvailableTestResult -> {
            if (policyConfig.shouldBlockV1TestResultQueries(testState.testKit)) Pending else {
                markTestDataForDeletion(testState, DEFAULT_TTL(clock))
                with(testState) { AvailableV1(VirologyLookupResponseV1(testEndDate, testResult, testKit)) }
            }
        }
    }

    fun lookup(
        request: VirologyLookupRequestV2,
        mobileAppVersion: MobileAppVersion
    ) = persistence
        .getTestResult(request.testResultPollingToken)
        ?.let { lookupV2(it, request.country, mobileAppVersion) }
        ?: NotFound

    private fun lookupV2(
        testState: TestState,
        country: Country,
        mobileAppVersion: MobileAppVersion
    ) = when (testState) {
        is PendingTestResult -> Pending
        is AvailableTestResult -> {
            val virologyCriteria = VirologyCriteria(Lookup, country, testState.testKit, testState.testResult)
            when {
                policyConfig.shouldBlockV2TestResultQueries(
                    virologyCriteria,
                    mobileAppVersion
                ) -> Pending
                else -> {
                    markTestDataForDeletion(testState, DEFAULT_TTL(clock))
                    val response = with(testState) {
                        VirologyLookupResponseV2(
                            testEndDate = testEndDate,
                            testResult = testResult,
                            testKit = testKit,
                            diagnosisKeySubmissionSupported = policyConfig.isDiagnosisKeysSubmissionSupported(virologyCriteria),
                            requiresConfirmatoryTest = policyConfig.isConfirmatoryTestRequired(virologyCriteria, mobileAppVersion),
                            confirmatoryDayLimit = policyConfig.confirmatoryDayLimit(virologyCriteria, mobileAppVersion),
                            shouldOfferFollowUpTest = policyConfig.shouldOfferFollowUpTest(virologyCriteria, mobileAppVersion)
                        )
                    }
                    AvailableV2(response)
                }
            }
        }
    }

    private fun markTestDataForDeletion(testResult: AvailableTestResult, timeToLive: VirologyDataTimeToLive) {
        try {
            persistence.markForDeletion(testResult, timeToLive)
        } catch (e: TransactionException) {
            events(TestResultMarkForDeletionFailure(testResult.testResultPollingToken, e.message))
        }
    }
}
