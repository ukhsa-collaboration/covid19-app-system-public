package smoke

import assertions.CtaExchangeAssertionsV2.confirmatoryDayLimit
import assertions.CtaExchangeAssertionsV2.diagnosisKeySubmissionSupported
import assertions.CtaExchangeAssertionsV2.requiresConfirmatoryTest
import assertions.CtaExchangeAssertionsV2.shouldOfferFollowUpTest
import assertions.CtaExchangeAssertionsV2.testEndDate
import assertions.CtaExchangeAssertionsV2.testKit
import assertions.CtaExchangeAssertionsV2.testResult
import assertions.VirologyAssertionsV2.confirmatoryDayLimit
import assertions.VirologyAssertionsV2.diagnosisKeySubmissionSupported
import assertions.VirologyAssertionsV2.requiresConfirmatoryTest
import assertions.VirologyAssertionsV2.shouldOfferFollowUpTest
import assertions.VirologyAssertionsV2.testEndDate
import assertions.VirologyAssertionsV2.testKit
import assertions.VirologyAssertionsV2.testResult
import org.http4k.cloudnative.env.Environment
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import smoke.actors.ApiVersion.V2
import smoke.actors.MobileApp
import smoke.actors.TestLab
import smoke.actors.createHandler
import smoke.env.SmokeTests
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNull
import uk.nhs.nhsx.core.headers.MobileAppVersion
import uk.nhs.nhsx.domain.Country
import uk.nhs.nhsx.domain.Country.Companion.England
import uk.nhs.nhsx.domain.Country.Companion.Wales
import uk.nhs.nhsx.domain.TestEndDate
import uk.nhs.nhsx.domain.TestJourney
import uk.nhs.nhsx.domain.TestJourney.CtaExchange
import uk.nhs.nhsx.domain.TestJourney.Lookup
import uk.nhs.nhsx.domain.TestKit
import uk.nhs.nhsx.domain.TestKit.LAB_RESULT
import uk.nhs.nhsx.domain.TestKit.RAPID_RESULT
import uk.nhs.nhsx.domain.TestKit.RAPID_SELF_REPORTED
import uk.nhs.nhsx.domain.TestResult
import uk.nhs.nhsx.domain.TestResult.Negative
import uk.nhs.nhsx.domain.TestResult.Plod
import uk.nhs.nhsx.domain.TestResult.Positive
import uk.nhs.nhsx.virology.VirologyUploadHandler.VirologyResultSource
import uk.nhs.nhsx.virology.VirologyUploadHandler.VirologyResultSource.Fiorano
import uk.nhs.nhsx.virology.VirologyUploadHandler.VirologyResultSource.Npex
import uk.nhs.nhsx.virology.VirologyUploadHandler.VirologyTokenExchangeSource
import uk.nhs.nhsx.virology.exchange.CtaExchangeResult
import uk.nhs.nhsx.virology.lookup.VirologyLookupResponseV2
import uk.nhs.nhsx.virology.lookup.VirologyLookupResult
import uk.nhs.nhsx.virology.order.VirologyOrderResponse
import uk.nhs.nhsx.virology.policy.VirologyCriteria
import uk.nhs.nhsx.virology.result.VirologyTokenStatusResponse

class VirologyV2SmokeTest {

    private val client = createHandler(Environment.ENV)
    private val config = SmokeTests.loadConfig()
    private val mobileApp = MobileApp(client, config)
    private val testLab = TestLab(client, config)

    private fun expectedSupportedFlagFor(
        testJourney: TestJourney,
        country: Country,
        testKit: TestKit,
        testResult: TestResult
    ): Boolean {
        val supported = setOf(
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

        )
        return supported.contains(VirologyCriteria(testJourney, country, testKit, testResult))
    }

    private fun expectedRequiredFlagFor(
        testJourney: TestJourney,
        country: Country,
        testKit: TestKit,
        testResult: TestResult
    ): Boolean {
        val required = setOf<VirologyCriteria>()
        return required.contains(VirologyCriteria(testJourney, country, testKit, testResult))
    }

    private fun lookupResponse(orderResponse: VirologyOrderResponse, country: Country): VirologyLookupResponseV2 {
        val pollingToken = orderResponse.testResultPollingToken
        return (mobileApp.pollForTestResult(pollingToken, V2, country) as VirologyLookupResult.AvailableV2).response
    }

    @ParameterizedTest
    @EnumSource(TestKit::class)
    fun `order, upload positive result and poll via npex for all test kits`(input: TestKit) {
        val orderResponse = mobileApp.orderTest(V2)

        testLab.uploadTestResult(
            token = orderResponse.tokenParameterValue,
            result = Positive,
            testKit = input,
            source = Npex,
            apiVersion = V2
        )

        val testResponse: VirologyLookupResponseV2 = lookupResponse(orderResponse, England)

        expectThat(testResponse) {
            testResult.isEqualTo(Positive)
            testKit.isEqualTo(input)
            testEndDate.isEqualTo(TestEndDate.of(2020, 4, 23))
            diagnosisKeySubmissionSupported.isEqualTo(
                expectedSupportedFlagFor(Lookup, England, input, Positive)
            )
            requiresConfirmatoryTest.isEqualTo(
                expectedRequiredFlagFor(Lookup, England, input, Positive)
            )
            confirmatoryDayLimit.isNull()
            shouldOfferFollowUpTest.isFalse()
        }
    }

