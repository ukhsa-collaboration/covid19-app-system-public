package smoke

import batchZipCreation.Exposure
import batchZipCreation.Exposure.TemporaryExposureKeyExport
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasElement
import com.natpryce.hamkrest.isNullOrEmptyString
import org.http4k.asString
import org.http4k.client.Java8HttpClient
import org.http4k.core.Status
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import smoke.actors.ApiVersion
import smoke.actors.ApiVersion.V1
import smoke.actors.ApiVersion.V2
import smoke.actors.BackgroundActivities
import smoke.actors.MobileApp
import smoke.actors.Synthetics
import smoke.actors.TestLab
import smoke.clients.AwsLambda
import smoke.data.DiagnosisKeyData.generateDiagnosisKeyData
import smoke.env.EnvConfig
import smoke.env.SmokeTests
import uk.nhs.nhsx.diagnosiskeyssubmission.model.ClientTemporaryExposureKey
import uk.nhs.nhsx.diagnosiskeyssubmission.model.ClientTemporaryExposureKeysPayload
import uk.nhs.nhsx.domain.DiagnosisKeySubmissionToken
import uk.nhs.nhsx.domain.TestKit
import uk.nhs.nhsx.domain.TestKit.LAB_RESULT
import uk.nhs.nhsx.domain.TestKit.RAPID_RESULT
import uk.nhs.nhsx.virology.VirologyUploadHandler.VirologyResultSource.Npex
import uk.nhs.nhsx.virology.VirologyUploadHandler.VirologyTokenExchangeSource.Eng
import uk.nhs.nhsx.virology.exchange.CtaExchangeResult
import uk.nhs.nhsx.virology.lookup.VirologyLookupResult.Available
import uk.nhs.nhsx.virology.lookup.VirologyLookupResult.AvailableV2
import uk.nhs.nhsx.domain.TestEndDate
import uk.nhs.nhsx.domain.TestResult
import uk.nhs.nhsx.domain.TestResult.Negative
import uk.nhs.nhsx.domain.TestResult.Positive
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Base64
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * Orders test, sends test result, polls result,
 * submits diagnosis keys, gets batch zip and
 * verifies if received batch contains the uploaded keys
 */
class TempExpKeysBatchSmokeTest {

    private val numberOfRuns = 20

    companion object {
        private val config = SmokeTests.loadConfig()

        @BeforeAll
        @JvmStatic
        fun `before all tests run check if endpoints are healthy`() {
            val config = SmokeTests.loadConfig()
            val client = Java8HttpClient()
            val synthetics = Synthetics(client, config)
            enableBatchProcessingOutsideTimeWindow()
            require(synthetics.checkEnCircuitBreakerHealth() == Status.OK)
            require(synthetics.checkDiagnosisKeysSubmissionHealth() == Status.OK)
            require(synthetics.checkTestResultsUploadHealth() == Status.OK)
            require(synthetics.checkVirologyKitHealth() == Status.OK)
        }

        private fun enableBatchProcessingOutsideTimeWindow() {
            val envVarName = "ABORT_OUTSIDE_TIME_WINDOW"
            val envVarValue = "false"
            val result = AwsLambda.updateLambdaEnvVar(
                config.diagnosis_keys_processing_function,
                envVarName to envVarValue
            )
            val updatedEnvVar = result.environment().variables()[envVarName]
            if (updatedEnvVar != envVarValue)
                throw IllegalStateException("Expected env var: $envVarName to be updated but it was not.")
        }
    }

    @Test
    fun `batch processing using virology V1 api (latest 2 hour window)`() {
        val scenario = TempExpKeysScenario(config)
        assumeTrue(scenario.isInsideProcessingWindow())

        val diagnosisKeySubmissionToken = scenario.virologyOrderAndUploadResult(Positive, V1, LAB_RESULT)
        val encodedSubmissionKeys = scenario.submitKeys(diagnosisKeySubmissionToken)

        val tekExport = scenario.invokeBatchProcessingAndGetLatestTekExport()
        scenario.checkTekExportContentsContains(encodedSubmissionKeys, tekExport)
    }

