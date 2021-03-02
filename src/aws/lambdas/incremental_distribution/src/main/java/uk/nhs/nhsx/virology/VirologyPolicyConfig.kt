package uk.nhs.nhsx.virology

import uk.nhs.nhsx.core.headers.MobileAppVersion
import uk.nhs.nhsx.virology.TestKit.LAB_RESULT
import uk.nhs.nhsx.virology.TestKit.RAPID_RESULT
import uk.nhs.nhsx.virology.TestKit.RAPID_SELF_REPORTED
import uk.nhs.nhsx.virology.result.VirologyResultRequest.NPEX_POSITIVE

class VirologyPolicyConfig(
    private val requireConfirmatoryTest: Set<VirologyCriteria> =
        setOf(
            VirologyCriteria.of(Country.of("England"), RAPID_SELF_REPORTED, NPEX_POSITIVE),
            VirologyCriteria.of(Country.of("Wales"), RAPID_SELF_REPORTED, NPEX_POSITIVE)
        ),
    private val supportedCountryTestKitPairs: Set<VirologyCriteria> =
        setOf(
            VirologyCriteria.of(Country.of("England"), LAB_RESULT, NPEX_POSITIVE),
            VirologyCriteria.of(Country.of("England"), RAPID_RESULT, NPEX_POSITIVE),
            VirologyCriteria.of(Country.of("Wales"), LAB_RESULT, NPEX_POSITIVE),
            VirologyCriteria.of(Country.of("Wales"), RAPID_RESULT, NPEX_POSITIVE)
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

    data class VirologyCriteria(private val country: Country,
                                private val testKit: TestKit,
                                private val testResult: String) {
        companion object {
            @JvmStatic
            fun of(country: Country, testKit: TestKit, testResult: String): VirologyCriteria {
                return VirologyCriteria(country, testKit, testResult)
            }
        }
    }
}
