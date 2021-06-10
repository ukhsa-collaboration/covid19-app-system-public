package uk.nhs.nhsx.sanity.lambdas.config

import com.fasterxml.jackson.databind.JsonNode
import com.natpryce.hamkrest.isNullOrBlank
import dev.forkhandles.result4k.get
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.mapFailure
import dev.forkhandles.result4k.resultFrom
import org.http4k.core.Uri
import org.http4k.hamkrest.hasBody
import uk.nhs.nhsx.sanity.lambdas.config.DeployedLambda.AnalyticsSubmission
import uk.nhs.nhsx.sanity.lambdas.config.DeployedLambda.AvailabilityAndroidDistribution
import uk.nhs.nhsx.sanity.lambdas.config.DeployedLambda.AvailabilityIosDistribution
import uk.nhs.nhsx.sanity.lambdas.config.DeployedLambda.DiagnosisKeysDistribution
import uk.nhs.nhsx.sanity.lambdas.config.DeployedLambda.DiagnosisKeysSubmission
import uk.nhs.nhsx.sanity.lambdas.config.DeployedLambda.ExposureNotificationDistribution
import uk.nhs.nhsx.sanity.lambdas.config.DeployedLambda.PostDistrictsDistribution
import uk.nhs.nhsx.sanity.lambdas.config.DeployedLambda.RiskyVenueConfigurationDistribution
import uk.nhs.nhsx.sanity.lambdas.config.DeployedLambda.RiskyVenuesDistribution
import uk.nhs.nhsx.sanity.lambdas.config.DeployedLambda.SelfIsolationDistribution
import uk.nhs.nhsx.sanity.lambdas.config.DeployedLambda.LocalMessagesDistribution
import uk.nhs.nhsx.sanity.lambdas.config.DeployedLambda.SymptomaticQuestionnaireDistribution
import uk.nhs.nhsx.sanity.lambdas.config.DeployedLambda.VirologyKit
import uk.nhs.nhsx.sanity.lambdas.config.Resource.DynamicContent
import uk.nhs.nhsx.sanity.lambdas.config.Resource.DynamicUrl
import uk.nhs.nhsx.sanity.lambdas.config.Resource.Missing
import uk.nhs.nhsx.sanity.lambdas.config.Resource.Static
import java.nio.file.Path

sealed class EndpointConfig(val name: String, val endpointUri: Uri) {
    override fun toString() = name

}

interface Secured {
    val authHeader: String
}

interface HealthCheck {
    val healthEndpoint: Uri
    val healthAuthHeader: String
}

interface StoreBacked {
    val storeName: String?
}

interface ServesResource {
    val resource: Resource
}

sealed class Resource {
    abstract val content: String?

    fun contentMatcher() = content?.let { hasBody(it) } ?: hasBody(!isNullOrBlank)

    class Static(private val value: String) : Resource() {
        override val content: String
            get() {
                System.err.println(value)
                return Path.of("../../$value").toFile().also { System.err.println("loading from: ${it.absolutePath}") }
                    .reader().readText()
            }
    }

    object DynamicContent : Resource() {
        override val content = null
    }

    object DynamicUrl : Resource() {
        override val content = null
    }

    object Missing : Resource() {
        override val content get() = throw RuntimeException("Missing resource!")
    }
}

/**
 * Distribution lambdas allow the MobileApp to download of data through CloudFront and effectively front an S3 bucket
 */
class Distribution(name: String, endpointUri: Uri, private val store: String?, override val resource: Resource) :
    EndpointConfig(name, endpointUri), StoreBacked, ServesResource {

    override val storeName get() = store ?: throw RuntimeException("Missing storeName in $this")

    companion object {
        fun from(endpoint: Endpoint) = { config: JsonNode ->
            Distribution(
                endpoint.lambda.name,
                config.requiredUri("${endpoint.name}_endpoint"),
                config.optionalText(endpoint.lambda.storeJsonName),
                endpoint.lambda.resource()
            )
        }
    }
}

/**
 * Download lambdas allow Third Parties to download of data through CloudFront and effectively front an S3 bucket
 */
class Download(name: String, endpointUri: Uri, override val resource: Resource) : EndpointConfig(name, endpointUri),
    ServesResource {
    companion object {
        fun from(endpoint: Endpoint) = { config: JsonNode ->
            Download(
                endpoint.lambda.name,
                config.requiredUri("${endpoint.name}_endpoint"),
                endpoint.lambda.resource(),
            )
        }
    }
}

/**
 * Upload lambdas allow Third Parties to upload data through CloudFront
 */
