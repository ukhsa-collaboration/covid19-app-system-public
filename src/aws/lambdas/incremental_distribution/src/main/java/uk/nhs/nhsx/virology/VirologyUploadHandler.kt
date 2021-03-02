package uk.nhs.nhsx.virology

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.HttpResponses
import uk.nhs.nhsx.core.Jackson
import uk.nhs.nhsx.core.Jackson.readOrNull
import uk.nhs.nhsx.core.SystemClock.CLOCK
import uk.nhs.nhsx.core.auth.ApiName.Health
import uk.nhs.nhsx.core.auth.ApiName.TestResultUpload
import uk.nhs.nhsx.core.auth.Authenticator
import uk.nhs.nhsx.core.auth.StandardAuthentication.awsAuthentication
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.PrintingJsonEvents
import uk.nhs.nhsx.core.events.UnprocessableJson
import uk.nhs.nhsx.core.events.VirologyResults
import uk.nhs.nhsx.core.events.VirologyTokenGen
import uk.nhs.nhsx.core.routing.Routing
import uk.nhs.nhsx.core.routing.Routing.Method.POST
import uk.nhs.nhsx.core.routing.Routing.path
import uk.nhs.nhsx.core.routing.RoutingHandler
import uk.nhs.nhsx.core.routing.StandardHandlers.authorisedBy
import uk.nhs.nhsx.core.routing.StandardHandlers.withoutSignedResponses
import uk.nhs.nhsx.virology.VirologyConfig.Companion.fromEnvironment
import uk.nhs.nhsx.virology.order.TokensGenerator
import uk.nhs.nhsx.virology.persistence.VirologyPersistenceService
import uk.nhs.nhsx.virology.result.VirologyResultRequest
import uk.nhs.nhsx.virology.result.VirologyTokenGenRequest

