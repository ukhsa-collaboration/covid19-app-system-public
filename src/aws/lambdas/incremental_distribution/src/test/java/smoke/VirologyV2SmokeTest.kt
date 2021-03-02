package smoke

import org.assertj.core.api.Assertions.assertThat
import org.http4k.client.JavaHttpClient
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import smoke.actors.ApiVersion.V2
import smoke.actors.MobileApp
import smoke.actors.TestLab
import smoke.actors.TestResult
import smoke.actors.TestResult.NEGATIVE
import smoke.actors.TestResult.POSITIVE
import smoke.actors.TestResultPollingToken
import smoke.actors.UserCountry
import smoke.actors.UserCountry.*
import smoke.env.SmokeTests
import uk.nhs.nhsx.core.headers.MobileAppVersion
import uk.nhs.nhsx.virology.Country
import uk.nhs.nhsx.virology.CtaToken
import uk.nhs.nhsx.virology.TestKit
import uk.nhs.nhsx.virology.TestKit.*
import uk.nhs.nhsx.virology.VirologyPolicyConfig.VirologyCriteria
import uk.nhs.nhsx.virology.VirologyUploadHandler.VirologyResultSource
import uk.nhs.nhsx.virology.VirologyUploadHandler.VirologyResultSource.Fiorano
import uk.nhs.nhsx.virology.VirologyUploadHandler.VirologyResultSource.Npex
import uk.nhs.nhsx.virology.VirologyUploadHandler.VirologyTokenExchangeSource
import uk.nhs.nhsx.virology.exchange.CtaExchangeResult
import uk.nhs.nhsx.virology.lookup.VirologyLookupResponseV2
import uk.nhs.nhsx.virology.order.VirologyOrderResponse
import uk.nhs.nhsx.virology.result.VirologyLookupResult
import uk.nhs.nhsx.virology.result.VirologyResultRequest.NPEX_POSITIVE

class VirologyV2SmokeTest {

    private val client = JavaHttpClient()
    private val config = SmokeTests.loadConfig()
    private val mobileApp = MobileApp(client, config)
    private val testLab = TestLab(client, config)

    private fun expectedSupportedFlagFor(country: UserCountry, testKit: TestKit, testResult: TestResult): Boolean {
        val supported = setOf(
            VirologyCriteria.of(Country.of("England"), LAB_RESULT, NPEX_POSITIVE),
            VirologyCriteria.of(Country.of("England"), RAPID_RESULT, NPEX_POSITIVE),
            VirologyCriteria.of(Country.of("Wales"), LAB_RESULT, NPEX_POSITIVE),
            VirologyCriteria.of(Country.of("Wales"), RAPID_RESULT, NPEX_POSITIVE)
        )
        return supported.contains(VirologyCriteria.of(Country.of(country.value), testKit, testResult.name))
    }

    private fun expectedRequiredFlagFor(country: UserCountry, testKit: TestKit, testResult: TestResult): Boolean {
        val required = setOf(
            VirologyCriteria.of(Country.of("England"), RAPID_SELF_REPORTED, NPEX_POSITIVE),
            VirologyCriteria.of(Country.of("Wales"), RAPID_SELF_REPORTED, NPEX_POSITIVE)
        )
        return required.contains(VirologyCriteria.of(Country.of(country.value), testKit, testResult.name))
    }

    private fun lookupResponse(orderResponse: VirologyOrderResponse, country: UserCountry): VirologyLookupResponseV2 {
        val pollingToken = TestResultPollingToken(orderResponse.testResultPollingToken)
        return (mobileApp.pollForTestResult(pollingToken, V2, country) as VirologyLookupResult.AvailableV2).virologyLookupResponse
    }