    @ParameterizedTest
    @EnumSource(TestKit::class)
    fun `batch processing using virology V2 api (latest 2 hour window)`(testKit: TestKit) {
        val scenario = TempExpKeysScenario(config)
        assumeTrue(scenario.isInsideProcessingWindow())

        val diagnosisKeySubmissionToken = scenario.virologyOrderAndUploadResult(Positive, V2, testKit)
        val encodedSubmissionKeys = scenario.submitKeys(diagnosisKeySubmissionToken)

        val tekExport = scenario.invokeBatchProcessingAndGetLatestTekExport()
        scenario.checkTekExportContentsContains(encodedSubmissionKeys, tekExport)
    }

    @Test
    fun `negative virology result with batch processing using virology V1 (latest 2 hour window)`() {
        val scenario = TempExpKeysScenario(config)
        assumeTrue(scenario.isInsideProcessingWindow())

        val diagnosisKeySubmissionToken = scenario.virologyOrderAndUploadResult(Negative, V1, LAB_RESULT)
        val encodedSubmissionKeys = scenario.submitKeys(diagnosisKeySubmissionToken)

        val tekExport = scenario.invokeBatchProcessingAndGetLatestTekExport()
        scenario.checkTekExportContentsDoesNotContain(encodedSubmissionKeys, tekExport)
    }

    @Test
    fun `negative virology result with batch processing using virology V2 (latest 2 hour window)`() {
        val scenario = TempExpKeysScenario(config)
        assumeTrue(scenario.isInsideProcessingWindow())

        val diagnosisKeySubmissionToken = scenario.virologyOrderAndUploadResult(Negative, V2, LAB_RESULT)
        val encodedSubmissionKeys = scenario.submitKeys(diagnosisKeySubmissionToken)

        val tekExport = scenario.invokeBatchProcessingAndGetLatestTekExport()
        scenario.checkTekExportContentsDoesNotContain(encodedSubmissionKeys, tekExport)
    }

    @Test
    fun `batch processing via token exchange using virology V1 (latest 2 hour window)`() {
        val scenario = TempExpKeysScenario(config)
        assumeTrue(scenario.isInsideProcessingWindow())

        val encodedSubmissionKeys = scenario.tokenGenTestAndSubmitKeys(Positive, V1, LAB_RESULT)

        val tekExport = scenario.invokeBatchProcessingAndGetLatestTekExport()
        scenario.checkTekExportContentsContains(encodedSubmissionKeys, tekExport)
    }

    @ParameterizedTest
    @EnumSource(TestKit::class)
    fun `batch processing via token exchange using virology V2 (latest 2 hour window)`(testKit: TestKit) {
        val scenario = TempExpKeysScenario(config)
        assumeTrue(scenario.isInsideProcessingWindow())

        val encodedSubmissionKeys = scenario.tokenGenTestAndSubmitKeys(Positive, V2, testKit)

        val tekExport = scenario.invokeBatchProcessingAndGetLatestTekExport()
        scenario.checkTekExportContentsContains(encodedSubmissionKeys, tekExport)
    }

    @Test
    fun `negative virology result with via token exchange using virology V1 (latest 2 hour window)`() {
        val scenario = TempExpKeysScenario(config)
        assumeTrue(scenario.isInsideProcessingWindow())

        val encodedSubmissionKeys = scenario.tokenGenTestAndSubmitKeys(Negative, V1, LAB_RESULT)

        val tekExport = scenario.invokeBatchProcessingAndGetLatestTekExport()
        scenario.checkTekExportContentsDoesNotContain(encodedSubmissionKeys, tekExport)
    }

    @Test
    fun `negative virology result with batch processing via token exchange using virology V2  (latest 2 hour window)`() {
        val scenario = TempExpKeysScenario(config)
        assumeTrue(scenario.isInsideProcessingWindow())

        val encodedSubmissionKeys = scenario.tokenGenTestAndSubmitKeys(Negative, V2, LAB_RESULT)

        val tekExport = scenario.invokeBatchProcessingAndGetLatestTekExport()
        scenario.checkTekExportContentsDoesNotContain(encodedSubmissionKeys, tekExport)
    }

