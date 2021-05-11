package smoke

import org.assertj.core.api.Assertions.assertThat
import org.http4k.client.JavaHttpClient
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import smoke.actors.ApiVersion.V2
import smoke.actors.MobileApp
import smoke.actors.TestLab
import smoke.env.SmokeTests
import uk.nhs.nhsx.core.headers.MobileAppVersion
import uk.nhs.nhsx.domain.Country
import uk.nhs.nhsx.domain.Country.Companion.England
import uk.nhs.nhsx.domain.Country.Companion.Wales
import uk.nhs.nhsx.domain.TestEndDate
import uk.nhs.nhsx.domain.TestKit
import uk.nhs.nhsx.domain.TestKit.*
import uk.nhs.nhsx.domain.TestResult
import uk.nhs.nhsx.domain.TestResult.Negative
import uk.nhs.nhsx.domain.TestResult.Positive
import uk.nhs.nhsx.virology.VirologyPolicyConfig.VirologyCriteria
import uk.nhs.nhsx.virology.VirologyUploadHandler.VirologyResultSource
import uk.nhs.nhsx.virology.VirologyUploadHandler.VirologyResultSource.Fiorano
import uk.nhs.nhsx.virology.VirologyUploadHandler.VirologyResultSource.Npex
import uk.nhs.nhsx.virology.VirologyUploadHandler.VirologyTokenExchangeSource
import uk.nhs.nhsx.virology.exchange.CtaExchangeResult
import uk.nhs.nhsx.virology.lookup.VirologyLookupResponseV2
import uk.nhs.nhsx.virology.lookup.VirologyLookupResult
import uk.nhs.nhsx.virology.order.VirologyOrderResponse

class VirologyV2SmokeTest {

    private val client = JavaHttpClient()
    private val config = SmokeTests.loadConfig()
    private val mobileApp = MobileApp(client, config)
    private val testLab = TestLab(client, config)

    private fun expectedSupportedFlagFor(country: Country, testKit: TestKit, testResult: TestResult): Boolean {
        val supported = setOf(
            VirologyCriteria(England, LAB_RESULT, Positive),
            VirologyCriteria(England, RAPID_RESULT, Positive),
            VirologyCriteria(Wales, LAB_RESULT, Positive),
            VirologyCriteria(Wales, RAPID_RESULT, Positive),
            VirologyCriteria(Wales, RAPID_SELF_REPORTED, Positive)

        )
        return supported.contains(VirologyCriteria(country, testKit, testResult))
    }

    private fun expectedRequiredFlagFor(country: Country, testKit: TestKit, testResult: TestResult): Boolean {
        val required = setOf(
            VirologyCriteria(England, RAPID_SELF_REPORTED, Positive)
        )
        return required.contains(VirologyCriteria(country, testKit, testResult))
    }

    private fun lookupResponse(orderResponse: VirologyOrderResponse, country: Country): VirologyLookupResponseV2 {
        val pollingToken = orderResponse.testResultPollingToken
        return (mobileApp.pollForTestResult(pollingToken, V2, country) as VirologyLookupResult.AvailableV2).response
    }

    @ParameterizedTest
    @EnumSource(TestKit::class)
    fun `order, upload positive result and poll via npex for all test kits`(testKit: TestKit) {
        val orderResponse = mobileApp.orderTest(V2)

        testLab.uploadTestResult(
            token = orderResponse.tokenParameterValue,
            result = Positive,
            testKit = testKit,
            source = Npex,
            apiVersion = V2
        )

        val testResponse = lookupResponse(orderResponse, England)

        assertThat(testResponse.testResult).isEqualTo(Positive)
        assertThat(testResponse.testKit).isEqualTo(testKit)
        assertThat(testResponse.testEndDate).isEqualTo(TestEndDate.of(2020, 4, 23))
        assertThat(testResponse.diagnosisKeySubmissionSupported).isEqualTo(
            expectedSupportedFlagFor(
                England,
                testKit,
                Positive
            )
        )
        assertThat(testResponse.requiresConfirmatoryTest).isEqualTo(
            expectedRequiredFlagFor(
                England,
                testKit,
                Positive
            )
        )
    }

    @ParameterizedTest
    @EnumSource(TestKit::class)
    fun `order, upload positive result and poll via fiorano for all test kits`(testKit: TestKit) {
        val orderResponse = mobileApp.orderTest(V2)

        testLab.uploadTestResult(
            token = orderResponse.tokenParameterValue,
            result = Positive,
            testKit = testKit,
            source = Fiorano,
            apiVersion = V2
        )

        val testResponse = lookupResponse(orderResponse, Wales)

        assertThat(testResponse.testResult).isEqualTo(Positive)
        assertThat(testResponse.testKit).isEqualTo(testKit)
        assertThat(testResponse.testEndDate).isEqualTo(TestEndDate.of(2020, 4, 23))
        assertThat(testResponse.diagnosisKeySubmissionSupported).isEqualTo(
            expectedSupportedFlagFor(
                Wales,
                testKit,
                Positive
            )
        )
        assertThat(testResponse.requiresConfirmatoryTest).isEqualTo(
            expectedRequiredFlagFor(
                Wales,
                testKit,
                Positive
            )
        )
    }