    @ParameterizedTest
    @EnumSource(TestKit::class)
    fun `order, upload positive result and poll via npex for all test kits`(testKit: TestKit) {
        val orderResponse = mobileApp.orderTest(V2)

        testLab.uploadTestResult(
            token = CtaToken.of(orderResponse.tokenParameterValue),
            result = POSITIVE,
            testKit = testKit,
            source = Npex,
            apiVersion = V2
        )

        val testResponse = lookupResponse(orderResponse, England)

        assertThat(testResponse.testResult).isEqualTo(POSITIVE.name)
        assertThat(testResponse.testKit).isEqualTo(testKit)
        assertThat(testResponse.testEndDate).isEqualTo("2020-04-23T00:00:00Z")
        assertThat(testResponse.diagnosisKeySubmissionSupported).isEqualTo(expectedSupportedFlagFor(England, testKit, POSITIVE))
        assertThat(testResponse.requiresConfirmatoryTest).isEqualTo(expectedRequiredFlagFor(England, testKit, POSITIVE))
    }

    @ParameterizedTest
    @EnumSource(TestKit::class)
    fun `order, upload positive result and poll via fiorano for all test kits`(testKit: TestKit) {
        val orderResponse = mobileApp.orderTest(V2)

        testLab.uploadTestResult(
            token = CtaToken.of(orderResponse.tokenParameterValue),
            result = POSITIVE,
            testKit = testKit,
            source = Fiorano,
            apiVersion = V2
        )

        val testResponse = lookupResponse(orderResponse, Wales)

        assertThat(testResponse.testResult).isEqualTo(POSITIVE.name)
        assertThat(testResponse.testKit).isEqualTo(testKit)
        assertThat(testResponse.testEndDate).isEqualTo("2020-04-23T00:00:00Z")
        assertThat(testResponse.diagnosisKeySubmissionSupported).isEqualTo(expectedSupportedFlagFor(Wales, testKit, POSITIVE))
        assertThat(testResponse.requiresConfirmatoryTest).isEqualTo(expectedRequiredFlagFor(Wales, testKit, POSITIVE))
    }

    @ParameterizedTest
    @EnumSource(value = TestKit::class, names = ["RAPID_RESULT", "RAPID_SELF_REPORTED"])
    fun `negative rapid test results uploaded via npex are rejected`(testKit: TestKit) {
        val orderResponse = mobileApp.orderTest(V2)

        testLab.uploadTestResultWithUnprocessableEntityV2(
            token = CtaToken.of(orderResponse.tokenParameterValue),
            result = NEGATIVE,
            testKit = testKit,
            source = Npex
        )
    }

    @ParameterizedTest
    @EnumSource(value = TestKit::class, names = ["RAPID_RESULT", "RAPID_SELF_REPORTED"])
    fun `negative rapid test results uploaded via fiorano are rejected`(testKit: TestKit) {
        val orderResponse = mobileApp.orderTest(V2)

        testLab.uploadTestResultWithUnprocessableEntityV2(
            token = CtaToken.of(orderResponse.tokenParameterValue),
            result = NEGATIVE,
            testKit = testKit,
            source = Fiorano
        )
    }

    @ParameterizedTest
    @EnumSource(VirologyResultSource::class)
    fun `order, upload and poll negative lab result for all sources`(source: VirologyResultSource) {
        val orderResponse = mobileApp.orderTest(V2)

        testLab.uploadTestResult(
            token = CtaToken.of(orderResponse.tokenParameterValue),
            result = NEGATIVE,
            testKit = LAB_RESULT,
            source = source,
            apiVersion = V2
        )

        val testResponse = lookupResponse(orderResponse, England)

        assertThat(testResponse.testResult).isEqualTo(NEGATIVE.name)
        assertThat(testResponse.testKit).isEqualTo(LAB_RESULT)
        assertThat(testResponse.testEndDate).isEqualTo("2020-04-23T00:00:00Z")
        assertThat(testResponse.diagnosisKeySubmissionSupported).isEqualTo(expectedSupportedFlagFor(England, LAB_RESULT, NEGATIVE))
        assertThat(testResponse.requiresConfirmatoryTest).isEqualTo(expectedRequiredFlagFor(England, LAB_RESULT, NEGATIVE))
    }

