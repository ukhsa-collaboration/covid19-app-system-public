package smoke.actors

import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.then
import smoke.actors.ApiVersion.V1
import smoke.actors.ApiVersion.V2
import smoke.env.EnvConfig
import uk.nhs.nhsx.virology.CtaToken
import uk.nhs.nhsx.virology.TestKit
import uk.nhs.nhsx.virology.VirologyUploadHandler.VirologyResultSource
import uk.nhs.nhsx.virology.VirologyUploadHandler.VirologyResultSource.Fiorano
import uk.nhs.nhsx.virology.VirologyUploadHandler.VirologyResultSource.Npex
import uk.nhs.nhsx.virology.VirologyUploadHandler.VirologyTokenExchangeSource
import uk.nhs.nhsx.virology.VirologyUploadHandler.VirologyTokenExchangeSource.Eng
import uk.nhs.nhsx.virology.VirologyUploadHandler.VirologyTokenExchangeSource.Wls
import uk.nhs.nhsx.virology.result.TestEndDate
import uk.nhs.nhsx.virology.result.TestResult
import uk.nhs.nhsx.virology.result.VirologyTokenGenResponse
import java.time.Instant

class TestLab(unauthedHttp: HttpHandler,
              private val envConfig: EnvConfig) {

    private val authedHttp = SetAuthHeader(envConfig.authHeaders.testResultUpload).then(unauthedHttp)

    fun generateCtaTokenFor(testResult: TestResult,
                            testEndDate: TestEndDate,
                            source: VirologyTokenExchangeSource,
                            apiVersion: ApiVersion,
                            testKit: TestKit): CtaToken {
        val payload = when (apiVersion) {
            V1 -> """
                    {
                      "testEndDate": "${TestEndDate.show(testEndDate)}",
                      "testResult": "$testResult"         
                    }
            """
            V2 -> """
                    {
                      "testEndDate": "${TestEndDate.show(testEndDate)}",
                      "testResult": "$testResult",
                      "testKit": "${testKit.name}"  
                    }"""
        }
        return generateCtaTokenFor2(source, payload, apiVersion)
    }

    private fun generateCtaTokenFor2(source: VirologyTokenExchangeSource,
                                     payload: String,
                                     version: ApiVersion): CtaToken {
        val uri = mapOf(
            Pair(Eng, V1) to envConfig.engTokenGenUploadEndpoint,
            Pair(Wls, V1) to envConfig.wlsTokenGenUploadEndpoint,
            Pair(Eng, V2) to envConfig.v2EngTokenGenUploadEndpoint,
            Pair(Wls, V2) to envConfig.v2WlsTokenGenUploadEndpoint
        )

        return authedHttp(uri[Pair(source, version)]?.let {
            Request(POST, it)
                .header("Content-Type", APPLICATION_JSON.value)
                .body(payload)
        } ?: error("no uri available!"))
            .requireStatusCode(Status.OK)
            .deserializeOrThrow<VirologyTokenGenResponse>().ctaToken
    }

    fun uploadTestResult(token: CtaToken,
                         result: TestResult,
                         source: VirologyResultSource,
                         apiVersion: ApiVersion,
                         testKit: TestKit) {

        val payload = virologyUploadPayloadFrom(token, result, apiVersion, testKit)

        sendVirologyResults(payload, source, apiVersion)
            .requireStatusCode(Status.ACCEPTED)
            .requireBodyText("successfully processed")
    }

    private fun virologyUploadPayloadFrom(token: CtaToken,
                                          result: TestResult,
                                          apiVersion: ApiVersion,
                                          testKit: TestKit) = when (apiVersion) {
        V1 -> """
            {
              "ctaToken": "${token.value}",
              "testEndDate": "2020-04-23T00:00:00Z",
              "testResult": "${result.wireValue}"
            }
        """
        V2 -> """
            {
              "ctaToken": "${token.value}",
              "testEndDate": "2020-04-23T00:00:00Z",
              "testKit": "${testKit.name}",
              "testResult": "${result.wireValue}"
            }
        """
    }

    fun uploadTestResultExpectingConflict(token: CtaToken,
                                          result: TestResult,
                                          source: VirologyResultSource,
                                          apiVersion: ApiVersion,
                                          testKit: TestKit) {
        val payload = virologyUploadPayloadFrom(token, result, apiVersion, testKit)

        sendVirologyResults(payload, source, apiVersion)
            .requireStatusCode(Status.CONFLICT)
            .requireNoPayload()
    }

    fun uploadTestResultWithUnprocessableEntityV2(token: CtaToken,
                                                  result: TestResult,
                                                  testKit: TestKit,
                                                  source: VirologyResultSource) {
        val payload = """
                    {
                      "ctaToken": "${token.value}",
                      "testEndDate": "2020-04-23T00:00:00Z",
                      "testKit": "${testKit.name}",
                      "testResult": "${result.wireValue}"
                    }
                """
        sendVirologyResults(payload, source, V2)
            .requireStatusCode(Status.UNPROCESSABLE_ENTITY)
            .requireNoPayload()
    }

    private fun sendVirologyResults(payload: String,
                                    source: VirologyResultSource,
                                    version: ApiVersion): Response {
        val uri = mapOf(
            Pair(Npex, V1) to envConfig.testResultsNpexUploadEndpoint,
            Pair(Fiorano, V1) to envConfig.testResultsFioranoUploadEndpoint,
            Pair(Npex, V2) to envConfig.v2NpexUploadEndpoint,
            Pair(Fiorano, V2) to envConfig.v2FioranoUploadEndpoint,
        )

        return authedHttp(uri[Pair(source, version)]?.let {
            Request(POST, it)
                .header("Content-Type", APPLICATION_JSON.value)
                .body(payload)
        } ?: error("no uri available!"))
    }

}
