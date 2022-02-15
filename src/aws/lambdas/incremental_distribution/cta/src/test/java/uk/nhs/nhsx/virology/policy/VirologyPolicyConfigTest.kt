package uk.nhs.nhsx.virology.policy

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isNull
import strikt.assertions.isTrue
import uk.nhs.nhsx.core.headers.MobileAppVersion
import uk.nhs.nhsx.domain.Country
import uk.nhs.nhsx.domain.Country.Companion.England
import uk.nhs.nhsx.domain.Country.Companion.Wales
import uk.nhs.nhsx.domain.TestJourney
import uk.nhs.nhsx.domain.TestJourney.CtaExchange
import uk.nhs.nhsx.domain.TestJourney.Lookup
import uk.nhs.nhsx.domain.TestKit
import uk.nhs.nhsx.domain.TestKit.LAB_RESULT
import uk.nhs.nhsx.domain.TestKit.RAPID_RESULT
import uk.nhs.nhsx.domain.TestKit.RAPID_SELF_REPORTED
import uk.nhs.nhsx.domain.TestResult
import uk.nhs.nhsx.domain.TestResult.Negative
import uk.nhs.nhsx.domain.TestResult.Positive
import uk.nhs.nhsx.domain.TestResult.Void

class VirologyPolicyConfigTest {

    private val config = VirologyPolicyConfig()
    private val mobileAppVersion = MobileAppVersion.Version(1, 0)

    @ParameterizedTest
    @MethodSource("doesNotRequireConfirmatoryTest")
    fun `confirmatory test is not required`(criteria: VirologyCriteria) {
        expectThat(config.isConfirmatoryTestRequired(criteria, mobileAppVersion)).isFalse()
    }

    @ParameterizedTest
    @MethodSource("supportsDiagnosisKeySubmission")
    fun `supports diagnosis key submission`(criteria: VirologyCriteria) {
        expectThat(config.isDiagnosisKeysSubmissionSupported(criteria)).isTrue()
    }

    @ParameterizedTest
    @MethodSource("blocksDiagnosisKeySubmission")
    fun `blocks diagnosis key submission for non positive results`(criteria: VirologyCriteria) {
        expectThat(config.isDiagnosisKeysSubmissionSupported(criteria)).isFalse()
    }

    @ParameterizedTest
    @EnumSource(TestKit::class)
    fun `blocks v1 test result queries`(testKit: TestKit) {
        val config = VirologyPolicyConfig(
            requireConfirmatoryTest = emptyMap(),
            diagnosisKeySubmissionSupported = emptySet(),
            blockedV1TestKitQueries = setOf(testKit)
        )
        expectThat(config.shouldBlockV1TestResultQueries(testKit)).isTrue()
    }

    @ParameterizedTest
    @EnumSource(TestKit::class)
    fun `blocks none v1 test result queries`(testKit: TestKit) {
        val config = VirologyPolicyConfig(
            requireConfirmatoryTest = emptyMap(),
            diagnosisKeySubmissionSupported = emptySet(),
            blockedV1TestKitQueries = emptySet()
        )
        expectThat(config.shouldBlockV1TestResultQueries(testKit)).isFalse()
    }

    @ParameterizedTest
    @MethodSource("doesNotRequireConfirmatoryTest")
    fun `does not block v2 for old app versions if criteria does not require confirmatory test`(criteria: VirologyCriteria) {
        val oldVersion = MobileAppVersion.Version(4, 3)
        expectThat(config.shouldBlockV2TestResultQueries(criteria, oldVersion)).isFalse()
    }

    @ParameterizedTest
    @MethodSource("doesNotRequireConfirmatoryTest")
    fun `does not block v2 for new app versions and criteria does not require confirmatory test`(criteria: VirologyCriteria) {
        val version = MobileAppVersion.Version(4, 4)
        expectThat(config.shouldBlockV2TestResultQueries(criteria, version)).isFalse()
    }

    @ParameterizedTest
    @MethodSource("requiresConfirmatoryTestForCertainCases")
    fun `confirmatory test is required for RAPID_RESULT, RAPID_SELF_REPORTED test kit on specific mobile versions`(
        criteria: VirologyCriteria,
        version: MobileAppVersion
    ) {
        expectThat(config.isConfirmatoryTestRequired(criteria, version)).isTrue()
    }

