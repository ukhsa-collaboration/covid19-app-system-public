package uk.nhs.nhsx.virology

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.kms.AWSKMSClientBuilder
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.HttpResponses
import uk.nhs.nhsx.core.Jackson
import uk.nhs.nhsx.core.Jackson.readOrNull
import uk.nhs.nhsx.core.StandardSigningFactory
import uk.nhs.nhsx.core.SystemClock.CLOCK
import uk.nhs.nhsx.core.auth.ApiName.Health
import uk.nhs.nhsx.core.auth.ApiName.Mobile
import uk.nhs.nhsx.core.auth.Authenticator
import uk.nhs.nhsx.core.auth.ResponseSigner
import uk.nhs.nhsx.core.auth.StandardAuthentication.awsAuthentication
import uk.nhs.nhsx.core.aws.ssm.AwsSsmParameters
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.InfoEvent
import uk.nhs.nhsx.core.events.PrintingJsonEvents
import uk.nhs.nhsx.core.events.UnprocessableJson
import uk.nhs.nhsx.core.events.VirologyCtaExchange
import uk.nhs.nhsx.core.events.VirologyOrder
import uk.nhs.nhsx.core.events.VirologyRegister
import uk.nhs.nhsx.core.events.VirologyResults
import uk.nhs.nhsx.core.routing.ApiGatewayHandler
import uk.nhs.nhsx.core.routing.Routing.Method.POST
import uk.nhs.nhsx.core.routing.Routing.path
import uk.nhs.nhsx.core.routing.Routing.routes
import uk.nhs.nhsx.core.routing.Routing.throttlingResponse
import uk.nhs.nhsx.core.routing.RoutingHandler
import uk.nhs.nhsx.core.routing.StandardHandlers.authorisedBy
import uk.nhs.nhsx.core.routing.StandardHandlers.mobileAppVersionFrom
import uk.nhs.nhsx.core.routing.StandardHandlers.withSignedResponses
import uk.nhs.nhsx.virology.exchange.CtaExchangeRequestV1
import uk.nhs.nhsx.virology.exchange.CtaExchangeRequestV2
import uk.nhs.nhsx.virology.lookup.VirologyLookupRequestV1
import uk.nhs.nhsx.virology.lookup.VirologyLookupRequestV2
import uk.nhs.nhsx.virology.lookup.VirologyLookupService
import uk.nhs.nhsx.virology.order.TokensGenerator
import uk.nhs.nhsx.virology.order.VirologyRequestType
import uk.nhs.nhsx.virology.order.VirologyRequestType.ORDER
import uk.nhs.nhsx.virology.order.VirologyRequestType.REGISTER
import uk.nhs.nhsx.virology.order.VirologyWebsiteConfig
import uk.nhs.nhsx.virology.persistence.VirologyPersistenceService
import java.time.Duration
import java.time.Instant
import java.util.function.Supplier