    @Test
    fun `lookup pending for old app versions and criteria that requires confirmatory test`() {
        val mobileApp = MobileApp(client, config, appVersion = MobileAppVersion.Version(4, 3))

        val orderResponse = mobileApp.orderTest(V2)

        testLab.uploadTestResult(
            token = CtaToken.of(orderResponse.tokenParameterValue),
            result = POSITIVE,
            testKit = RAPID_SELF_REPORTED,
            source = Npex,
            apiVersion = V2
        )

        val testResponse = mobileApp.pollForTestResult(TestResultPollingToken(orderResponse.testResultPollingToken), V2, England)
        assertThat(testResponse).isInstanceOf(VirologyLookupResult.Pending::class.java)
    }

    @ParameterizedTest
    @EnumSource(TestKit::class)
    fun `lab token gen and cta exchange via eng source for all test kits`(testKit: TestKit) {
        val ctaToken = testLab.generateCtaTokenFor(
            testResult = POSITIVE,
            testEndDate = "2020-11-19T00:00:00Z",
            source = VirologyTokenExchangeSource.Eng,
            apiVersion = V2,
            testKit = testKit
        )

        val exchangeResponse = mobileApp.exchange(ctaToken, V2, England)

        val ctaExchangeResponse = (exchangeResponse as CtaExchangeResult.AvailableV2).ctaExchangeResponse
        assertThat(ctaExchangeResponse.testResult).isEqualTo(POSITIVE.name)
        assertThat(ctaExchangeResponse.testEndDate).isEqualTo("2020-11-19T00:00:00Z")
        assertThat(ctaExchangeResponse.testKit).isEqualTo(testKit)
        assertThat(ctaExchangeResponse.diagnosisKeySubmissionSupported).isEqualTo(expectedSupportedFlagFor(England, testKit, POSITIVE))
        assertThat(ctaExchangeResponse.requiresConfirmatoryTest).isEqualTo(expectedRequiredFlagFor(England, testKit, POSITIVE))
    }

    @ParameterizedTest
    @EnumSource(TestKit::class)
    fun `lab token gen and cta exchange via wls source for all test kits`(testKit: TestKit) {
        val ctaToken = testLab.generateCtaTokenFor(
            testResult = POSITIVE,
            testEndDate = "2020-11-19T00:00:00Z",
            source = VirologyTokenExchangeSource.Wls,
            apiVersion = V2,
            testKit = testKit
        )

        val exchangeResponse = mobileApp.exchange(ctaToken, V2, Wales)

        val ctaExchangeResponse = (exchangeResponse as CtaExchangeResult.AvailableV2).ctaExchangeResponse
        assertThat(ctaExchangeResponse.testResult).isEqualTo(POSITIVE.name)
        assertThat(ctaExchangeResponse.testEndDate).isEqualTo("2020-11-19T00:00:00Z")
        assertThat(ctaExchangeResponse.testKit).isEqualTo(testKit)
        assertThat(ctaExchangeResponse.diagnosisKeySubmissionSupported).isEqualTo(expectedSupportedFlagFor(Wales, testKit, POSITIVE))
        assertThat(ctaExchangeResponse.requiresConfirmatoryTest).isEqualTo(expectedRequiredFlagFor(Wales, testKit, POSITIVE))
    }

    @Test
    fun `cta exchange not found for old app versions and criteria that requires confirmatory test`() {
        val ctaToken = testLab.generateCtaTokenFor(
            testResult = POSITIVE,
            testEndDate = "2020-11-19T00:00:00Z",
            source = VirologyTokenExchangeSource.Wls,
            apiVersion = V2,
            testKit = RAPID_SELF_REPORTED
        )

        val mobileApp = MobileApp(client, config, appVersion = MobileAppVersion.Version(4, 3))
        val exchangeResponse = mobileApp.exchange(ctaToken, V2, Wales)
        assertThat(exchangeResponse).isInstanceOf(CtaExchangeResult.NotFound::class.java)
    }

}