    @ParameterizedTest
    @MethodSource("doesNotRequireConfirmatoryTestForCertainCases")
    fun `confirmatory test is not required for RAPID_RESULT, RAPID_SELF_REPORTED test kit on specific mobile versions`(
        criteria: VirologyCriteria,
        version: MobileAppVersion
    ) {
        expectThat(config.isConfirmatoryTestRequired(criteria, version)).isFalse()
    }

    @Test
    fun `current state for blocking v1 test result queries`() {
        expectThat(config.shouldBlockV1TestResultQueries(LAB_RESULT)).isFalse()
        expectThat(config.shouldBlockV1TestResultQueries(RAPID_RESULT)).isFalse()
        expectThat(config.shouldBlockV1TestResultQueries(RAPID_SELF_REPORTED)).isTrue()
    }

    @Test
    fun `confirmatory day limit on cta exchange for England for new app versions`() {
        val mobileAppVersion = MobileAppVersion.Version(4, 11)
        val virologyCriteria = VirologyCriteria(CtaExchange, England, RAPID_RESULT, Positive)
        expectThat(config.confirmatoryDayLimit(virologyCriteria, mobileAppVersion)).isNull()
    }

    @Test
    fun `confirmatory day limit on cta exchange for England for old app versions`() {
        val mobileAppVersion = MobileAppVersion.Version(4, 10)
        val virologyCriteria = VirologyCriteria(CtaExchange, England, RAPID_RESULT, Positive)
        expectThat(config.confirmatoryDayLimit(virologyCriteria, mobileAppVersion)).isNull()
    }

    @Test
    fun `confirmatory day limit on lookup for England for new app versions`() {
        val mobileAppVersion = MobileAppVersion.Version(4, 11)
        val virologyCriteria = VirologyCriteria(Lookup, England, RAPID_RESULT, Positive)
        expectThat(config.confirmatoryDayLimit(virologyCriteria, mobileAppVersion)).isNull()
    }

    @Test
    fun `confirmatory day limit on lookup for England for old app versions`() {
        val mobileAppVersion = MobileAppVersion.Version(4, 10)
        val virologyCriteria = VirologyCriteria(Lookup, England, RAPID_RESULT, Positive)
        expectThat(config.confirmatoryDayLimit(virologyCriteria, mobileAppVersion)).isNull()
    }

    @Test
    fun `confirmatory day limit on cta exchange for Wales RAPID_RESULT for new app versions`() {
        val mobileAppVersion = MobileAppVersion.Version(4, 11)
        val virologyCriteria = VirologyCriteria(CtaExchange, Wales, RAPID_RESULT, Positive)
        expectThat(config.confirmatoryDayLimit(virologyCriteria, mobileAppVersion)).isNull()
    }

    @Test
    fun `confirmatory day limit on cta exchange for Wales RAPID_RESULT for old app versions`() {
        val mobileAppVersion = MobileAppVersion.Version(4, 10)
        val virologyCriteria = VirologyCriteria(CtaExchange, Wales, RAPID_RESULT, Positive)
        expectThat(config.confirmatoryDayLimit(virologyCriteria, mobileAppVersion)).isNull()
    }

    @Test
    fun `confirmatory day limit on lookup for Wales RAPID_RESULT for new app versions`() {
        val mobileAppVersion = MobileAppVersion.Version(4, 11)
        val virologyCriteria = VirologyCriteria(Lookup, Wales, RAPID_RESULT, Positive)
        expectThat(config.confirmatoryDayLimit(virologyCriteria, mobileAppVersion)).isNull()
    }

    @Test
    fun `confirmatory day limit on lookup for Wales RAPID_RESULT for old app versions`() {
        val mobileAppVersion = MobileAppVersion.Version(4, 10)
        val virologyCriteria = VirologyCriteria(Lookup, Wales, RAPID_RESULT, Positive)
        expectThat(config.confirmatoryDayLimit(virologyCriteria, mobileAppVersion)).isNull()
    }

    @Test
    fun `confirmatory day limit on cta exchange for Wales RAPID_SELF_REPORTED for new app versions`() {
        val mobileAppVersion = MobileAppVersion.Version(4, 11)
        val virologyCriteria = VirologyCriteria(CtaExchange, Wales, RAPID_SELF_REPORTED, Positive)
        expectThat(config.confirmatoryDayLimit(virologyCriteria, mobileAppVersion)).isNull()
    }