class Upload(
    name: String,
    endpointUri: Uri,
    override val authHeader: String,
    override val healthEndpoint: Uri,
    override val healthAuthHeader: String
) : EndpointConfig(name, endpointUri), HealthCheck, Secured {
    companion object {
        fun from(endpoint: Endpoint) = { config: JsonNode ->
            Upload(
                endpoint.lambda.name,
                config.requiredUri("${endpoint.name}_endpoint"),
                config.authHeader(endpoint.lambda.endpointBearerTokenJsonName!!),
                config.requiredUri(endpoint.lambda.healthEndpoint),
                config.authHeader("health")
            )
        }
    }
}

/**
 * Submission lambdas allow the MobileApp to submit data through CloudFront and effectively front an S3 bucket
 */
class Submission(
    name: String,
    endpointUri: Uri,
    override val authHeader: String,
    override val healthEndpoint: Uri,
    override val healthAuthHeader: String,
    override val storeName: String?,
    override val resource: Resource
) : EndpointConfig(name, endpointUri), StoreBacked, HealthCheck, Secured, ServesResource {
    companion object {
        fun from(endpoint: Endpoint) = { config: JsonNode ->
            Submission(
                endpoint.lambda.name,
                config.requiredUri("${endpoint.name}_endpoint"),
                config.authHeader(endpoint.lambda.endpointBearerTokenJsonName!!),
                config.requiredUri(endpoint.lambda.healthEndpoint),
                config.authHeader("health"),
                config.optionalText(endpoint.lambda.storeJsonName),
                endpoint.lambda.resource(),
            )
        }
    }
}

/**
 * Processing lambdas run out-of-band and create work, for example on a schedule. They may have an output store
 */
class Processing(name: String, endpointUri: Uri, override val storeName: String?) : EndpointConfig(name, endpointUri),
    StoreBacked {
    companion object {
        fun from(endpoint: Endpoint) = { config: JsonNode ->
            Processing(
                endpoint.lambda.name,
                config.requiredUri("${endpoint.name}_endpoint"),
                config.optionalText(endpoint.lambda.storeJsonName)
            )
        }
    }
}

/**
 * CircuitBreaker lambdas allow flagging of various operations by the Mobile App as a safety feature.
 */
class CircuitBreaker(
    name: String,
    endpointUri: Uri,
    override val authHeader: String,
    override val healthEndpoint: Uri,
    override val healthAuthHeader: String
) : EndpointConfig(name, endpointUri), HealthCheck, Secured {
    companion object {
        fun from(endpoint: Endpoint) = { config: JsonNode ->
            CircuitBreaker(
                endpoint.name,
                config.requiredUri("${endpoint.name}_endpoint"),
                config.authHeader(endpoint.lambda.endpointBearerTokenJsonName!!),
                config.requiredUri(endpoint.lambda.healthEndpoint),
                config.authHeader("health"),
            )
        }
    }
}

private fun JsonNode.optionalText(name: String) = this[name]?.textValue()

private fun JsonNode.requiredText(name: String): String = resultFrom { this[name]!!.textValue() }
    .mapFailure { "Missing value $name".also { System.err.println(it) } }
    .get()

private fun JsonNode.requiredUri(name: String) = Uri.of(requiredText(name))

private fun JsonNode.authHeader(name: String): String =
    resultFrom { this["auth_headers"]!! }
        .map { it.requiredText(name).removePrefix("Bearer ") }
        .mapFailure { throw RuntimeException("Missing value $name") }
        .get()

private val resources = mapOf(
    AvailabilityAndroidDistribution to Static("src/static/availability-android.json"),
    AvailabilityIosDistribution to Static("src/static/availability-ios.json"),
    ExposureNotificationDistribution to Static("src/static/exposure-configuration.json"),
    LocalMessagesDistribution to Static("out/local-messages/local-messages.json"),
    SymptomaticQuestionnaireDistribution to Static("src/static/symptomatic-questionnaire.json"),
    SelfIsolationDistribution to Static("src/static/self-isolation.json"),
    RiskyVenueConfigurationDistribution to Static("src/static/risky-venue-configuration.json"),
    RiskyVenuesDistribution to DynamicContent,
    PostDistrictsDistribution to DynamicContent,
    DiagnosisKeysDistribution to DynamicUrl,
    VirologyKit to DynamicUrl,
    DiagnosisKeysSubmission to DynamicUrl,
    AnalyticsSubmission to DynamicUrl
)

fun DeployedLambda.resource() = resources[this] ?: Missing