    @Test
    fun `batch processing filtering out invalid keys (latest 2 hour window)`() {
        val scenario = TempExpKeysScenario(config)
        assumeTrue(scenario.isInsideProcessingWindow())

        // generate some valid keys and add 1 invalid key
        val validKeys = generateDiagnosisKeyData(Random.nextInt(5))
        val invalidKeys = listOf("invalid-key")
        val allKeys = listOf(validKeys, invalidKeys).flatten()

        val diagnosisKeySubmissionToken = scenario.virologyOrderAndUploadResult(Positive, V1, LAB_RESULT)
        scenario.submitKeys(diagnosisKeySubmissionToken, allKeys)

        val tekExport = scenario.invokeBatchProcessingAndGetLatestTekExport()
        scenario.checkTekExportContentsContains(validKeys, tekExport)
        scenario.checkTekExportContentsDoesNotContain(invalidKeys, tekExport)
    }

    @Test
    fun `bulk submission batch processing (latest 2 hour window)`() {
        val scenario = TempExpKeysScenario(config)
        assumeTrue(scenario.isInsideProcessingWindow())

        val executor = Executors.newFixedThreadPool(numberOfRuns)

        val encodedSubmissionKeys =
            (0..numberOfRuns)
                .map {
                    executor.submit(Callable {
                        val scenarioX = TempExpKeysScenario(config)
                        scenarioX.submitKeys(scenarioX.virologyOrderAndUploadResult(Positive, V1, LAB_RESULT))
                    })
                }
                .map { it.get(3, TimeUnit.MINUTES) }
                .flatten()

        val tekExport = scenario.invokeBatchProcessingAndGetLatestTekExport()
        scenario.checkTekExportContentsContains(encodedSubmissionKeys, tekExport)
    }

    @Test
    fun `single submission batch processing with onset of symptoms date`() {
        val scenario = TempExpKeysScenario(config)
        assumeTrue(scenario.isInsideProcessingWindow())

        val diagnosisKeySubmissionToken = scenario.virologyOrderAndUploadResult(Positive, V1, LAB_RESULT)
        val payload = scenario.submitKeysWithOnsetDays(diagnosisKeySubmissionToken)

        val tekExport = scenario.invokeBatchProcessingAndGetLatestTekExport()
        scenario.checkTekExportContentsContainsOnsetDays(payload, tekExport)
    }

    @Test
    fun `gets all yesterdays two hourly exports and decodes contents`() {
        val scenario = TempExpKeysScenario(config)
        val exports = scenario.getAllYesterdaysTwoHourlyExports()

        exports
            .flatMap { it.keysList }
            .forEach {
                assertThat(it.keyData.toString(), !isNullOrEmptyString)
                assertThat(it.rollingPeriod, equalTo(144))
            }
    }

    @Test
    fun `gets all daily exports and decodes contents`() {
        val scenario = TempExpKeysScenario(config)
        val exports = scenario.getAllDailyExports()

        exports
            .flatMap { it.keysList }
            .forEach {
                assertThat(it.keyData.toString(), !isNullOrEmptyString)
                assertThat(it.rollingPeriod, equalTo(144))
            }
    }

    @Test
    fun `order LAB_RESULT and RAPID_TEST and verify both exposure keys are present in tekExport`() {
        val scenario = TempExpKeysScenario(config)
        assumeTrue(scenario.isInsideProcessingWindow())

        val (keys, tekExport) = scenario.orderLABandRAPIDTestAndGetLatestTekExport()
        scenario.checkTekExportContentsContains(keys, tekExport)
    }

    private class TempExpKeysScenario(config: EnvConfig) {
        private val client = Java8HttpClient()
        private val mobileApp = MobileApp(client, config)
        private val testLab = TestLab(client, config)
        private val backgroundActivities = BackgroundActivities(config)
        private val maxKeysPerRun = 10