    @ParameterizedTest
    @EnumSource(value = TestKit::class, names = ["RAPID_RESULT", "RAPID_SELF_REPORTED"])
    fun `negative rapid test results uploaded via npex are rejected`(testKit: TestKit) {
        val orderResponse = mobileApp.orderTest(V2)

        testLab.uploadTestResultWithUnprocessableEntityV2(
            token = orderResponse.tokenParameterValue,
            result = Negative,
            testKit = testKit,
            source = Npex
        )
    }

    @ParameterizedTest
    @EnumSource(value = TestKit::class, names = ["RAPID_RESULT", "RAPID_SELF_REPORTED"])
    fun `negative rapid test results uploaded via fiorano are rejected`(testKit: TestKit) {
        val orderResponse = mobileApp.orderTest(V2)

        testLab.uploadTestResultWithUnprocessableEntityV2(
            token = orderResponse.tokenParameterValue,
            result = Negative,
            testKit = testKit,
            source = Fiorano
        )
    }

    @ParameterizedTest
    @EnumSource(VirologyResultSource::class)
    fun `order, upload and poll negative lab result for all sources`(source: VirologyResultSource) {
        val orderResponse = mobileApp.orderTest(V2)

        testLab.uploadTestResult(
            token = orderResponse.tokenParameterValue,
            result = Negative,
            testKit = LAB_RESULT,
            source = source,
            apiVersion = V2
        )

        val testResponse = lookupResponse(orderResponse, England)

        assertThat(testResponse.testResult).isEqualTo(Negative)
        assertThat(testResponse.testKit).isEqualTo(LAB_RESULT)
        assertThat(testResponse.testEndDate).isEqualTo(TestEndDate.of(2020, 4, 23))
        assertThat(testResponse.diagnosisKeySubmissionSupported).isEqualTo(
            expectedSupportedFlagFor(
                England,
                LAB_RESULT,
                Negative
            )
        )
        assertThat(testResponse.requiresConfirmatoryTest).isEqualTo(
            expectedRequiredFlagFor(
                England,
                LAB_RESULT,
                Negative
            )
        )
    }

    @Test
    fun `lookup pending for old app versions and criteria that requires confirmatory test`() {
        val mobileApp = MobileApp(client, config, appVersion = MobileAppVersion.Version(4, 3))

        val orderResponse = mobileApp.orderTest(V2)

        testLab.uploadTestResult(
            token = orderResponse.tokenParameterValue,
            result = Positive,
            testKit = RAPID_SELF_REPORTED,
            source = Npex,
            apiVersion = V2
        )

        val testResponse = mobileApp.pollForTestResult(orderResponse.testResultPollingToken, V2, England)
        assertThat(testResponse).isInstanceOf(VirologyLookupResult.Pending::class.java)
    }

    @ParameterizedTest
    @EnumSource(TestKit::class)
    fun `lab token gen and cta exchange via eng source for all test kits`(testKit: TestKit) {
        val ctaToken = testLab.generateCtaTokenFor(
            testResult = Positive,
            testEndDate = TestEndDate.of(2020, 11, 19),
            source = VirologyTokenExchangeSource.Eng,
            apiVersion = V2,
            testKit = testKit
        )

        val exchangeResponse = mobileApp.exchange(ctaToken, V2, England)

        val ctaExchangeResponse = (exchangeResponse as CtaExchangeResult.AvailableV2).ctaExchangeResponse
        assertThat(ctaExchangeResponse.testResult).isEqualTo(Positive)
        assertThat(ctaExchangeResponse.testEndDate).isEqualTo(TestEndDate.of(2020, 11, 19))
        assertThat(ctaExchangeResponse.testKit).isEqualTo(testKit)
        assertThat(ctaExchangeResponse.diagnosisKeySubmissionSupported).isEqualTo(
            expectedSupportedFlagFor(
                England,
                testKit,
                Positive
            )
        )
        assertThat(ctaExchangeResponse.requiresConfirmatoryTest).isEqualTo(
            expectedRequiredFlagFor(
                England,
                testKit,
                Positive
            )
        )
    }

