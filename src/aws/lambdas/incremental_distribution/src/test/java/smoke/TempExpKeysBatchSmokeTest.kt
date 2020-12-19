package smoke

import batchZipCreation.Exposure
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasElement
import com.natpryce.hamkrest.isNullOrEmptyString
import org.apache.logging.log4j.LogManager
import org.http4k.asString
import org.http4k.client.JavaHttpClient
import org.http4k.core.Status
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import smoke.clients.*
import smoke.env.EnvConfig
import smoke.env.SmokeTests
import uk.nhs.nhsx.core.random.crockford.CrockfordDammRandomStringGenerator
import uk.nhs.nhsx.diagnosiskeyssubmission.model.ClientTemporaryExposureKey
import uk.nhs.nhsx.diagnosiskeyssubmission.model.ClientTemporaryExposureKeysPayload
import uk.nhs.nhsx.virology.exchange.CtaExchangeResponse
import uk.nhs.nhsx.virology.exchange.CtaExchangeResult
import uk.nhs.nhsx.virology.order.VirologyOrderResponse
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
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

        private val logger = LogManager.getLogger(TempExpKeysBatchSmokeTest::class.java)
        private val config = SmokeTests.loadConfig()

        @BeforeAll
        @JvmStatic
        fun `before all tests run check if endpoints are healthy`() {
            val config = SmokeTests.loadConfig()
            val client = JavaHttpClient()
            val healthClient = HealthClient(client, config)
            enableBatchProcessingOutsideTimeWindow()
            healthClient.enCircuitBreakerHealthEndpoint().requireStatusCode(Status.OK)
            healthClient.diagnosisKeysSubmission().requireStatusCode(Status.OK)
            healthClient.testResultsHealthEndpoint().requireStatusCode(Status.OK)
            healthClient.virologyKitHealthEndpoint().requireStatusCode(Status.OK)
        }

        private fun enableBatchProcessingOutsideTimeWindow() {
            val envVarName = "ABORT_OUTSIDE_TIME_WINDOW"
            val envVarValue = "false"
            val result = AwsLambda.updateLambdaEnvVar(
                config.diagnosisKeysProcessingFunction,
                envVarName to envVarValue
            )
            val updatedEnvVar = result.environment.variables[envVarName]
            if (updatedEnvVar != envVarValue)
                throw IllegalStateException("Expected env var: $envVarName to be updated but it was not.")
        }
    }

    @Test
    fun `single submission batch processing (latest 2 hour window)`() {
        val scenario = TempExpKeysScenario(config)
        assumeTrue(scenario.isInsideProcessingWindow())

        val virologyTestResponse = scenario.virologyOrderAndUploadResult("POSITIVE")
        val encodedSubmissionKeys = scenario.submitKeys(virologyTestResponse)

        val tekExport = scenario.invokeBatchProcessingAndGetLatestTekExport()
        scenario.checkTekExportContentsContains(encodedSubmissionKeys, tekExport)
    }

    @Test
    fun `negative virology result with single submission batch processing (latest 2 hour window)`() {
        val scenario = TempExpKeysScenario(config)
        assumeTrue(scenario.isInsideProcessingWindow())

        val virologyTestResponse = scenario.virologyOrderAndUploadResult("NEGATIVE")
        val encodedSubmissionKeys = scenario.submitKeys(virologyTestResponse)

        val tekExport = scenario.invokeBatchProcessingAndGetLatestTekExport()
        scenario.checkTekExportContentsDoesNotContain(encodedSubmissionKeys, tekExport)
    }

    @Test
    fun `single submission batch processing via token exchange (latest 2 hour window)`() {
        val scenario = TempExpKeysScenario(config)
        assumeTrue(scenario.isInsideProcessingWindow())

        val encodedSubmissionKeys = scenario.tokenGenTestAndSubmitKeys("POSITIVE")

        val tekExport = scenario.invokeBatchProcessingAndGetLatestTekExport()
        scenario.checkTekExportContentsContains(encodedSubmissionKeys, tekExport)
    }

    @Test
    fun `negative virology result with single submission batch processing via token exchange (latest 2 hour window)`() {
        val scenario = TempExpKeysScenario(config)
        assumeTrue(scenario.isInsideProcessingWindow())

        val encodedSubmissionKeys = scenario.tokenGenTestAndSubmitKeys("NEGATIVE")

        val tekExport = scenario.invokeBatchProcessingAndGetLatestTekExport()
        scenario.checkTekExportContentsDoesNotContain(encodedSubmissionKeys, tekExport)
    }

    @Test
    fun `single submission batch processing filtering out invalid keys (latest 2 hour window)`() {
        val scenario = TempExpKeysScenario(config)
        assumeTrue(scenario.isInsideProcessingWindow())

        // generate some valid keys and add 1 invalid key
        val validKeys = scenario.generateKeyData(Random.nextInt(5))
        val invalidKeys = listOf("invalid-key")
        val allKeys = listOf(validKeys, invalidKeys).flatten()

        val virologyTestResponse = scenario.virologyOrderAndUploadResult("POSITIVE")
        scenario.submitKeys(virologyTestResponse, allKeys)

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
                        val scenario = TempExpKeysScenario(config)
                        val virologyTestResponse = scenario.virologyOrderAndUploadResult("POSITIVE")
                        scenario.submitKeys(virologyTestResponse)
                    })
                }
                .map { it.get(3, TimeUnit.MINUTES) }
                .flatten()

        logger.info("submitted ${encodedSubmissionKeys.size} key(s)")

        val tekExport = scenario.invokeBatchProcessingAndGetLatestTekExport()
        scenario.checkTekExportContentsContains(encodedSubmissionKeys, tekExport)
    }

    @Test
    fun `single submission batch processing with onset of symptoms date`() {
        val scenario = TempExpKeysScenario(config)
        assumeTrue(scenario.isInsideProcessingWindow())

        val virologyTestResponse = scenario.virologyOrderAndUploadResult("POSITIVE")
        val payload = scenario.submitKeysWithOnsetDays(virologyTestResponse)

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

    private class TempExpKeysScenario(config: EnvConfig) {
        private val logger = LogManager.getLogger(TempExpKeysScenario::class.java)

        private val keyGenerator = CrockfordDammRandomStringGenerator()
        private val client = JavaHttpClient()
        private val virologyClient = VirologyClient(client, config)
        private val diagnosisKeysSubmissionClient = DiagnosisKeysSubmissionClient(client, config)
        private val enCircuitBreakerClient = EnCircuitBreakerClient(client, config)
        private val exportBatchClient = ExportBatchClient(client, config)
        private val batchProcessingClient = BatchProcessingClient(config)
        private val maxKeysPerRun = 10

        fun tokenGenTestAndSubmitKeys(testResult: String): List<String> {
            val encodedSubmissionKeys = generateKeyData(Random.nextInt(maxKeysPerRun))

            val testLabResponse = virologyClient.ctaTokenGen(testResult)
            virologyClient.uploadTestResultWithCheckingConflict(testLabResponse.ctaToken, testResult)

            val exchangeResponse: CtaExchangeResponse = (virologyClient.exchangeCtaToken(testLabResponse) as CtaExchangeResult.Available).ctaExchangeResponse

            // circuit breaker request
            val tokenResponse = enCircuitBreakerClient.request()
            assertThat(tokenResponse.approval, equalTo("yes"))
            assertThat(tokenResponse.approvalToken, !isNullOrEmptyString)

            // circuit breaker approval
            val resolutionResponse = enCircuitBreakerClient.resolution(tokenResponse)
            assertThat(resolutionResponse.approval, equalTo("yes"))

            val payload = diagnosisKeysSubmissionClient.createKeysPayload(exchangeResponse.diagnosisKeySubmissionToken, encodedSubmissionKeys)
            diagnosisKeysSubmissionClient.sendTempExposureKeys(payload)
            return encodedSubmissionKeys
        }

        fun virologyOrderAndUploadResult(testResult: String): VirologyOrderResponse {
            val virologyOrderResponse = virologyClient.orderTest()

            virologyClient.checkTestResultNotAvailableYet(virologyOrderResponse.testResultPollingToken)

            virologyClient.uploadTestResult(virologyOrderResponse, testResult)
            virologyClient.uploadTestResultWithCheckingConflict(virologyOrderResponse.tokenParameterValue, testResult)

            val retrieveTestResult = virologyClient.retrieveTestResult(virologyOrderResponse.testResultPollingToken)
            assertThat(retrieveTestResult.testResult, equalTo(testResult))

            // circuit breaker request
            val tokenResponse = enCircuitBreakerClient.request()
            assertThat(tokenResponse.approval, equalTo("yes"))
            assertThat(tokenResponse.approvalToken, !isNullOrEmptyString)

            // circuit breaker approval
            val resolutionResponse = enCircuitBreakerClient.resolution(tokenResponse)
            assertThat(resolutionResponse.approval, equalTo("yes"))

            return virologyOrderResponse
        }

        fun submitKeys(virologyOrderResponse: VirologyOrderResponse,
                       encodedSubmissionKeys: List<String> = generateKeyData(Random.nextInt(maxKeysPerRun))): List<String> {
            val payload = diagnosisKeysSubmissionClient.createKeysPayload(
                virologyOrderResponse.diagnosisKeySubmissionToken,
                encodedSubmissionKeys
            )
            diagnosisKeysSubmissionClient.sendTempExposureKeys(payload)
            return encodedSubmissionKeys
        }

        fun submitKeysWithOnsetDays(virologyOrderResponse: VirologyOrderResponse): ClientTemporaryExposureKeysPayload {
            val encodedSubmissionKeys = generateKeyData(2)
            val payload = diagnosisKeysSubmissionClient.createKeysPayloadWithOnsetDays(virologyOrderResponse.diagnosisKeySubmissionToken, encodedSubmissionKeys)
            diagnosisKeysSubmissionClient.sendTempExposureKeys(payload)
            return payload
        }

        fun invokeBatchProcessingAndGetLatestTekExport(): Exposure.TemporaryExposureKeyExport {
            batchProcessingClient.invokeBatchProcessing()
            return exportBatchClient.getLatestTwoHourlyTekExport()
        }

        fun isInsideProcessingWindow(): Boolean {
            val dateTime = LocalDateTime.ofInstant(Instant.now(), ZoneId.of("UTC"))
            if (dateTime.hour % 2 != 0 && dateTime.minute >= 45) {
                return false
            }
            return true
        }

        fun checkTekExportContentsContains(expectedKeys: List<String>,
                                           tekExport: Exposure.TemporaryExposureKeyExport) {
            val receivedEncodedKeys = tekExport.keysList
                .map { Base64.getEncoder().encode(it.keyData.asReadOnlyByteBuffer()).asString() }

            expectedKeys.forEach {
                logger.info("verifying key exists: $it")
                assertThat(receivedEncodedKeys, hasElement(it))
            }
        }

        fun checkTekExportContentsDoesNotContain(expectedKeys: List<String>,
                                                 tekExport: Exposure.TemporaryExposureKeyExport) {
            val receivedEncodedKeys = tekExport.keysList
                .map { Base64.getEncoder().encode(it.keyData.asReadOnlyByteBuffer()).asString() }

            expectedKeys.forEach {
                logger.info("verifying key does not exist: $it")
                assertThat(receivedEncodedKeys, !hasElement(it))
            }
        }

        fun checkTekExportContentsContainsOnsetDays(submissionPayload: ClientTemporaryExposureKeysPayload,
                                                    tekExport: Exposure.TemporaryExposureKeyExport) {
            val receivedEncodedKeys = tekExport.keysList
            checkTekExportKeysMatchSubmissionPayloadKeys(submissionPayload.temporaryExposureKeys, receivedEncodedKeys)
        }

        fun checkTekExportKeysMatchSubmissionPayloadKeys(submissionKeys: List<ClientTemporaryExposureKey>, tekExportKeys: List<Exposure.TemporaryExposureKey>) {
            val convertedTekKeys = tekExportKeys.map { convertTekKeyToClientKey(it) }
            submissionKeys.forEach {
                val tekExportKey: ClientTemporaryExposureKey? = convertedTekKeys.find { k -> it.key == k.key }
                if (null != it.daysSinceOnsetOfSymptoms) {
                    assertThat(it.daysSinceOnsetOfSymptoms, equalTo(tekExportKey?.daysSinceOnsetOfSymptoms))
                } else {
                    assertThat(tekExportKey?.daysSinceOnsetOfSymptoms, equalTo(0))
                }
            }
        }

        fun convertTekKeyToClientKey(tek: Exposure.TemporaryExposureKey): ClientTemporaryExposureKey {
            val key = ClientTemporaryExposureKey(Base64.getEncoder().encode(tek.keyData.asReadOnlyByteBuffer()).asString(), tek.rollingStartIntervalNumber, tek.rollingPeriod)
            if (tek.hasDaysSinceOnsetOfSymptoms()) {
                key.setDaysSinceOnsetOfSymptoms(tek.daysSinceOnsetOfSymptoms)
            }
            return key
        }

        fun generateKeyData(numKeys: Int) =
            (0..numKeys)
                .map { keyGenerator.generate() + keyGenerator.generate() }
                .map { Base64.getEncoder().encodeToString(it.toByteArray()) }

        fun getAllYesterdaysTwoHourlyExports(): List<Exposure.TemporaryExposureKeyExport> {
            val yesterdayMidnight = LocalDateTime
                .ofInstant(Instant.now(), ZoneId.of("UTC"))
                .minusDays(1)
                .withHour(0)

            return (0..12)
                .map {
                    val twoHourlyWindowStr =
                        yesterdayMidnight.minusHours((2 * it).toLong())
                            .format(DateTimeFormatter.ofPattern("YYYYMMddHH"))
                            .toString()
                    "$twoHourlyWindowStr.zip"
                }
                .map {
                    exportBatchClient.getTwoHourlyTekExport(it)
                }
        }

        fun getAllDailyExports(): List<Exposure.TemporaryExposureKeyExport> {
            val tomorrowMidnight = LocalDateTime
                .ofInstant(Instant.now(), ZoneId.of("UTC"))
                .plusDays(1)
                .withHour(0)

            return (0..14)
                .map {
                    val dailyStr =
                        tomorrowMidnight.minusDays(it.toLong())
                            .format(DateTimeFormatter.ofPattern("YYYYMMddHH"))
                            .toString()
                    "$dailyStr.zip"
                }
                .map {
                    exportBatchClient.getDailyTekExport(it)
                }
        }
    }

}