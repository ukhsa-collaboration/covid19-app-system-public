package uk.nhs.nhsx.virology.policy

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import uk.nhs.nhsx.core.headers.MobileAppVersion
import uk.nhs.nhsx.domain.Country.Companion.England
import uk.nhs.nhsx.domain.Country.Companion.Wales
import uk.nhs.nhsx.domain.TestJourney.CtaExchange
import uk.nhs.nhsx.domain.TestJourney.Lookup
import uk.nhs.nhsx.domain.TestKit
import uk.nhs.nhsx.domain.TestKit.LAB_RESULT
import uk.nhs.nhsx.domain.TestKit.RAPID_RESULT
import uk.nhs.nhsx.domain.TestKit.RAPID_SELF_REPORTED
import uk.nhs.nhsx.domain.TestResult.Negative
import uk.nhs.nhsx.domain.TestResult.Positive
import uk.nhs.nhsx.domain.TestResult.Void

class VirologyPolicyConfigTest {

    private val config = VirologyPolicyConfig()
    private val mobileAppVersion = MobileAppVersion.Version(1, 0)

    private val doesNotRequireConfirmatoryTest = setOf(
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

    private val requiresConfirmatoryTest = setOf(
        VirologyCriteria(CtaExchange, England, RAPID_SELF_REPORTED, Positive)
    )

    @Test
    fun `confirmatory test is not required`() {
        doesNotRequireConfirmatoryTest.forEach {
            assertThat(config.isConfirmatoryTestRequired(it, mobileAppVersion), equalTo(false))
        }
    }

    @Test
    fun `confirmatory test is required`() {
        requiresConfirmatoryTest.forEach {
            assertThat(config.isConfirmatoryTestRequired(it, mobileAppVersion), equalTo(true))
        }
    }

    @Test
    fun `confirmatory test is required for RAPID_RESULT test kit on specific mobile versions`() {
        val mobileAppVersion = MobileAppVersion.Version(4, 11)

        val testCases = listOf(
            VirologyCriteria(CtaExchange, England, RAPID_RESULT, Positive),
            VirologyCriteria(CtaExchange, Wales, RAPID_RESULT, Positive),
        )

        testCases.forEach {
            assertThat(config.isConfirmatoryTestRequired(it, mobileAppVersion), equalTo(true))
        }
    }

    @Test
    fun `confirmatory test is not required for RAPID_RESULT test kit on specific mobile versions`() {
        val mobileAppVersion = MobileAppVersion.Version(4, 10)

        val testCases = listOf(
            VirologyCriteria(CtaExchange, England, RAPID_RESULT, Positive),
            VirologyCriteria(CtaExchange, Wales, RAPID_RESULT, Positive),
        )

        testCases.forEach {
            assertThat(config.isConfirmatoryTestRequired(it, mobileAppVersion), equalTo(false))
        }
    }

    @Test
    fun `supports diagnosis key submission`() {
        val testCases = listOf(
            VirologyCriteria(Lookup, England, LAB_RESULT, Positive),
            VirologyCriteria(Lookup, Wales, LAB_RESULT, Positive),
            VirologyCriteria(Lookup, Wales, RAPID_RESULT, Positive),
            VirologyCriteria(Lookup, Wales, RAPID_SELF_REPORTED, Positive),

            VirologyCriteria(CtaExchange, England, LAB_RESULT, Positive),
            VirologyCriteria(CtaExchange, Wales, LAB_RESULT, Positive),
            VirologyCriteria(CtaExchange, Wales, RAPID_RESULT, Positive),
            VirologyCriteria(CtaExchange, Wales, RAPID_SELF_REPORTED, Positive)
        )

        testCases.forEach {
            assertThat(config.isDiagnosisKeysSubmissionSupported(it), equalTo(true))
        }
    }

    @Test
    fun `blocks diagnosis key submission for non positive results`() {
        val testCases = listOf(
            VirologyCriteria(Lookup, England, RAPID_RESULT, Negative),
            VirologyCriteria(Lookup, England, LAB_RESULT, Negative),
            VirologyCriteria(Lookup, England, RAPID_RESULT, Void),
            VirologyCriteria(Lookup, England, LAB_RESULT, Void),

            VirologyCriteria(CtaExchange, Wales, RAPID_RESULT, Negative),
            VirologyCriteria(CtaExchange, Wales, LAB_RESULT, Negative),
            VirologyCriteria(CtaExchange, Wales, RAPID_RESULT, Void),
            VirologyCriteria(CtaExchange, Wales, LAB_RESULT, Void)
        )

        testCases.forEach {
            assertThat(config.isDiagnosisKeysSubmissionSupported(it), equalTo(false))
        }
    }

    @ParameterizedTest
    @EnumSource(TestKit::class)
    fun `blocks v1 test result queries`(testKit: TestKit) {
        val config = VirologyPolicyConfig(
            requireConfirmatoryTest = emptyMap(),
            diagnosisKeySubmissionSupported = emptySet(),
            blockedV1TestKitQueries = setOf(testKit)
        )
        assertThat(config.shouldBlockV1TestResultQueries(testKit), equalTo(true))
    }

    @ParameterizedTest
    @EnumSource(TestKit::class)
    fun `blocks none v1 test result queries`(testKit: TestKit) {
        val config = VirologyPolicyConfig(
            requireConfirmatoryTest = emptyMap(),
            diagnosisKeySubmissionSupported = emptySet(),
            blockedV1TestKitQueries = emptySet()
        )
        assertThat(config.shouldBlockV1TestResultQueries(testKit), equalTo(false))
    }

    @Test
    fun `blocks v2 for old versions if criteria requires confirmatory test`() {
        val oldVersion = MobileAppVersion.Version(4, 3)
        requiresConfirmatoryTest.forEach {
            assertThat(config.shouldBlockV2TestResultQueries(it, oldVersion), equalTo(true))
        }
    }

    @Test
    fun `does not block v2 for new app versions if criteria requires confirmatory test`() {
        val newVersion = MobileAppVersion.Version(4, 4)
        requiresConfirmatoryTest.forEach {
            assertThat(config.shouldBlockV2TestResultQueries(it, newVersion), equalTo(false))
        }
    }

    @Test
    fun `does not block v2 for old app versions if criteria does not require confirmatory test`() {
        val oldVersion = MobileAppVersion.Version(4, 3)
        doesNotRequireConfirmatoryTest.forEach {
            assertThat(config.shouldBlockV2TestResultQueries(it, oldVersion), equalTo(false))
        }
    }

    @Test
    fun `does not block v2 for new app versions and criteria does not require confirmatory test`() {
        val version = MobileAppVersion.Version(4, 4)
        doesNotRequireConfirmatoryTest.forEach {
            assertThat(config.shouldBlockV2TestResultQueries(it, version), equalTo(false))
        }
    }

    @Test
    fun `does not block v2 for unknown app versions even if criteria requires confirmatory test`() {
        requiresConfirmatoryTest.forEach {
            assertThat(config.shouldBlockV2TestResultQueries(it, MobileAppVersion.Unknown), equalTo(false))
        }
    }

    @Test
    fun `current state for blocking v1 test result queries`() {
        assertThat(config.shouldBlockV1TestResultQueries(LAB_RESULT), equalTo(false))
        assertThat(config.shouldBlockV1TestResultQueries(RAPID_RESULT), equalTo(false))
        assertThat(config.shouldBlockV1TestResultQueries(RAPID_SELF_REPORTED), equalTo(true))
    }

    @Test
    fun `confirmatory day limit on cta exchange for England for new app versions`() {
        val mobileAppVersion = MobileAppVersion.Version(4, 11)
        val virologyCriteria = VirologyCriteria(CtaExchange, England, RAPID_RESULT, Positive)
        assertThat(config.confirmatoryDayLimit(virologyCriteria, mobileAppVersion), equalTo(2))
    }

    @Test
    fun `confirmatory day limit on cta exchange for England for old app versions`() {
        val mobileAppVersion = MobileAppVersion.Version(4, 10)
        val virologyCriteria = VirologyCriteria(CtaExchange, England, RAPID_RESULT, Positive)
        assertThat(config.confirmatoryDayLimit(virologyCriteria, mobileAppVersion), equalTo(null))
    }

    @Test
    fun `confirmatory day limit on lookup for England for new app versions`() {
        val mobileAppVersion = MobileAppVersion.Version(4, 11)
        val virologyCriteria = VirologyCriteria(Lookup, England, RAPID_RESULT, Positive)
        assertThat(config.confirmatoryDayLimit(virologyCriteria, mobileAppVersion), equalTo(null))
    }

    @Test
    fun `confirmatory day limit on lookup for England for old app versions`() {
        val mobileAppVersion = MobileAppVersion.Version(4, 10)
        val virologyCriteria = VirologyCriteria(Lookup, England, RAPID_RESULT, Positive)
        assertThat(config.confirmatoryDayLimit(virologyCriteria, mobileAppVersion), equalTo(null))
    }

    @Test
    fun `confirmatory day limit on cta exchange for Wales for new app versions`() {
        val mobileAppVersion = MobileAppVersion.Version(4, 11)
        val virologyCriteria = VirologyCriteria(CtaExchange, Wales, RAPID_RESULT, Positive)
        assertThat(config.confirmatoryDayLimit(virologyCriteria, mobileAppVersion), equalTo(1))
    }

    @Test
    fun `confirmatory day limit on cta exchange for Wales for old app versions`() {
        val mobileAppVersion = MobileAppVersion.Version(4, 10)
        val virologyCriteria = VirologyCriteria(CtaExchange, Wales, RAPID_RESULT, Positive)
        assertThat(config.confirmatoryDayLimit(virologyCriteria, mobileAppVersion), equalTo(null))
    }

    @Test
    fun `confirmatory day limit on lookup for Wales for new app versions`() {
        val mobileAppVersion = MobileAppVersion.Version(4, 11)
        val virologyCriteria = VirologyCriteria(Lookup, Wales, RAPID_RESULT, Positive)
        assertThat(config.confirmatoryDayLimit(virologyCriteria, mobileAppVersion), equalTo(null))
    }

    @Test
    fun `confirmatory day limit on lookup for Wales for old app versions`() {
        val mobileAppVersion = MobileAppVersion.Version(4, 10)
        val virologyCriteria = VirologyCriteria(Lookup, Wales, RAPID_RESULT, Positive)
        assertThat(config.confirmatoryDayLimit(virologyCriteria, mobileAppVersion), equalTo(null))
    }
}
