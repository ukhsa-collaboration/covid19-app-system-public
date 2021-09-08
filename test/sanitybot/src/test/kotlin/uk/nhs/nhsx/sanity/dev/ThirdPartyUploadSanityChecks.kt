package uk.nhs.nhsx.sanity.dev

import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNPROCESSABLE_ENTITY
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import uk.nhs.nhsx.sanity.LambdaSanityCheck
import uk.nhs.nhsx.sanity.config.DeployedApiResource.RiskyPostcodeDistrictsUpload
import uk.nhs.nhsx.sanity.config.DeployedApiResource.RiskyVenuesUpload
import uk.nhs.nhsx.sanity.config.DeployedApiResource.TestResultsUpload
import uk.nhs.nhsx.sanity.config.Upload

@EnabledIfEnvironmentVariable(named = "TARGET_ENVIRONMENT", matches = "dev")
class ThirdPartyUploadSanityChecks : LambdaSanityCheck() {

//    Check NPEx (England) test lab upload endpoint - POST gets 422✅
//    Check Fiorano (Wales) test lab upload endpoint - POST gets 422✅
//    Check risky venue upload - POST gets 422✅
//    Check risky post district upload - POST gets 422✅
//    Check England token endpoint - POST gets 422✅
//    Check Fiorano (Wales) token endpoint - POST gets 422✅
//    Check risky post districts upload health - POST gets 200✅
//    Check risky venues upload health - POST gets 200✅

    @ParameterizedTest(name = "{arguments}")
    @MethodSource("uploads")
    fun `Third-party upload returns a 422`(upload: Upload) {
        assertThat(upload.withSecureClient(Request(POST, upload.endpointUri)),
            hasStatus(UNPROCESSABLE_ENTITY))
    }

    @Suppress("unused")
    companion object {
        @JvmStatic
        private fun uploads(): List<Any> = endpoints()
            .filterIsInstance<Upload>()
    }

    @Test
    fun `england token endpoint returns a 422`() {
        val lambda = env.configFor(TestResultsUpload, "test_results_eng_tokengen_upload") as Upload
        assertThat(lambda.withSecureClient(Request(POST, lambda.endpointUri)), hasStatus(UNPROCESSABLE_ENTITY))
    }

    @Test
    fun `wales token endpoint returns a 422`() {
        val lambda = env.configFor(TestResultsUpload, "test_results_wls_tokengen_upload") as Upload
        assertThat(lambda.withSecureClient(Request(POST, lambda.endpointUri)), hasStatus(UNPROCESSABLE_ENTITY))
    }

    @Test
    fun `risky post districts upload health endpoint returns a 200`() {
        val lambda = env.configFor(RiskyPostcodeDistrictsUpload, "risky_post_districts_upload") as Upload
        assertThat(lambda.withHealthClient(Request(POST, lambda.healthEndpoint)), hasStatus(OK))
    }

    @Test
    fun `risky venues upload health endpoint returns a 200`() {
        val lambda = env.configFor(RiskyVenuesUpload, "risky_venues_upload") as Upload
        assertThat(lambda.withHealthClient(Request(POST, lambda.healthEndpoint)), hasStatus(OK))
    }
}
