package uk.nhs.nhsx.virology.lookup

import uk.nhs.nhsx.core.Clock
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.exceptions.TransactionException
import uk.nhs.nhsx.core.headers.MobileAppVersion
import uk.nhs.nhsx.virology.Country
import uk.nhs.nhsx.virology.TestResultMarkForDeletionFailure
import uk.nhs.nhsx.virology.VirologyPolicyConfig
import uk.nhs.nhsx.virology.VirologyPolicyConfig.VirologyCriteria
import uk.nhs.nhsx.virology.persistence.TestState
import uk.nhs.nhsx.virology.persistence.TestState.AvailableTestResult
import uk.nhs.nhsx.virology.persistence.TestState.PendingTestResult
import uk.nhs.nhsx.virology.persistence.VirologyDataTimeToLive
import uk.nhs.nhsx.virology.persistence.VirologyDataTimeToLiveCalculator
import uk.nhs.nhsx.virology.persistence.VirologyPersistenceService

class VirologyLookupService(
    private val persistence: VirologyPersistenceService,
    private val clock: Clock,
    private val policyConfig: VirologyPolicyConfig,
    private val events: Events
) {
    fun lookup(request: VirologyLookupRequestV1): VirologyLookupResult = persistence
        .getTestResult(request.testResultPollingToken)
        .map { lookupV1(it) }
        .orElseGet { VirologyLookupResult.NotFound() }

    private fun lookupV1(testState: TestState) = when (testState) {
        is PendingTestResult -> VirologyLookupResult.Pending()
        is AvailableTestResult -> {
            when {
                policyConfig.shouldBlockV1TestResultQueries(testState.testKit) -> VirologyLookupResult.Pending()
                else -> {
                    markTestDataForDeletion(
                        testState,
                        VirologyDataTimeToLiveCalculator.DEFAULT_TTL(clock)
                    )
                    VirologyLookupResult.Available(
                        VirologyLookupResponseV1(
                            testState.testEndDate,
                            testState.testResult,
                            testState.testKit
                        )
                    )
                }
            }
        }
    }

    fun lookup(
        request: VirologyLookupRequestV2,
        mobileAppVersion: MobileAppVersion
    ): VirologyLookupResult = persistence
        .getTestResult(request.testResultPollingToken)
        .map { lookupV2(it, request.country, mobileAppVersion) }
        .orElseGet { VirologyLookupResult.NotFound() }

    private fun lookupV2(
        testState: TestState,
        country: Country,
        mobileAppVersion: MobileAppVersion
    ) = when (testState) {
        is PendingTestResult -> VirologyLookupResult.Pending()
        is AvailableTestResult -> {
            val virologyCriteria = VirologyCriteria(country, testState.testKit, testState.testResult)
            when {
                policyConfig.shouldBlockV2TestResultQueries(
                    virologyCriteria,
                    mobileAppVersion
                ) -> VirologyLookupResult.Pending()
                else -> {
                    markTestDataForDeletion(
                        testState,
                        VirologyDataTimeToLiveCalculator.DEFAULT_TTL(clock)
                    )
                    VirologyLookupResult.AvailableV2(
                        VirologyLookupResponseV2(
                            testState.testEndDate,
                            testState.testResult,
                            testState.testKit,
                            policyConfig.isDiagnosisKeysSubmissionSupported(virologyCriteria),
                            policyConfig.isConfirmatoryTestRequired(virologyCriteria)
                        )
                    )
                }
            }
        }
    }

    private fun markTestDataForDeletion(testResult: AvailableTestResult, timeToLive: VirologyDataTimeToLive) {
        try {
            persistence.markForDeletion(testResult, timeToLive)
        } catch (e: TransactionException) {
            events(
                TestResultMarkForDeletionFailure(
                    testResult.testResultPollingToken,
                    e.message!!
                )
            )
        }
    }
}
