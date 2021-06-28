package uk.nhs.nhsx.sanity.lambdas.common

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.format.Jackson.auto
import org.http4k.hamkrest.hasContentType
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import uk.nhs.nhsx.sanity.lambdas.LambdaSanityCheck
import uk.nhs.nhsx.sanity.lambdas.config.DeployedLambda.PostDistrictsDistribution
import uk.nhs.nhsx.sanity.lambdas.config.DeployedLambda.RiskyVenuesDistribution
import uk.nhs.nhsx.sanity.lambdas.config.DeployedLambda.LocalMessagesDistribution
import uk.nhs.nhsx.sanity.lambdas.config.Distribution
import uk.nhs.nhsx.sanity.lambdas.config.Resource.DynamicContent
import uk.nhs.nhsx.sanity.lambdas.config.Resource.DynamicUrl

@DisabledIfEnvironmentVariable(named = "TARGET_ENVIRONMENT", matches = "dev")
class DistributionSanityChecks : LambdaSanityCheck() {

//    Check risky venue distribution - GET 200 + IS JSON✅
//    Check risky post districts distribution - GET 200 + IS JSON✅
//    Check risky post districts distribution v2 - GET 200 + IS JSON✅
//    Check android availability distribution - GET - match static✅
//    Check ios availability distribution - GET - match static✅
//    Check exposure risk configuration distribution - GET - match static✅
//    Check Self isolation distribution - GET - match static✅
//    Check Symptomatic questionnaire distribution - GET - match static✅

    @Test
    fun `Risky venue distribution returns a 200`() {
        val riskyVenue = env.configFor(RiskyVenuesDistribution, "risky_venues_distribution") as Distribution
        assertThat(insecureClient(Request(GET, riskyVenue.endpointUri)),
            hasStatus(OK).and(hasContentType(ContentType("application/json")))
        )
    }

    @Test
    fun `Risky post districts distribution returns a 200`() {
        val riskyPostDistrict = env.configFor(PostDistrictsDistribution, "post_districts_distribution") as Distribution
        assertThat(insecureClient(Request(GET, riskyPostDistrict.endpointUri)),
            hasStatus(OK).and(hasContentType(ContentType("application/json")))
        )
    }

    @Test
    fun `Risky post districts v2 distribution returns a 200`() {
        val riskyPostDistrict = env.configFor(PostDistrictsDistribution, "post_districts_distribution") as Distribution
        assertThat(insecureClient(Request(GET, Uri.of(riskyPostDistrict.endpointUri.toString() + "-v2"))),
            hasStatus(OK).and(hasContentType(ContentType("application/json")))
        )
    }

    @Test
    fun `Risky post districts v2 distribution matches static tier metadata`() {
        val riskyPostDistrict = env.configFor(PostDistrictsDistribution, "post_districts_distribution") as Distribution
        val apiResponse = insecureClient(Request(GET, Uri.of(riskyPostDistrict.endpointUri.toString() + "-v2")))
        val apiContent = Body.auto<Map<String,Any>>().toLens().extract(apiResponse)
        val staticTierMetadata = downloadS3Object(riskyPostDistrict.storeName,"tier-metadata")
        assertThat(apiContent["riskLevels"], equalTo(staticTierMetadata))

    }

    @Test
    fun `Local Messages distribution endpoint returns a 200`() {
        val localMessages = env.configFor(LocalMessagesDistribution,"local_messages_distribution") as Distribution
        assertThat(insecureClient(Request(GET, localMessages.endpointUri)),
            hasStatus(OK).and(hasContentType(ContentType("application/json")))
        )
    }


    @MethodSource("distribution")
    @ParameterizedTest(name = "{arguments}")
    fun `Distribution endpoint returns a 200 and matches resource`(distribution: Distribution) {
        assertThat(insecureClient(Request(GET, distribution.endpointUri)),
            hasStatus(OK).and(distribution.resource.contentMatcher())
        )
    }

    @Suppress("unused")
    companion object {
        @JvmStatic
        private fun distribution(): List<Any> =
            endpoints()
                .filterIsInstance<Distribution>()
                .filterNot { it.resource == DynamicUrl || it.resource == DynamicContent }
    }

    private fun downloadS3Object(bucket: String, key: String): Any {
        val s3 = S3Client.builder().build()
        val s3ObjectResponse = s3.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build())
        val http4kResponse = Response(OK).body(s3ObjectResponse)
        return Body.auto<Map<String,Any>>().toLens().extract(http4kResponse)
    }
}