        fun tokenGenTestAndSubmitKeys(testResult: TestResult, apiVersion: ApiVersion, testKit: TestKit): List<String> {
            val encodedSubmissionKeys = generateDiagnosisKeyData(Random.nextInt(maxKeysPerRun))

            val ctaToken = testLab.generateCtaTokenFor(testResult, TestEndDate.of(2020, 4, 23), Eng, apiVersion, testKit)

            testLab.uploadTestResultExpectingConflict(ctaToken, testResult, Npex, apiVersion, testKit)

            val exchangeResponseSubmissionToken = when (apiVersion) {
                V1 -> {
                    val ctaExchangeResponse = (mobileApp.exchange(ctaToken, apiVersion) as CtaExchangeResult.Available).ctaExchangeResponse
                    ctaExchangeResponse.diagnosisKeySubmissionToken
                }
                V2 -> {
                    val ctaExchangeResponse = (mobileApp.exchange(ctaToken, apiVersion) as CtaExchangeResult.AvailableV2).ctaExchangeResponse
                    ctaExchangeResponse.diagnosisKeySubmissionToken
                }
            }

            val diagnosisKeySubmissionToken = exchangeResponseSubmissionToken

            mobileApp.exposureCircuitBreaker.requestAndApproveCircuitBreak()

            mobileApp.submitKeys(diagnosisKeySubmissionToken, encodedSubmissionKeys)
            return encodedSubmissionKeys
        }

        fun virologyOrderAndUploadResult(testResult: TestResult,
                                         apiVersion: ApiVersion,
                                         testKit: TestKit
        ): DiagnosisKeySubmissionToken {
            val orderResponse = mobileApp.orderTest(apiVersion)
            val ctaToken = orderResponse.tokenParameterValue
            val pollingToken = orderResponse.testResultPollingToken

            mobileApp.pollForIncompleteTestResult(orderResponse, apiVersion)
            testLab.uploadTestResult(ctaToken, testResult, Npex, apiVersion, testKit)
            testLab.uploadTestResultExpectingConflict(ctaToken, testResult, Npex, apiVersion, testKit)

            when (apiVersion) {
                V1 -> {
                    val pollResult = mobileApp.pollForTestResult(pollingToken, apiVersion) as Available
                    assertThat(pollResult.response.testResult, equalTo(testResult))
                }
                V2 -> {
                    val pollResultV2 = mobileApp.pollForTestResult(pollingToken, apiVersion) as AvailableV2
                    assertThat(pollResultV2.response.testResult, equalTo(testResult))
                }
            }

            return orderResponse.diagnosisKeySubmissionToken
        }

        fun submitKeys(diagnosisKeySubmissionToken: DiagnosisKeySubmissionToken,
                       encodedSubmissionKeys: List<String> = generateDiagnosisKeyData(Random.nextInt(maxKeysPerRun))): List<String> {
            mobileApp.submitKeys(diagnosisKeySubmissionToken, encodedSubmissionKeys)
            return encodedSubmissionKeys
        }

        fun submitKeysWithOnsetDays(diagnosisKeySubmissionToken: DiagnosisKeySubmissionToken): ClientTemporaryExposureKeysPayload {
            return mobileApp.submitKeysWithOnsetDays(diagnosisKeySubmissionToken, generateDiagnosisKeyData(2))
        }

        fun invokeBatchProcessingAndGetLatestTekExport(): TemporaryExposureKeyExport {
            backgroundActivities.invokesBatchProcessing()
            return mobileApp.getLatestTwoHourlyTekExport()
        }

        fun orderLABandRAPIDTestAndGetLatestTekExport(): Pair<List<String>, TemporaryExposureKeyExport> {
            val firstKeys = mobileApp.orderTest(V2).run {
                testLab.uploadTestResult(tokenParameterValue, Positive, Npex, V2, LAB_RESULT)
                submitKeys(diagnosisKeySubmissionToken)
            }

            val secondKeys = mobileApp.orderTest(V2).run {
                testLab.uploadTestResult(tokenParameterValue, Positive, Npex, V2, RAPID_RESULT)
                submitKeys(diagnosisKeySubmissionToken)
            }

            val export = invokeBatchProcessingAndGetLatestTekExport()

            return Pair(firstKeys + secondKeys, export)
        }

