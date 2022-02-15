package uk.nhs.nhsx.virology.policy

import uk.nhs.nhsx.core.headers.MobileAppVersion
import uk.nhs.nhsx.core.headers.MobileAppVersion.Unknown
import uk.nhs.nhsx.core.headers.MobileAppVersion.Version
import uk.nhs.nhsx.domain.Country.Companion.England
import uk.nhs.nhsx.domain.Country.Companion.Wales
import uk.nhs.nhsx.domain.TestJourney.CtaExchange
import uk.nhs.nhsx.domain.TestJourney.Lookup
import uk.nhs.nhsx.domain.TestKit
import uk.nhs.nhsx.domain.TestKit.LAB_RESULT
import uk.nhs.nhsx.domain.TestKit.RAPID_RESULT
import uk.nhs.nhsx.domain.TestKit.RAPID_SELF_REPORTED
import uk.nhs.nhsx.domain.TestResult.Positive

class VirologyPolicyConfig(

    private val shouldOfferFollowUpTest: Map<VirologyCriteria, MobileVersionChecker> = mapOf(),

    private val requireConfirmatoryTest: Map<VirologyCriteria, MobileVersionChecker> = mapOf(
        VirologyCriteria(CtaExchange, England, RAPID_SELF_REPORTED, Positive) to FromMinimumInclusive(Version(4, 26)),
        VirologyCriteria(CtaExchange, England, RAPID_RESULT, Positive) to FromMinimumInclusive(Version(4, 26)),
    ),

    private val diagnosisKeySubmissionSupported: Set<VirologyCriteria> = setOf(
        VirologyCriteria(Lookup, England, LAB_RESULT, Positive),
        VirologyCriteria(Lookup, England, RAPID_RESULT, Positive),
        VirologyCriteria(Lookup, England, RAPID_SELF_REPORTED, Positive),
        VirologyCriteria(Lookup, Wales, LAB_RESULT, Positive),
        VirologyCriteria(Lookup, Wales, RAPID_RESULT, Positive),
        VirologyCriteria(Lookup, Wales, RAPID_SELF_REPORTED, Positive),

        VirologyCriteria(CtaExchange, England, LAB_RESULT, Positive),
        VirologyCriteria(CtaExchange, England, RAPID_RESULT, Positive),
        VirologyCriteria(CtaExchange, England, RAPID_SELF_REPORTED, Positive),
        VirologyCriteria(CtaExchange, Wales, LAB_RESULT, Positive),
        VirologyCriteria(CtaExchange, Wales, RAPID_RESULT, Positive),
        VirologyCriteria(CtaExchange, Wales, RAPID_SELF_REPORTED, Positive)
    ),

    private val confirmatoryDayLimit: Map<VirologyCriteria, ConfirmatoryDayLimit> = mapOf(),

    private val blockedV1TestKitQueries: Set<TestKit> = setOf(RAPID_SELF_REPORTED)
) {

    data class ConfirmatoryDayLimit(
        val mobileVersionChecker: MobileVersionChecker,
        val confirmatoryDayLimit: Int
    )

    fun isConfirmatoryTestRequired(
        virologyCriteria: VirologyCriteria,
        version: MobileAppVersion
    ) = requireConfirmatoryTest[virologyCriteria]?.invoke(version) ?: false

    fun isDiagnosisKeysSubmissionSupported(virologyCriteria: VirologyCriteria) =
        diagnosisKeySubmissionSupported.contains(virologyCriteria)

    fun confirmatoryDayLimit(
        virologyCriteria: VirologyCriteria,
        version: MobileAppVersion
    ) = confirmatoryDayLimit[virologyCriteria]
        ?.takeIf { it.mobileVersionChecker(version) }
        ?.let(ConfirmatoryDayLimit::confirmatoryDayLimit)

    fun shouldBlockV1TestResultQueries(testKit: TestKit) = blockedV1TestKitQueries.contains(testKit)

    fun shouldBlockV2TestResultQueries(
        virologyCriteria: VirologyCriteria,
        version: MobileAppVersion
    ) = when (version) {
        is Version -> version <= Version(4, 3) && isConfirmatoryTestRequired(virologyCriteria, version)
        Unknown -> false
    }

    fun shouldOfferFollowUpTest(
        virologyCriteria: VirologyCriteria,
        version: MobileAppVersion
    ) = shouldOfferFollowUpTest[virologyCriteria]?.invoke(version) ?: false
}
