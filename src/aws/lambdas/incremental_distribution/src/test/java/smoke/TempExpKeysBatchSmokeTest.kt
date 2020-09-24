package smoke

import batchZipCreation.Exposure
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasElement
import com.natpryce.hamkrest.isNullOrEmptyString
import junit.framework.Assert.assertEquals
import org.apache.logging.log4j.LogManager
import org.http4k.asString
import org.http4k.client.JavaHttpClient
import org.junit.Test
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

    private val logger = LogManager.getLogger(TempExpKeysBatchSmokeTest::class.java)
    private val config = SmokeTests.loadConfig()
    private val numberOfRuns = 50

    @Test
    fun `single submission batch processing (latest 2 hour window)`() {
        val scenario = TempExpKeysScenario(config)

        val virologyTestResponse = scenario.orderTestUploadResult("POSITIVE")
        val encodedSubmissionKeys = scenario.submitKeys(virologyTestResponse)

        val tekExport = scenario.invokeBatchProcessingAndGetTekExport()
        scenario.checkTekExportContentsContains(encodedSubmissionKeys, tekExport)
    }

    @Test
    fun `negative virology result with single submission batch processing (latest 2 hour window)`() {
        val scenario = TempExpKeysScenario(config)

        val virologyTestResponse = scenario.orderTestUploadResult("NEGATIVE")
        val encodedSubmissionKeys = scenario.submitKeys(virologyTestResponse)

        val tekExport = scenario.invokeBatchProcessingAndGetTekExport()
        scenario.checkTekExportContentsDoesNotContain(encodedSubmissionKeys, tekExport)
    }

    @Test
    fun `single submission batch processing via token exchange (latest 2 hour window)`() {
        val scenario = TempExpKeysScenario(config)

        val encodedSubmissionKeys = scenario.tokenGenTestAndSubmitKeys("POSITIVE")

        val tekExport = scenario.invokeBatchProcessingAndGetTekExport()
        scenario.checkTekExportContentsContains(encodedSubmissionKeys, tekExport)
    }

    @Test
    fun `negative virology result with single submission batch processing via token exchange (latest 2 hour window)`() {
        val scenario = TempExpKeysScenario(config)

        val encodedSubmissionKeys = scenario.tokenGenTestAndSubmitKeys("NEGATIVE")

        val tekExport = scenario.invokeBatchProcessingAndGetTekExport()
        scenario.checkTekExportContentsDoesNotContain(encodedSubmissionKeys, tekExport)
    }

    @Test
    fun `bulk submission batch processing (latest 2 hour window)`() {
        val executor = Executors.newFixedThreadPool(50)

        val encodedSubmissionKeys =
            (0..numberOfRuns)
                .map {
                    executor.submit(Callable {
                        val scenario = TempExpKeysScenario(config)
                        val virologyTestResponse = scenario.orderTestUploadResult("POSITIVE")
                        scenario.submitKeys(virologyTestResponse)
                    })
                }
                .map { it.get(3, TimeUnit.MINUTES) }
                .flatten()

        logger.info("submitted ${encodedSubmissionKeys.size} key(s)")

        val scenario = TempExpKeysScenario(config)
        val tekExport = scenario.invokeBatchProcessingAndGetTekExport()
        scenario.checkTekExportContentsContains(encodedSubmissionKeys, tekExport)
    }

    @Test
    fun `single submission batch processing with onset of symptoms date`() {
        val scenario = TempExpKeysScenario(config)

        val virologyTestResponse = scenario.orderTestUploadResult("POSITIVE")
        val payload = scenario.submitKeysWithOnsetDays(virologyTestResponse)

        val tekExport = scenario.invokeBatchProcessingAndGetTekExport()
        scenario.checkTekExportContentsContainsOnsetDays(payload, tekExport)
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

            val tokenResponse = enCircuitBreakerClient.requestCircuitBreaker()
            assertThat(tokenResponse.approval, equalTo("yes"))
            assertThat(tokenResponse.approvalToken, !isNullOrEmptyString)

            val payload = diagnosisKeysSubmissionClient.createKeysPayload(exchangeResponse.diagnosisKeySubmissionToken, encodedSubmissionKeys)
            diagnosisKeysSubmissionClient.sendTempExposureKeys(payload)
            return encodedSubmissionKeys
        }

        fun orderTestUploadResult(testResult: String): VirologyOrderResponse {
            val virologyOrderResponse = virologyClient.orderTest()

            virologyClient.checkTestResultNotAvailableYet(virologyOrderResponse.testResultPollingToken)

            virologyClient.uploadTestResult(virologyOrderResponse, testResult)
            virologyClient.uploadTestResultWithCheckingConflict(virologyOrderResponse.tokenParameterValue, testResult)

            val retrieveTestResult = virologyClient.retrieveTestResult(virologyOrderResponse.testResultPollingToken)
            assertThat(retrieveTestResult.testResult, equalTo(testResult))

            val tokenResponse = enCircuitBreakerClient.requestCircuitBreaker()
            assertThat(tokenResponse.approval, equalTo("yes"))
            assertThat(tokenResponse.approvalToken, !isNullOrEmptyString)

            return virologyOrderResponse
        }

        fun submitKeys(virologyOrderResponse: VirologyOrderResponse): List<String> {
            val encodedSubmissionKeys = generateKeyData(Random.nextInt(maxKeysPerRun))
            val payload = diagnosisKeysSubmissionClient.createKeysPayload(virologyOrderResponse.diagnosisKeySubmissionToken, encodedSubmissionKeys)
            diagnosisKeysSubmissionClient.sendTempExposureKeys(payload)
            return encodedSubmissionKeys
        }

        fun submitKeysWithOnsetDays(virologyOrderResponse: VirologyOrderResponse): ClientTemporaryExposureKeysPayload {
            val encodedSubmissionKeys = generateKeyData(2)
            val payload = diagnosisKeysSubmissionClient.createKeysPayloadWithOnsetDays(virologyOrderResponse.diagnosisKeySubmissionToken, encodedSubmissionKeys)
            diagnosisKeysSubmissionClient.sendTempExposureKeys(payload)
            return payload
        }

        fun invokeBatchProcessingAndGetTekExport(): Exposure.TemporaryExposureKeyExport {
            batchProcessingClient.invokeBatchProcessing()
            return exportBatchClient.getLatestTwoHourlyTekExport()
        }

        fun isOutsideProcessingWindow(): Boolean {
            val dateTime = LocalDateTime.ofInstant(Instant.now(), ZoneId.of("UTC"))
            if (dateTime.hour % 2 != 0 && dateTime.minute >= 45) {
                return true
            }
            return false
        }

        fun checkTekExportContentsContains(expectedKeys: List<String>,
                                           tekExport: Exposure.TemporaryExposureKeyExport) {
            if (isOutsideProcessingWindow()) {
                logger.info("skipping non processable batch window")
                return
            }

            val receivedEncodedKeys = tekExport.keysList
                .map { Base64.getEncoder().encode(it.keyData.asReadOnlyByteBuffer()).asString() }

            expectedKeys.forEach {
                logger.info("verifying key exists: $it")
                assertThat(receivedEncodedKeys, hasElement(it))
            }
        }

        fun checkTekExportContentsDoesNotContain(expectedKeys: List<String>,
                                                 tekExport: Exposure.TemporaryExposureKeyExport) {
            if (isOutsideProcessingWindow()) {
                logger.info("skipping non processable batch window")
                return
            }

            val receivedEncodedKeys = tekExport.keysList
                .map { Base64.getEncoder().encode(it.keyData.asReadOnlyByteBuffer()).asString() }

            expectedKeys.forEach {
                logger.info("verifying key does not exist: $it")
                assertThat(receivedEncodedKeys, !hasElement(it))
            }
        }

        fun checkTekExportContentsContainsOnsetDays(submissionPayload: ClientTemporaryExposureKeysPayload,
                                                    tekExport: Exposure.TemporaryExposureKeyExport) {
            if (isOutsideProcessingWindow()) {
                logger.info("skipping non processable batch window")
                return
            }

            val receivedEncodedKeys = tekExport.keysList
            checkTekExportKeysMatchSubmissionPayloadKeys(submissionPayload.temporaryExposureKeys, receivedEncodedKeys)
        }

        fun checkTekExportKeysMatchSubmissionPayloadKeys(submissionKeys: List<ClientTemporaryExposureKey>, tekExportKeys: List<Exposure.TemporaryExposureKey>){

            val convertedTekKeys: List<ClientTemporaryExposureKey> = tekExportKeys.map { it -> convertTekKeyToClientKey(it) }
            submissionKeys.forEach {
                val tekExportKey: ClientTemporaryExposureKey? = convertedTekKeys.find { k -> it.key == k.key }
                assertEquals(it.daysSinceOnsetOfSymptoms, tekExportKey?.daysSinceOnsetOfSymptoms)
            }
        }

        fun convertTekKeyToClientKey(tek: Exposure.TemporaryExposureKey) : ClientTemporaryExposureKey {
            val key = ClientTemporaryExposureKey(Base64.getEncoder().encode(tek.keyData.asReadOnlyByteBuffer()).asString(), tek.rollingStartIntervalNumber, tek.rollingPeriod)
            if(tek.hasDaysSinceOnsetOfSymptoms()) {
                key.setDaysSinceOnsetOfSymptoms(tek.daysSinceOnsetOfSymptoms)
            }
            return key
        }

        private fun generateKeyData(numKeys: Int) =
            (0..numKeys)
                .map { keyGenerator.generate() }
                .map { Base64.getEncoder().encodeToString(it.toByteArray()) }

    }

}