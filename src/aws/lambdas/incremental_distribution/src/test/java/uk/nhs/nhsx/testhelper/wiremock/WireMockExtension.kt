package uk.nhs.nhsx.testhelper.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import com.github.tomakehurst.wiremock.client.VerificationException

import com.github.tomakehurst.wiremock.verification.NearMiss

import com.github.tomakehurst.wiremock.verification.LoggedRequest

class WireMockExtension : BeforeEachCallback, AfterEachCallback, ParameterResolver {

    private val server: WireMockServer = WireMockServer(0)

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean =
        parameterContext.parameter.type.equals(WireMockServer::class.java)

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any = server

    override fun beforeEach(context: ExtensionContext) {
        server.start()

        if (server.options.httpDisabled) {
            WireMock.configureFor("https", "localhost", server.httpsPort());
        } else {
            WireMock.configureFor("localhost", server.port());
        }
    }

    override fun afterEach(context: ExtensionContext) = try {
        failOnUnmatchedRequests()
        server.resetAll()
    } finally {
        server.stop()
    }

    private fun failOnUnmatchedRequests() {
        val unmatchedRequests: List<LoggedRequest> = server.findAllUnmatchedRequests()
        if (unmatchedRequests.isNotEmpty()) {
            val nearMisses: List<NearMiss> = server.findNearMissesForAllUnmatchedRequests()
            if (nearMisses.isEmpty()) {
                throw VerificationException.forUnmatchedRequests(unmatchedRequests)
            } else {
                throw VerificationException.forUnmatchedNearMisses(nearMisses)
            }
        }
    }
}
