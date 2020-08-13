package smoke

import batchZipCreation.Exposure
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasElement
import com.natpryce.hamkrest.isNullOrEmptyString
import org.http4k.asString
import org.http4k.client.JavaHttpClient
import org.junit.Test
import org.slf4j.LoggerFactory
import smoke.clients.*
import smoke.env.EnvConfig
import smoke.env.SmokeTests
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

    private val logger = LoggerFactory.getLogger(TempExpKeysBatchSmokeTest::class.java)
    private val config = SmokeTests.loadConfig()
    private val numberOfRuns = 50

    @Test
    fun `single submission batch processing (latest 2 hour window)`() {
        val tempExpKeysScenario = TempExpKeysScenario(config)

        val encodedSubmissionKeys = tempExpKeysScenario.orderTestUploadResultAndSubmitKeys()

        tempExpKeysScenario.invokeBatchProcessingAndValidateKeys(encodedSubmissionKeys)
    }

    @Test
    fun `bulk submission batch processing (latest 2 hour window)`() {
        val executor = Executors.newFixedThreadPool(50)

        val expectedKeys =
            (0..numberOfRuns)
                .map {
                    executor.submit(Callable {
                        val scenario = TempExpKeysScenario(config)
                        scenario.orderTestUploadResultAndSubmitKeys()
                    })
                }
                .map { it.get(3, TimeUnit.MINUTES) }
                .flatten()

        logger.info("submitted ${expectedKeys.size} key(s)")

        val scenario = TempExpKeysScenario(config)
        scenario.invokeBatchProcessingAndValidateKeys(expectedKeys)
    }

    private class TempExpKeysScenario(config: EnvConfig) {
        private val logger = LoggerFactory.getLogger(TempExpKeysScenario::class.java)

        private val client = JavaHttpClient()
        private val testKitOrderClient = TestKitOrderClient(client, config)
        private val testResultUploadClient = TestResultUploadClient(client, config)
        private val diagnosisKeysSubmissionClient = DiagnosisKeysSubmissionClient(client, config)
        private val enCircuitBreakerClient = EnCircuitBreakerClient(client, config)
        private val exportBatchClient = ExportBatchClient(client, config)
        private val batchProcessingClient = BatchProcessingClient(config)
        private val maxKeysPerRun = 10

        fun orderTestUploadResultAndSubmitKeys(): List<String> {
            val encodedSubmissionKeys = generateKeyData(Random.nextInt(maxKeysPerRun))

            val testOrderResponse = testKitOrderClient.orderTest()

            testResultUploadClient.uploadTestResult(testOrderResponse)

            val retrieveTestResult = testKitOrderClient.retrieveTestResult(testOrderResponse)
            assertThat(retrieveTestResult.testResult, equalTo("POSITIVE"))

            val tokenResponse = enCircuitBreakerClient.requestCircuitBreaker()
            assertThat(tokenResponse.approval, equalTo("yes"))
            assertThat(tokenResponse.approvalToken, !isNullOrEmptyString)

            diagnosisKeysSubmissionClient.sendTempExposureKeys(testOrderResponse, encodedSubmissionKeys)

            return encodedSubmissionKeys
        }

        fun invokeBatchProcessingAndValidateKeys(encodedSubmissionKeys: List<String>) {
            batchProcessingClient.invokeBatchProcessing()
            val tekExport = exportBatchClient.getLatestTwoHourlyTekExport()
            checkTekExportContents(encodedSubmissionKeys, tekExport)
        }

        private fun checkTekExportContents(expectedKeys: List<String>,
                                           tekExport: Exposure.TemporaryExposureKeyExport) {
            logger.info("checkTekExportContents")

            val dateTime = LocalDateTime.ofInstant(Instant.now(), ZoneId.of("UTC"))
            if (dateTime.hour % 2 != 0 && dateTime.minute >= 45) {
                logger.info("skipping non processable batch window")
                return
            }

            val receivedEncodedKeys = tekExport.keysList
                .map { Base64.getEncoder().encode(it.keyData.asReadOnlyByteBuffer()).asString() }

            expectedKeys.forEach {
                logger.info("verifying key: $it")
                assertThat(receivedEncodedKeys, hasElement(it))
            }
        }

        private fun generateKeyData(numKeys: Int) =
            (0..numKeys).map {
                Base64.getEncoder().encodeToString(UUID.randomUUID().toString().toByteArray())
            }
    }

}