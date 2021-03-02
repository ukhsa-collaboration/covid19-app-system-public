package uk.nhs.nhsx.virology

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import uk.nhs.nhsx.core.headers.MobileAppVersion
import uk.nhs.nhsx.virology.TestKit.LAB_RESULT
import uk.nhs.nhsx.virology.TestKit.RAPID_RESULT
import uk.nhs.nhsx.virology.TestKit.RAPID_SELF_REPORTED
import uk.nhs.nhsx.virology.VirologyPolicyConfig.VirologyCriteria
import uk.nhs.nhsx.virology.result.VirologyResultRequest.NPEX_NEGATIVE
import uk.nhs.nhsx.virology.result.VirologyResultRequest.NPEX_POSITIVE
import uk.nhs.nhsx.virology.result.VirologyResultRequest.NPEX_VOID

class VirologyPolicyConfigTest {

    private val doesNotRequireConfirmatoryTest = setOf(
        VirologyCriteria.of(Country.of("England"), LAB_RESULT, NPEX_POSITIVE),
        VirologyCriteria.of(Country.of("England"), LAB_RESULT, NPEX_NEGATIVE),
        VirologyCriteria.of(Country.of("England"), LAB_RESULT, NPEX_VOID),
        VirologyCriteria.of(Country.of("England"), RAPID_RESULT, NPEX_POSITIVE),
        VirologyCriteria.of(Country.of("England"), RAPID_RESULT, NPEX_NEGATIVE),
        VirologyCriteria.of(Country.of("England"), RAPID_RESULT, NPEX_VOID),
        VirologyCriteria.of(Country.of("England"), RAPID_SELF_REPORTED, NPEX_NEGATIVE),
        VirologyCriteria.of(Country.of("England"), RAPID_SELF_REPORTED, NPEX_VOID),
        VirologyCriteria.of(Country.of("Wales"), LAB_RESULT, NPEX_POSITIVE),
        VirologyCriteria.of(Country.of("Wales"), LAB_RESULT, NPEX_NEGATIVE),
        VirologyCriteria.of(Country.of("Wales"), LAB_RESULT, NPEX_VOID),
        VirologyCriteria.of(Country.of("Wales"), RAPID_RESULT, NPEX_POSITIVE),
        VirologyCriteria.of(Country.of("Wales"), RAPID_RESULT, NPEX_NEGATIVE),
        VirologyCriteria.of(Country.of("Wales"), RAPID_RESULT, NPEX_VOID),
        VirologyCriteria.of(Country.of("Wales"), RAPID_SELF_REPORTED, NPEX_NEGATIVE),
        VirologyCriteria.of(Country.of("Wales"), RAPID_SELF_REPORTED, NPEX_VOID),
    )

    private val requiresConfirmatoryTest = setOf(
        VirologyCriteria.of(Country.of("England"), RAPID_SELF_REPORTED, NPEX_POSITIVE),
        VirologyCriteria.of(Country.of("Wales"), RAPID_SELF_REPORTED, NPEX_POSITIVE)
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
            VirologyCriteria.of(Country.of("England"), LAB_RESULT, NPEX_POSITIVE),
            VirologyCriteria.of(Country.of("England"), RAPID_RESULT, NPEX_POSITIVE),
            VirologyCriteria.of(Country.of("Wales"), LAB_RESULT, NPEX_POSITIVE),
            VirologyCriteria.of(Country.of("Wales"), RAPID_RESULT, NPEX_POSITIVE)
        )

        testCases.forEach {
            assertThat(config.isDiagnosisKeysSubmissionSupported(it), equalTo(true))
        }
    }

    @Test
    fun `blocks diagnosis key submission for non positive results`() {
        val config = VirologyPolicyConfig()
        val testCases = listOf(
            VirologyCriteria.of(Country.of("Some-Country"), RAPID_RESULT, NPEX_NEGATIVE),
            VirologyCriteria.of(Country.of("Some-Country"), LAB_RESULT, NPEX_NEGATIVE),
            VirologyCriteria.of(Country.of("Some-Country"), RAPID_RESULT, NPEX_VOID),
            VirologyCriteria.of(Country.of("Some-Country"), LAB_RESULT, NPEX_VOID)
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
