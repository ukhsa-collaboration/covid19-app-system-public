package uk.nhs.nhsx.virology

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import uk.nhs.nhsx.core.headers.MobileAppVersion
import uk.nhs.nhsx.virology.Country.Companion.England
import uk.nhs.nhsx.virology.TestKit.LAB_RESULT
import uk.nhs.nhsx.virology.TestKit.RAPID_RESULT
import uk.nhs.nhsx.virology.TestKit.RAPID_SELF_REPORTED
import uk.nhs.nhsx.virology.VirologyPolicyConfig.VirologyCriteria
import uk.nhs.nhsx.virology.result.TestResult
import uk.nhs.nhsx.virology.result.TestResult.*

class VirologyPolicyConfigTest {

    private val england = England
    private val wales = Country.of("Wales")

    private val doesNotRequireConfirmatoryTest = setOf(
        VirologyCriteria(england, LAB_RESULT, Positive),
        VirologyCriteria(england, LAB_RESULT, Negative),
        VirologyCriteria(england, LAB_RESULT, Void),
        VirologyCriteria(england, RAPID_RESULT, Positive),
        VirologyCriteria(england, RAPID_RESULT, Negative),
        VirologyCriteria(england, RAPID_RESULT, Void),
        VirologyCriteria(england, RAPID_SELF_REPORTED, Negative),
        VirologyCriteria(england, RAPID_SELF_REPORTED, Void),
        VirologyCriteria(wales, LAB_RESULT, Positive),
        VirologyCriteria(wales, LAB_RESULT, Negative),
        VirologyCriteria(wales, LAB_RESULT, Void),
        VirologyCriteria(wales, RAPID_RESULT, Positive),
        VirologyCriteria(wales, RAPID_RESULT, Negative),
        VirologyCriteria(wales, RAPID_RESULT, Void),
        VirologyCriteria(wales, RAPID_SELF_REPORTED, Negative),
        VirologyCriteria(wales, RAPID_SELF_REPORTED, Void),
    )

    private val requiresConfirmatoryTest = setOf(
        VirologyCriteria(england, RAPID_SELF_REPORTED, Positive),
        VirologyCriteria(wales, RAPID_SELF_REPORTED, Positive)
    )

    @Test
    fun `confirmatory test is not required`() {
        val config = VirologyPolicyConfig()
        doesNotRequireConfirmatoryTest.forEach {
            assertThat(config.isConfirmatoryTestRequired(it), equalTo(false))
        }
    }

    @Test
    fun `confirmatory test is required`() {
        val config = VirologyPolicyConfig()
        requiresConfirmatoryTest.forEach {
            assertThat(config.isConfirmatoryTestRequired(it), equalTo(true))
        }
    }

    @Test
    fun `supports diagnosis key submission`() {
        val config = VirologyPolicyConfig()
        val testCases = listOf(
            VirologyCriteria(england, LAB_RESULT, Positive),
            VirologyCriteria(england, RAPID_RESULT, Positive),
            VirologyCriteria(wales, LAB_RESULT, Positive),
            VirologyCriteria(wales, RAPID_RESULT, Positive)
        )

        testCases.forEach {
            assertThat(config.isDiagnosisKeysSubmissionSupported(it), equalTo(true))
        }
    }

    @Test
    fun `blocks diagnosis key submission for non positive results`() {
        val config = VirologyPolicyConfig()
        val testCases = listOf(
            VirologyCriteria(Country.of("Some-Country"), RAPID_RESULT, Negative),
            VirologyCriteria(Country.of("Some-Country"), LAB_RESULT, Negative),
            VirologyCriteria(Country.of("Some-Country"), RAPID_RESULT, Void),
            VirologyCriteria(Country.of("Some-Country"), LAB_RESULT, Void)
        )

        testCases.forEach {
            assertThat(config.isDiagnosisKeysSubmissionSupported(it), equalTo(false))
        }
    }

    @ParameterizedTest
    @EnumSource(TestKit::class)
    fun `blocks v1 test result queries`(testKit: TestKit) {
        val config = VirologyPolicyConfig(
            emptySet(),
            emptySet(),
            setOf(testKit)
        )
        assertThat(config.shouldBlockV1TestResultQueries(testKit), equalTo(true))
    }

    @ParameterizedTest
    @EnumSource(TestKit::class)
    fun `blocks none v1 test result queries`(testKit: TestKit) {
        val config = VirologyPolicyConfig(
            emptySet(),
            emptySet(),
            emptySet()
        )
        assertThat(config.shouldBlockV1TestResultQueries(testKit), equalTo(false))
    }

    @Test
    fun `blocks v2 for old versions if criteria requires confirmatory test`() {
        val oldVersion = MobileAppVersion.Version(4, 3)
        val config = VirologyPolicyConfig()
        requiresConfirmatoryTest.forEach {
            assertThat(config.shouldBlockV2TestResultQueries(it, oldVersion), equalTo(true))
        }
    }

    @Test
    fun `does not block v2 for new app versions if criteria requires confirmatory test`() {
        val newVersion = MobileAppVersion.Version(4, 4)
        val config = VirologyPolicyConfig()
        requiresConfirmatoryTest.forEach {
            assertThat(config.shouldBlockV2TestResultQueries(it, newVersion), equalTo(false))
        }
    }

    @Test
    fun `does not block v2 for old app versions if criteria does not require confirmatory test`() {
        val oldVersion = MobileAppVersion.Version(4, 3)
        val config = VirologyPolicyConfig()
        doesNotRequireConfirmatoryTest.forEach {
            assertThat(config.shouldBlockV2TestResultQueries(it, oldVersion), equalTo(false))
        }
    }

    @Test
    fun `does not block v2 for new app versions and criteria does not require confirmatory test`() {
        val version = MobileAppVersion.Version(4, 4)
        val config = VirologyPolicyConfig()
        doesNotRequireConfirmatoryTest.forEach {
            assertThat(config.shouldBlockV2TestResultQueries(it, version), equalTo(false))
        }
    }

    @Test
    fun `does not block v2 for unknown app versions even if criteria requires confirmatory test`() {
        val config = VirologyPolicyConfig()
        requiresConfirmatoryTest.forEach {
            assertThat(config.shouldBlockV2TestResultQueries(it, MobileAppVersion.Unknown), equalTo(false))
        }
    }

    @Test
    fun `current state for blocking v1 test result queries`() {
        val config = VirologyPolicyConfig()
        assertThat(config.shouldBlockV1TestResultQueries(LAB_RESULT), equalTo(false))
        assertThat(config.shouldBlockV1TestResultQueries(RAPID_RESULT), equalTo(false))
        assertThat(config.shouldBlockV1TestResultQueries(RAPID_SELF_REPORTED), equalTo(true))
    }
}