        fun isInsideProcessingWindow(): Boolean {
            val dateTime = LocalDateTime.ofInstant(Instant.now(), ZoneId.of("UTC"))
            if (dateTime.hour % 2 != 0 && dateTime.minute >= 45) {
                return false
            }
            return true
        }

        fun checkTekExportContentsContains(expectedKeys: List<String>,
                                           tekExport: TemporaryExposureKeyExport) {
            val receivedEncodedKeys = tekExport.keysList
                .map { Base64.getEncoder().encode(it.keyData.asReadOnlyByteBuffer()).asString() }

            expectedKeys.forEach {
                assertThat(receivedEncodedKeys, hasElement(it))
            }
        }

        fun checkTekExportContentsDoesNotContain(expectedKeys: List<String>,
                                                 tekExport: TemporaryExposureKeyExport) {
            val receivedEncodedKeys = tekExport.keysList
                .map { Base64.getEncoder().encode(it.keyData.asReadOnlyByteBuffer()).asString() }

            expectedKeys.forEach {
                assertThat(receivedEncodedKeys, !hasElement(it))
            }
        }

        fun checkTekExportContentsContainsOnsetDays(submissionPayload: ClientTemporaryExposureKeysPayload,
                                                    tekExport: TemporaryExposureKeyExport) {
            val receivedEncodedKeys = tekExport.keysList
            checkTekExportKeysMatchSubmissionPayloadKeys(submissionPayload.temporaryExposureKeys, receivedEncodedKeys)
        }

        fun checkTekExportKeysMatchSubmissionPayloadKeys(submissionKeys: List<ClientTemporaryExposureKey?>, tekExportKeys: List<Exposure.TemporaryExposureKey>) {
            val convertedTekKeys = tekExportKeys.map { convertTekKeyToClientKey(it) }
            submissionKeys.filterNotNull().forEach {
                val tekExportKey: ClientTemporaryExposureKey? = convertedTekKeys.find { k -> it.key == k.key }
                if (null != it.daysSinceOnsetOfSymptoms) {
                    assertThat(it.daysSinceOnsetOfSymptoms, equalTo(tekExportKey?.daysSinceOnsetOfSymptoms))
                } else {
                    assertThat(tekExportKey?.daysSinceOnsetOfSymptoms, equalTo(0))
                }
            }
        }

        fun convertTekKeyToClientKey(tek: Exposure.TemporaryExposureKey): ClientTemporaryExposureKey {
            val key = ClientTemporaryExposureKey(
                Base64.getEncoder().encode(tek.keyData.asReadOnlyByteBuffer()).asString(),
                tek.rollingStartIntervalNumber,
                tek.rollingPeriod
            )
            if (tek.hasDaysSinceOnsetOfSymptoms()) {
                key.daysSinceOnsetOfSymptoms = tek.daysSinceOnsetOfSymptoms
            }
            return key
        }


        fun getAllYesterdaysTwoHourlyExports(): List<TemporaryExposureKeyExport> {
            val yesterdayMidnight = LocalDateTime
                .ofInstant(Instant.now(), ZoneId.of("UTC"))
                .minusDays(1)
                .withHour(0)

            return (0..12)
                .map { yesterdayMidnight.minusHours((2 * it).toLong()) }
                .map { mobileApp.getTwoHourlyTekExport(it) }
        }

        fun getAllDailyExports(): List<TemporaryExposureKeyExport> {
            val tomorrowMidnight = LocalDateTime
                .ofInstant(Instant.now(), ZoneId.of("UTC"))
                .plusDays(1)
                .withHour(0)

            return (0..14)
                .map { tomorrowMidnight.minusDays(it.toLong()).toLocalDate() }
                .map { mobileApp.getDailyTekExport(it) }
        }
    }

}
