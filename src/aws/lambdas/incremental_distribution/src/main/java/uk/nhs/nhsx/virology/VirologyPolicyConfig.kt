package uk.nhs.nhsx.virology

import uk.nhs.nhsx.core.headers.MobileAppVersion
import uk.nhs.nhsx.virology.Country.Companion.England
import uk.nhs.nhsx.virology.TestKit.LAB_RESULT
import uk.nhs.nhsx.virology.TestKit.RAPID_RESULT
import uk.nhs.nhsx.virology.TestKit.RAPID_SELF_REPORTED
import uk.nhs.nhsx.virology.result.TestResult
import uk.nhs.nhsx.virology.result.TestResult.Positive

class VirologyPolicyConfig(
    private val requireConfirmatoryTest: Set<VirologyCriteria> =
        setOf(
            VirologyCriteria(England, RAPID_SELF_REPORTED, Positive),
            VirologyCriteria(Country.of("Wales"), RAPID_SELF_REPORTED, Positive)
        ),
    private val supportedCountryTestKitPairs: Set<VirologyCriteria> =
        setOf(
            VirologyCriteria(England, LAB_RESULT, Positive),
            VirologyCriteria(England, RAPID_RESULT, Positive),
            VirologyCriteria(Country.of("Wales"), LAB_RESULT, Positive),
            VirologyCriteria(Country.of("Wales"), RAPID_RESULT, Positive)
        ),
    private val blockedV1TestKitQueries: Set<TestKit> =
        setOf(
            RAPID_SELF_REPORTED
        )
) {

    fun isConfirmatoryTestRequired(virologyCriteria: VirologyCriteria): Boolean =
        requireConfirmatoryTest.contains(virologyCriteria)

    fun isDiagnosisKeysSubmissionSupported(virologyCriteria: VirologyCriteria): Boolean =
        supportedCountryTestKitPairs.contains(virologyCriteria)

    fun shouldBlockV1TestResultQueries(testKit: TestKit): Boolean =
        blockedV1TestKitQueries.contains(testKit)

    fun shouldBlockV2TestResultQueries(virologyCriteria: VirologyCriteria, version: MobileAppVersion): Boolean =
        when (version) {
            is MobileAppVersion.Version ->
                version <= MobileAppVersion.Version(4, 3) && isConfirmatoryTestRequired(virologyCriteria)
            MobileAppVersion.Unknown -> false
        }

    data class VirologyCriteria(
        private val country: Country,
        private val testKit: TestKit,
        private val testResult: TestResult
    )
}