class VirologyUploadHandler @JvmOverloads constructor(
    environment: Environment = Environment.fromSystem(),
    private val events: Events = PrintingJsonEvents(CLOCK),
    authenticator: Authenticator = awsAuthentication(TestResultUpload, events),
    virologyService: VirologyService = virologyService(environment, events),
    healthAuthenticator: Authenticator = awsAuthentication(Health, events)
) : RoutingHandler() {

    val handler = withoutSignedResponses(
        events,
        environment,
        Routing.routes(
            authorisedBy(
                healthAuthenticator,
                path(POST, "/upload/virology-test/health") { HttpResponses.ok() }
            ),
            authorisedBy(
                authenticator,
                path(POST, "/upload/virology-test/npex-result") { r ->
                    events.emit(javaClass, VirologyResults())
                    handleV1TestResult(VirologyResultSource.Npex, virologyService, r)
                }),
            authorisedBy(
                authenticator,
                path(POST, "/upload/virology-test/fiorano-result") { r ->
                    events.emit(javaClass, VirologyResults())
                    handleV1TestResult(VirologyResultSource.Fiorano, virologyService, r)
                }),
            authorisedBy(
                authenticator,
                path(POST, "/upload/virology-test/eng-result-tokengen") { r ->
                    events.emit(javaClass, VirologyTokenGen())
                    handleV1TokenGen(VirologyTokenExchangeSource.Eng, virologyService, r)
                }),
            authorisedBy(
                authenticator,
                path(POST, "/upload/virology-test/wls-result-tokengen") { r ->
                    events.emit(javaClass, VirologyTokenGen())
                    handleV1TokenGen(VirologyTokenExchangeSource.Wls, virologyService, r)
                }),
            authorisedBy(
                authenticator,
                path(POST, "/upload/virology-test/v2/npex-result") { r ->
                    events.emit(javaClass, VirologyResults())
                    handleV2TestResult(VirologyResultSource.Npex, virologyService, r)
                }),
            authorisedBy(
                authenticator,
                path(POST, "/upload/virology-test/v2/fiorano-result") { r ->
                    events.emit(javaClass, VirologyResults())
                    handleV2TestResult(VirologyResultSource.Fiorano, virologyService, r)
                }),
            authorisedBy(
                authenticator,
                path(POST, "/upload/virology-test/v2/eng-result-tokengen") { r ->
                    events.emit(javaClass, VirologyTokenGen())
                    handleV2TokenGen(VirologyTokenExchangeSource.Eng, virologyService, r)
                }),
            authorisedBy(
                authenticator,
                path(POST, "/upload/virology-test/v2/wls-result-tokengen") { r ->
                    events.emit(javaClass, VirologyTokenGen())
                    handleV2TokenGen(VirologyTokenExchangeSource.Wls, virologyService, r)
                }
            )
        )
    )

    private fun handleV2TestResult(
        source: VirologyResultSource,
        virologyService: VirologyService,
        request: APIGatewayProxyRequestEvent
    ) = readJsonOrNull<VirologyResultRequest>(request) { VirologyResultRequest.v2TestKitValidator(it) }
        ?.let {
            events.emit(javaClass, TestResultUploaded(2, source, it.ctaToken, it.testResult, it.testKit))
            virologyService.acceptTestResult(it).toHttpResponse()
        }
        ?: HttpResponses.unprocessableEntity()

    private fun handleV2TokenGen(
        source: VirologyTokenExchangeSource,
        virologyService: VirologyService,
        request: APIGatewayProxyRequestEvent
    ): APIGatewayProxyResponseEvent =
        readJsonOrNull<VirologyTokenGenRequest>(request) { VirologyTokenGenRequest.v2TestKitValidator(it) }
            ?.let {
                events(javaClass, CtaTokenGen(2, source, it.testResult, it.testKit))
                HttpResponses.ok(Jackson.toJson(virologyService.acceptTestResultGeneratingTokens(it)))
            }
            ?: HttpResponses.unprocessableEntity()

    private fun handleV1TestResult(
        source: VirologyResultSource,
        virologyService: VirologyService,
        request: APIGatewayProxyRequestEvent
    ): APIGatewayProxyResponseEvent =
        readJsonOrNull<VirologyResultRequest>(request) { VirologyResultRequest.v1TestKitValidator(it) }
            ?.let {
                it.testKit = TestKit.LAB_RESULT
                events.emit(javaClass, TestResultUploaded(1, source, it.ctaToken, it.testResult, it.testKit))
                virologyService.acceptTestResult(it).toHttpResponse()
            }
            ?: HttpResponses.unprocessableEntity()

    private fun handleV1TokenGen(
        source: VirologyTokenExchangeSource,
        virologyService: VirologyService,
        request: APIGatewayProxyRequestEvent
    ): APIGatewayProxyResponseEvent =
        readJsonOrNull<VirologyTokenGenRequest>(request) { VirologyTokenGenRequest.v1TestKitValidator(it) }
            ?.let {
                it.testKit = TestKit.LAB_RESULT
                events(javaClass, CtaTokenGen(1, source, it.testResult, it.testKit))
                HttpResponses.ok(Jackson.toJson(virologyService.acceptTestResultGeneratingTokens(it)))
            }
            ?: HttpResponses.unprocessableEntity()

    override fun handler() = handler

    enum class VirologyResultSource {
        Npex, Fiorano
    }

    enum class VirologyTokenExchangeSource {
        Eng, Wls
    }

    private inline fun <reified T> readJsonOrNull(request: APIGatewayProxyRequestEvent, fn: (T) -> T): T? =
        readOrNull(request.body, T::class.java) { e: Exception -> events.emit(javaClass, UnprocessableJson(e)) }
            ?.let {
                try {
                    fn(it)
                } catch (e: Exception) {
                    null
                }
            }

    companion object {
        private fun virologyService(environment: Environment, events: Events): VirologyService = VirologyService(
            VirologyPersistenceService(
                AmazonDynamoDBClientBuilder.defaultClient(),
                fromEnvironment(environment),
                events
            ),
            TokensGenerator(),
            CLOCK,
            VirologyPolicyConfig(),
            events
        )
    }
}
