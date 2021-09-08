package uk.nhs.nhsx.sanity.waf

import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable
import uk.nhs.nhsx.sanity.LambdaSanityCheck
import uk.nhs.nhsx.sanity.config.DeployedApiResource.RiskyPostcodeDistrictsUpload
import uk.nhs.nhsx.sanity.config.DeployedApiResource.RiskyVenuesUpload
import uk.nhs.nhsx.sanity.config.Upload

@DisabledIfEnvironmentVariable(named = "TARGET_ENVIRONMENT", matches = "dev")
class RestrictedHealthSanityChecks : LambdaSanityCheck() {

//    Check risky venues upload health endpoint - post gets 403  - WHY?✅
//    Check risky post districts upload health endpoint  - post gets 403  - WHY?✅

    @Test
    fun `risky venues upload returns a 403`() {
        val lambda = env.configFor(RiskyVenuesUpload, "risky_venues_upload") as Upload
        assertThat(lambda.withHealthClient(Request(POST, lambda.healthEndpoint)), hasStatus(FORBIDDEN))
    }

    @Test
    fun `risky post districts upload returns a 403`() {
        val lambda = env.configFor(RiskyPostcodeDistrictsUpload, "risky_post_districts_upload") as Upload
        assertThat(lambda.withHealthClient(Request(POST, lambda.healthEndpoint)), hasStatus(FORBIDDEN))
    }
}