    @Test
    fun `order, upload plod result and poll via npex`() {
        val orderResponse = mobileApp.orderTest(V2)

        testLab.uploadTestResult(
            token = orderResponse.tokenParameterValue,
            result = Plod,
            testKit = LAB_RESULT,
            source = Npex,
            apiVersion = V2
        )

        val testResponse = lookupResponse(orderResponse, England)

        expectThat(testResponse) {
            testResult.isEqualTo(Plod)
            testKit.isEqualTo(LAB_RESULT)
            testEndDate.isEqualTo(TestEndDate.of(2020, 4, 23))
            diagnosisKeySubmissionSupported.isFalse()
            requiresConfirmatoryTest.isFalse()
            confirmatoryDayLimit.isNull()
            shouldOfferFollowUpTest.isFalse()
        }
    }

    @ParameterizedTest
    @EnumSource(TestKit::class)
    fun `order, upload positive result and poll via fiorano for all test kits`(input: TestKit) {
        val orderResponse = mobileApp.orderTest(V2)

        testLab.uploadTestResult(
            token = orderResponse.tokenParameterValue,
            result = Positive,
            testKit = input,
            source = Fiorano,
            apiVersion = V2
        )

        val testResponse = lookupResponse(orderResponse, Wales)

        expectThat(testResponse) {
            testResult.isEqualTo(Positive)
            testKit.isEqualTo(input)
            testEndDate.isEqualTo(TestEndDate.of(2020, 4, 23))
            diagnosisKeySubmissionSupported.isEqualTo(
                expectedSupportedFlagFor(Lookup, Wales, input, Positive)
            )
            requiresConfirmatoryTest.isEqualTo(
                expectedRequiredFlagFor(Lookup, Wales, input, Positive)
            )
            confirmatoryDayLimit.isNull()
            shouldOfferFollowUpTest.isFalse()
        }
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

        expectThat(testResponse) {
            testResult.isEqualTo(Negative)
            testKit.isEqualTo(LAB_RESULT)
            testEndDate.isEqualTo(TestEndDate.of(2020, 4, 23))
            diagnosisKeySubmissionSupported.isEqualTo(
                expectedSupportedFlagFor(Lookup, England, LAB_RESULT, Negative)
            )
            requiresConfirmatoryTest.isEqualTo(
                expectedRequiredFlagFor(Lookup, England, LAB_RESULT, Negative)
            )
            confirmatoryDayLimit.isNull()
            shouldOfferFollowUpTest.isFalse()
        }
    }

    @ParameterizedTest
    @EnumSource(TestKit::class)
    fun `lab token gen and cta exchange via eng source for all test kits`(input: TestKit) {
        val ctaToken = testLab.generateCtaTokenFor(
            testResult = Positive,
            testEndDate = TestEndDate.of(2020, 11, 19),
            source = VirologyTokenExchangeSource.Eng,
            apiVersion = V2,
            testKit = input
        )

        val exchangeResponse = mobileApp.exchange(ctaToken, V2, England)

        expectThat(exchangeResponse)
            .isA<CtaExchangeResult.AvailableV2>()
            .get(CtaExchangeResult.AvailableV2::ctaExchangeResponse).and {
                testResult.isEqualTo(Positive)
                testEndDate.isEqualTo(TestEndDate.of(2020, 11, 19))
                testKit.isEqualTo(input)
                diagnosisKeySubmissionSupported.isEqualTo(
                    expectedSupportedFlagFor(CtaExchange, England, input, Positive)
                )
                requiresConfirmatoryTest.isEqualTo(
                    expectedRequiredFlagFor(CtaExchange, England, input, Positive)
                )
                confirmatoryDayLimit.isNull()
                shouldOfferFollowUpTest.isFalse()
            }
    }

    @Test
    fun `lab token gen and cta exchange via eng source for all test kits`() {
        val ctaToken = testLab.generateCtaTokenFor(
            testResult = Plod,
            testEndDate = TestEndDate.of(2020, 11, 19),
            source = VirologyTokenExchangeSource.Eng,
            apiVersion = V2,
            testKit = LAB_RESULT
        )

        val exchangeResponse = mobileApp.exchange(ctaToken, V2, England)

        expectThat(exchangeResponse)
            .isA<CtaExchangeResult.AvailableV2>()
            .get(CtaExchangeResult.AvailableV2::ctaExchangeResponse).and {
                testResult.isEqualTo(Plod)
                testKit.isEqualTo(LAB_RESULT)
                testEndDate.isEqualTo(TestEndDate.of(2020, 11, 19))
                diagnosisKeySubmissionSupported.isFalse()
                requiresConfirmatoryTest.isFalse()
                confirmatoryDayLimit.isNull()
                shouldOfferFollowUpTest.isFalse()
            }
    }

