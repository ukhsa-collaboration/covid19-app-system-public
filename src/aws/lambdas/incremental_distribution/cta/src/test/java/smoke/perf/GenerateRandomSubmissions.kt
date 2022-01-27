package smoke.perf

import org.http4k.client.JavaHttpClient
import org.http4k.filter.debug
import smoke.actors.ApiVersion.V2
import smoke.actors.MobileApp
import smoke.actors.TestLab
import smoke.data.DiagnosisKeyData.generateDiagnosisKeyData
import smoke.env.EnvConfig
import smoke.env.SmokeTests
import uk.nhs.nhsx.domain.TestKit.LAB_RESULT
import uk.nhs.nhsx.domain.TestResult.Positive
import uk.nhs.nhsx.virology.VirologyUploadHandler.VirologyResultSource.Npex
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class GenerateRandomSubmissions(maxThreads: Int, config: EnvConfig) {
    private val pool = Executors.newFixedThreadPool(maxThreads)
    private val client = JavaHttpClient().debug()
    private val mobileApp = MobileApp(client, config)
    private val testLab = TestLab(client, config)

    fun generate(numberOfSubmissions: Int) {
        repeat(numberOfSubmissions) {
            pool.submit {
                val orderResponse = mobileApp.orderTest(V2)
                val ctaToken = orderResponse.tokenParameterValue
                val diagnosisKeySubmissionToken = orderResponse.diagnosisKeySubmissionToken

                testLab.uploadTestResult(
                    token = ctaToken,
                    result = Positive,
                    source = Npex,
                    apiVersion = V2,
                    testKit = LAB_RESULT
                )

                mobileApp.submitKeys(
                    diagnosisKeySubmissionToken = diagnosisKeySubmissionToken,
                    encodedSubmissionKeys = generateDiagnosisKeyData(Random.nextInt(10))
                )
            }
        }

        pool.shutdown()
        pool.awaitTermination(1, TimeUnit.DAYS)
    }
}

fun main() {
    val config = SmokeTests.loadConfig("../../../../out/gen/config/test_config_ci.json")
    GenerateRandomSubmissions(60, config).generate(100)
}