    @Test
    fun `confirmatory day limit on cta exchange for Wales RAPID_SELF_REPORTED for old app versions`() {
        val mobileAppVersion = MobileAppVersion.Version(4, 10)
        val virologyCriteria = VirologyCriteria(CtaExchange, Wales, RAPID_SELF_REPORTED, Positive)
        expectThat(config.confirmatoryDayLimit(virologyCriteria, mobileAppVersion)).isNull()
    }

    @Test
    fun `confirmatory day limit on lookup for Wales RAPID_SELF_REPORTED for new app versions`() {
        val mobileAppVersion = MobileAppVersion.Version(4, 11)
        val virologyCriteria = VirologyCriteria(Lookup, Wales, RAPID_SELF_REPORTED, Positive)
        expectThat(config.confirmatoryDayLimit(virologyCriteria, mobileAppVersion)).isNull()
    }

    @Test
    fun `confirmatory day limit on lookup for Wales RAPID_SELF_REPORTED for old app versions`() {
        val mobileAppVersion = MobileAppVersion.Version(4, 10)
        val virologyCriteria = VirologyCriteria(Lookup, Wales, RAPID_SELF_REPORTED, Positive)
        expectThat(config.confirmatoryDayLimit(virologyCriteria, mobileAppVersion)).isNull()
    }

    @ParameterizedTest
    @MethodSource("shouldNotOfferFollowUpTest")
    fun `should offer follow up test`(
        virologyCriteria: VirologyCriteria
    ) {
        expectThat(config.shouldOfferFollowUpTest(virologyCriteria, MobileAppVersion.Version(4, 25))).isFalse()
    }