    @ParameterizedTest
    @EnumSource(TestKit::class)
    fun `lab token gen and cta exchange via wls source for all test kits`(input: TestKit) {
        val ctaToken = testLab.generateCtaTokenFor(
            testResult = Positive,
            testEndDate = TestEndDate.of(2020, 11, 19),
            source = VirologyTokenExchangeSource.Wls,
            apiVersion = V2,
            testKit = input
        )

        val exchangeResponse = mobileApp.exchange(ctaToken, V2, Wales)

        expectThat(exchangeResponse)
            .isA<CtaExchangeResult.AvailableV2>()
            .get(CtaExchangeResult.AvailableV2::ctaExchangeResponse).and {
                testResult.isEqualTo(Positive)
                testKit.isEqualTo(input)
                testEndDate.isEqualTo(TestEndDate.of(2020, 11, 19))
                diagnosisKeySubmissionSupported.isEqualTo(
                    expectedSupportedFlagFor(CtaExchange, Wales, input, Positive)
                )
                requiresConfirmatoryTest.isEqualTo(
                    expectedRequiredFlagFor(CtaExchange, Wales, input, Positive)
                )
                confirmatoryDayLimit.isNull()
                shouldOfferFollowUpTest.isFalse()
            }
    }

    @ParameterizedTest
    @CsvSource(value = ["4, 26, false", "4, 25, false"])
    fun `cta exchange available for specific app versions which require confirmatory test`(
        majorAppVersion: Int,
        minorAppVersion: Int,
        confirmatoryTestRequired: Boolean
    ) {
        val ctaToken = testLab.generateCtaTokenFor(
            testResult = Positive,
            testEndDate = TestEndDate.of(2020, 11, 19),
            source = VirologyTokenExchangeSource.Eng,
            apiVersion = V2,
            testKit = RAPID_SELF_REPORTED
        )

        val appVersion = MobileAppVersion.Version(majorAppVersion, minorAppVersion)
        val mobileApp = MobileApp(client, config, appVersion = appVersion)
        val exchangeResponse = mobileApp.exchange(ctaToken, V2, England)

        expectThat(exchangeResponse).isA<CtaExchangeResult.AvailableV2>()
            .get(CtaExchangeResult.AvailableV2::ctaExchangeResponse).and {
                requiresConfirmatoryTest.isEqualTo(confirmatoryTestRequired)
                shouldOfferFollowUpTest.isFalse()
            }
    }

    @Test
    fun `cta exchange for new app versions and criteria that sets confirmatory day limit for England`() {
        val ctaToken = testLab.generateCtaTokenFor(
            testResult = Positive,
            testEndDate = TestEndDate.of(2020, 11, 19),
            source = VirologyTokenExchangeSource.Eng,
            apiVersion = V2,
            testKit = RAPID_RESULT
        )

        val mobileApp = MobileApp(client, config, appVersion = MobileAppVersion.Version(4, 11))
        val exchangeResponse = mobileApp.exchange(ctaToken, V2, England)

        expectThat(exchangeResponse)
            .isA<CtaExchangeResult.AvailableV2>()
            .get(CtaExchangeResult.AvailableV2::ctaExchangeResponse).and {
                confirmatoryDayLimit.isNull()
                requiresConfirmatoryTest.isFalse()
                shouldOfferFollowUpTest.isFalse()
            }
    }

    @Test
    fun `cta exchange for new app versions and criteria that sets confirmatory day limit for Wales`() {
        val ctaToken = testLab.generateCtaTokenFor(
            testResult = Positive,
            testEndDate = TestEndDate.of(2020, 11, 19),
            source = VirologyTokenExchangeSource.Wls,
            apiVersion = V2,
            testKit = RAPID_RESULT
        )

        val mobileApp = MobileApp(client, config, appVersion = MobileAppVersion.Version(4, 11))
        val exchangeResponse = mobileApp.exchange(ctaToken, V2, Wales)

        expectThat(exchangeResponse)
            .isA<CtaExchangeResult.AvailableV2>()
            .get(CtaExchangeResult.AvailableV2::ctaExchangeResponse).and {
                confirmatoryDayLimit.isNull()
                requiresConfirmatoryTest.isFalse()
                shouldOfferFollowUpTest.isFalse()
            }
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

        expectThat(tokenCheckResponse)
            .get(VirologyTokenStatusResponse::tokenStatus)
            .isEqualTo("consumable")
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

        expectThat(tokenCheckResponse)
            .get(VirologyTokenStatusResponse::tokenStatus)
            .isEqualTo("consumable")
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

        expectThat(tokenCheckResponse)
            .get(VirologyTokenStatusResponse::tokenStatus)
            .isEqualTo("other")
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

        expectThat(tokenCheckResponse)
            .get(VirologyTokenStatusResponse::tokenStatus)
            .isEqualTo("other")
    }
}