    @ParameterizedTest
    @EnumSource(TestKit::class)
    fun `lab token gen and cta exchange via wls source for all test kits`(testKit: TestKit) {
        val ctaToken = testLab.generateCtaTokenFor(
            testResult = Positive,
            testEndDate = TestEndDate.of(2020, 11, 19),
            source = VirologyTokenExchangeSource.Wls,
            apiVersion = V2,
            testKit = testKit
        )

        val exchangeResponse = mobileApp.exchange(ctaToken, V2, Wales)

        val ctaExchangeResponse = (exchangeResponse as CtaExchangeResult.AvailableV2).ctaExchangeResponse
        assertThat(ctaExchangeResponse.testResult).isEqualTo(Positive)
        assertThat(ctaExchangeResponse.testEndDate).isEqualTo(TestEndDate.of(2020, 11, 19))
        assertThat(ctaExchangeResponse.testKit).isEqualTo(testKit)
        assertThat(ctaExchangeResponse.diagnosisKeySubmissionSupported).isEqualTo(
            expectedSupportedFlagFor(
                Wales,
                testKit,
                Positive
            )
        )
        assertThat(ctaExchangeResponse.requiresConfirmatoryTest).isEqualTo(
            expectedRequiredFlagFor(
                Wales,
                testKit,
                Positive
            )
        )
    }

    @Test
    fun `cta exchange not found for old app versions and criteria that requires confirmatory test`() {
        val ctaToken = testLab.generateCtaTokenFor(
            testResult = Positive,
            testEndDate = TestEndDate.of(2020, 11, 19),
            source = VirologyTokenExchangeSource.Eng,
            apiVersion = V2,
            testKit = RAPID_SELF_REPORTED
        )

        val mobileApp = MobileApp(client, config, appVersion = MobileAppVersion.Version(4, 3))
        val exchangeResponse = mobileApp.exchange(ctaToken, V2, England)
        assertThat(exchangeResponse).isInstanceOf(CtaExchangeResult.NotFound::class.java)
    }

    @ParameterizedTest
    @EnumSource(TestKit::class)
    fun `check for token status returns consumable when token has been generated and not consumed for all Eng test kits`(
        testKit: TestKit
    ) {
        val ctaToken = testLab.generateCtaTokenFor(
            testResult = Positive,
            testEndDate = TestEndDate.of(2020, 11, 19),
            source = VirologyTokenExchangeSource.Eng,
            apiVersion = V2,
            testKit = testKit
        )
        val tokenCheckResponse = testLab.checkToken(ctaToken, VirologyTokenExchangeSource.Eng)
        assertThat(tokenCheckResponse.tokenStatus).isEqualTo("consumable")

    }

    @ParameterizedTest
    @EnumSource(TestKit::class)
    fun `check for token status returns consumable when token has been generated and not consumed for all Wls test kits`(
        testKit: TestKit
    ) {
        val ctaToken = testLab.generateCtaTokenFor(
            testResult = Positive,
            testEndDate = TestEndDate.of(2020, 11, 19),
            source = VirologyTokenExchangeSource.Wls,
            apiVersion = V2,
            testKit = testKit
        )
        val tokenCheckResponse = testLab.checkToken(ctaToken, VirologyTokenExchangeSource.Wls)
        assertThat(tokenCheckResponse.tokenStatus).isEqualTo("consumable")

    }

    @ParameterizedTest
    @EnumSource(TestKit::class)
    fun `check for token status returns other when token has been generated and consumed for all Eng test kits`(testKit: TestKit) {
        val ctaToken = testLab.generateCtaTokenFor(
            testResult = Positive,
            testEndDate = TestEndDate.of(2020, 11, 19),
            source = VirologyTokenExchangeSource.Eng,
            apiVersion = V2,
            testKit = testKit
        )
        mobileApp.exchange(ctaToken, V2, England)
        val tokenCheckResponse = testLab.checkToken(ctaToken, VirologyTokenExchangeSource.Eng)
        assertThat(tokenCheckResponse.tokenStatus).isEqualTo("other")

    }
    @ParameterizedTest
    @EnumSource(TestKit::class)
    fun `check for token status returns other when token has been generated and consumed for all Wls test kits`(testKit: TestKit) {
        val ctaToken = testLab.generateCtaTokenFor(
            testResult = Positive,
            testEndDate = TestEndDate.of(2020, 11, 19),
            source = VirologyTokenExchangeSource.Wls,
            apiVersion = V2,
            testKit = testKit
        )
        mobileApp.exchange(ctaToken, V2, Wales)
        val tokenCheckResponse = testLab.checkToken(ctaToken, VirologyTokenExchangeSource.Wls)
        assertThat(tokenCheckResponse.tokenStatus).isEqualTo("other")

    }
}