class VirologySubmissionHandler @JvmOverloads constructor(
    environment: Environment = Environment.fromSystem(),
    clock: Supplier<Instant> = CLOCK,
    delayDuration: Duration = Duration.ofSeconds(1),
    private val events: Events = PrintingJsonEvents(clock),
    mobileAuthenticator: Authenticator = awsAuthentication(Mobile, events),
    signer: ResponseSigner = StandardSigningFactory(
        clock,
        AwsSsmParameters(),
        AWSKMSClientBuilder.defaultClient()
    ).signResponseWithKeyGivenInSsm(environment, events),
    persistence: VirologyPersistenceService = VirologyPersistenceService(
        AmazonDynamoDBClientBuilder.defaultClient(),
        VirologyConfig.fromEnvironment(environment),
        events
    ),
    virology: VirologyService = virologyService(clock, events, persistence),
    virologyLookup: VirologyLookupService = virologyLookup(clock, events, persistence),
    websiteConfig: VirologyWebsiteConfig = VirologyWebsiteConfig.fromEnvironment(environment),
    healthAuthenticator: Authenticator = awsAuthentication(Health, events)
) : RoutingHandler() {

    private val handler = withSignedResponses(
        events,
        environment,
        signer,
        routes(
            authorisedBy(
                healthAuthenticator,
                path(POST, "/virology-test/health") { _, _ -> HttpResponses.ok() }
            ),
            authorisedBy(
                mobileAuthenticator,
                path(POST, "/virology-test/home-kit/order") { _, _ ->
                    events.emit(javaClass, VirologyOrder())
                    handleVirologyOrder(virology, websiteConfig, ORDER)
                }),
            authorisedBy(
                mobileAuthenticator,
                path(POST, "/virology-test/home-kit/register") { _, _ ->
                    events.emit(javaClass, VirologyRegister())
                    handleVirologyOrder(virology, websiteConfig, REGISTER)
                }),
            authorisedBy(
                mobileAuthenticator,
                path(POST, "/virology-test/results") { r: APIGatewayProxyRequestEvent, _ ->
                    events.emit(javaClass, VirologyResults())
                    readOrNull<VirologyLookupRequestV1>(r.body) { e ->
                        events.emit(
                            javaClass,
                            UnprocessableJson(e)
                        )
                    }
                        ?.let { virologyLookup.lookup(it).toHttpResponse() }
                        ?: HttpResponses.unprocessableEntity()
                }),
            authorisedBy(
                mobileAuthenticator,
                path(POST, "/virology-test/cta-exchange") { r: APIGatewayProxyRequestEvent, _ ->
                    events.emit(javaClass, VirologyCtaExchange())
                    throttlingResponse(delayDuration) {
                        readOrNull<CtaExchangeRequestV1>(r.body) {
                            events.emit(
                                javaClass,
                                UnprocessableVirologyCtaExchange(it)
                            )
                        }
                            ?.let { virology.exchangeCtaTokenForV1(it).toHttpResponse() }
                            ?: HttpResponses.badRequest()
                    }
                }
            ),
            authorisedBy(
                mobileAuthenticator,
                path(POST, "/virology-test/v2/order") { _, _ ->
                    events.emit(javaClass, VirologyOrder())
                    handleVirologyOrder(virology, websiteConfig, ORDER)
                }
            ),
            authorisedBy(
                mobileAuthenticator,
                path(POST, "/virology-test/v2/results") { r: APIGatewayProxyRequestEvent, _ ->
                    events.emit(javaClass, VirologyResults())
                    readOrNull<VirologyLookupRequestV2>(r.body) { e -> events.emit(javaClass, UnprocessableJson(e)) }
                        ?.let { virologyLookup.lookup(it, mobileAppVersionFrom(r)).toHttpResponse() }
                        ?: HttpResponses.unprocessableEntity()
                }
            ),
            authorisedBy(
                mobileAuthenticator,
                path(POST, "/virology-test/v2/cta-exchange") { r: APIGatewayProxyRequestEvent, _ ->
                    events.emit(javaClass, VirologyCtaExchange())
                    throttlingResponse(delayDuration) {
                        readOrNull<CtaExchangeRequestV2>(r.body) { e: Exception ->
                            events.emit(javaClass, UnprocessableVirologyCtaExchange(e))
                        }
                            ?.let {
                                virology.exchangeCtaTokenForV2(
                                    it, mobileAppVersionFrom(r)
                                ).toHttpResponse()
                            } ?: HttpResponses.badRequest()
                    }
                }
            )
        ))

    private fun handleVirologyOrder(
        service: VirologyService,
        websiteConfig: VirologyWebsiteConfig,
        order: VirologyRequestType
    ): APIGatewayProxyResponseEvent {
        val response = service.handleTestOrderRequest(websiteConfig, order)
        events.emit(
            javaClass,
            InfoEvent("Virology order created ctaToken: ${response.tokenParameterValue}, testResultToken: ${response.testResultPollingToken}")
        )
        return HttpResponses.ok(Jackson.toJson(response))
    }

    override fun handler(): ApiGatewayHandler = handler

    companion object {
        private fun virologyService(
            clock: Supplier<Instant>,
            events: Events,
            persistence: VirologyPersistenceService
        ): VirologyService {
            return VirologyService(
                persistence,
                TokensGenerator,
                clock,
                VirologyPolicyConfig(),
                events
            )
        }

        private fun virologyLookup(
            clock: Supplier<Instant>,
            events: Events,
            persistence: VirologyPersistenceService
        ): VirologyLookupService {
            return VirologyLookupService(
                persistence,
                clock,
                VirologyPolicyConfig(),
                events
            )
        }
    }
}