    companion object {
        @JvmStatic
        fun shouldNotOfferFollowUpTest() = cartesianProduct(
            TestJourney.values().toSet(),
            setOf(England, Wales),
            TestKit.values().toSet(),
            TestResult.values().toSet()
        ).map { (a, b, c, d) ->
            VirologyCriteria(
                testJourney = a as TestJourney,
                country = b as Country,
                testKit = c as TestKit,
                testResult = d as TestResult
            )
        }

        @JvmStatic
        fun doesNotRequireConfirmatoryTest() = setOf(
            VirologyCriteria(Lookup, England, LAB_RESULT, Positive),
            VirologyCriteria(Lookup, England, LAB_RESULT, Negative),
            VirologyCriteria(Lookup, England, LAB_RESULT, Void),
            VirologyCriteria(Lookup, England, RAPID_RESULT, Positive),
            VirologyCriteria(Lookup, England, RAPID_RESULT, Negative),
            VirologyCriteria(Lookup, England, RAPID_RESULT, Void),
            VirologyCriteria(Lookup, England, RAPID_SELF_REPORTED, Positive),
            VirologyCriteria(Lookup, England, RAPID_SELF_REPORTED, Negative),
            VirologyCriteria(Lookup, England, RAPID_SELF_REPORTED, Void),
            VirologyCriteria(Lookup, Wales, LAB_RESULT, Positive),
            VirologyCriteria(Lookup, Wales, LAB_RESULT, Negative),
            VirologyCriteria(Lookup, Wales, LAB_RESULT, Void),
            VirologyCriteria(Lookup, Wales, RAPID_RESULT, Positive),
            VirologyCriteria(Lookup, Wales, RAPID_RESULT, Negative),
            VirologyCriteria(Lookup, Wales, RAPID_RESULT, Void),
            VirologyCriteria(Lookup, Wales, RAPID_SELF_REPORTED, Positive),
            VirologyCriteria(Lookup, Wales, RAPID_SELF_REPORTED, Negative),
            VirologyCriteria(Lookup, Wales, RAPID_SELF_REPORTED, Void),

            VirologyCriteria(CtaExchange, England, LAB_RESULT, Positive),
            VirologyCriteria(CtaExchange, England, LAB_RESULT, Negative),
            VirologyCriteria(CtaExchange, England, LAB_RESULT, Void),
            VirologyCriteria(CtaExchange, England, RAPID_RESULT, Positive),
            VirologyCriteria(CtaExchange, England, RAPID_RESULT, Negative),
            VirologyCriteria(CtaExchange, England, RAPID_RESULT, Void),
            VirologyCriteria(CtaExchange, England, RAPID_SELF_REPORTED, Positive),
            VirologyCriteria(CtaExchange, England, RAPID_SELF_REPORTED, Negative),
            VirologyCriteria(CtaExchange, England, RAPID_SELF_REPORTED, Void),
            VirologyCriteria(CtaExchange, Wales, LAB_RESULT, Positive),
            VirologyCriteria(CtaExchange, Wales, LAB_RESULT, Negative),
            VirologyCriteria(CtaExchange, Wales, LAB_RESULT, Void),
            VirologyCriteria(CtaExchange, Wales, RAPID_RESULT, Positive),
            VirologyCriteria(CtaExchange, Wales, RAPID_RESULT, Negative),
            VirologyCriteria(CtaExchange, Wales, RAPID_RESULT, Void),
            VirologyCriteria(CtaExchange, Wales, RAPID_SELF_REPORTED, Positive),
            VirologyCriteria(CtaExchange, Wales, RAPID_SELF_REPORTED, Negative),
            VirologyCriteria(CtaExchange, Wales, RAPID_SELF_REPORTED, Void),
        )

        @JvmStatic
        fun requiresConfirmatoryTestForCertainCases() = setOf(
            Arguments.of(
                VirologyCriteria(CtaExchange, England, RAPID_RESULT, Positive),
                MobileAppVersion.Version(4, 26)
            ),
            Arguments.of(
                VirologyCriteria(CtaExchange, England, RAPID_SELF_REPORTED, Positive),
                MobileAppVersion.Version(4, 26)
            ),
        )

        @JvmStatic
        fun doesNotRequireConfirmatoryTestForCertainCases() = setOf(
            Arguments.of(
                VirologyCriteria(CtaExchange, England, RAPID_SELF_REPORTED, Positive),
                MobileAppVersion.Version(4, 25)
            ),
            Arguments.of(
                VirologyCriteria(CtaExchange, England, RAPID_RESULT, Positive),
                MobileAppVersion.Version(4, 25)
            ),
            Arguments.of(
                VirologyCriteria(CtaExchange, Wales, RAPID_RESULT, Positive),
                MobileAppVersion.Version(4, 26)
            ),
            Arguments.of(
                VirologyCriteria(CtaExchange, Wales, RAPID_SELF_REPORTED, Positive),
                MobileAppVersion.Version(4, 26)
            ),
        )

        @JvmStatic
        fun supportsDiagnosisKeySubmission() = setOf(
            VirologyCriteria(Lookup, England, LAB_RESULT, Positive),
            VirologyCriteria(Lookup, Wales, LAB_RESULT, Positive),
            VirologyCriteria(Lookup, Wales, RAPID_RESULT, Positive),
            VirologyCriteria(Lookup, Wales, RAPID_SELF_REPORTED, Positive),

            VirologyCriteria(CtaExchange, England, LAB_RESULT, Positive),
            VirologyCriteria(CtaExchange, Wales, LAB_RESULT, Positive),
            VirologyCriteria(CtaExchange, Wales, RAPID_RESULT, Positive),
            VirologyCriteria(CtaExchange, Wales, RAPID_SELF_REPORTED, Positive)
        )

        @JvmStatic
        fun blocksDiagnosisKeySubmission() = setOf(
            VirologyCriteria(Lookup, England, RAPID_RESULT, Negative),
            VirologyCriteria(Lookup, England, LAB_RESULT, Negative),
            VirologyCriteria(Lookup, England, RAPID_RESULT, Void),
            VirologyCriteria(Lookup, England, LAB_RESULT, Void),

            VirologyCriteria(CtaExchange, Wales, RAPID_RESULT, Negative),
            VirologyCriteria(CtaExchange, Wales, LAB_RESULT, Negative),
            VirologyCriteria(CtaExchange, Wales, RAPID_RESULT, Void),
            VirologyCriteria(CtaExchange, Wales, LAB_RESULT, Void)
        )

        private fun cartesianProduct(
            a: Set<*>,
            b: Set<*>,
            vararg sets: Set<*>
        ): Set<List<*>> = (setOf(a, b).plus(sets))
            .fold(listOf(listOf<Any?>())) { acc, set -> acc.flatMap { list -> set.map { element -> list + element } } }
            .toSet()
    }
}
