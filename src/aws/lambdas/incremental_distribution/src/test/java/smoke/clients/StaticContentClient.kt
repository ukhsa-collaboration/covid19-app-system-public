package smoke.clients

import org.http4k.client.JavaHttpClient
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.slf4j.LoggerFactory
import smoke.env.EnvConfig

class StaticContentClient(private val client: JavaHttpClient,
                          private val config: EnvConfig) {

    private val logger = LoggerFactory.getLogger(StaticContentClient::class.java)

    fun availabilityAndroid() = getStaticContent(config.availabilityAndroidDistUrl)

    fun availabilityIos() = getStaticContent(config.availabilityIosDistUrl)

    fun exposureConfiguration() = getStaticContent(config.exposureConfigurationDistUrl)

    fun riskyPostDistricts() = getStaticContent(config.postDistrictsDistUrl)

    fun riskyVenues() = getStaticContent(config.riskyVenuesDistUrl)

    fun selfIsolation() = getStaticContent(config.selfIsolationDistUrl)

    fun symptomaticQuestionnaire() = getStaticContent(config.symptomaticQuestionnaireDistUrl)

    private fun getStaticContent(uri: String): String {
        logger.info("getStaticContent: $uri")

        return client(Request(Method.GET, uri))
            .requireStatusCode(Status.OK)
            .requireSignatureHeaders()
            .requireJsonContentType()
            .bodyString()
    }

}
