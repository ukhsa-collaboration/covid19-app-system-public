package uk.nhs.nhsx.sanity.lambdas.prod

import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.sanity.lambdas.LambdaSanityCheck
import uk.nhs.nhsx.sanity.lambdas.config.DeployedLambda.RiskyPostcodeDistrictsUpload
import uk.nhs.nhsx.sanity.lambdas.config.DeployedLambda.RiskyVenuesUpload
import uk.nhs.nhsx.sanity.lambdas.config.Upload

class RestrictedHealthSanityChecks : LambdaSanityCheck() {

//    Check risky venues upload health endpoint - post gets 403  - WHY?✅
//    Check risky post districts upload health endpoint  - post gets 403  - WHY?✅

    @Test
    fun `risky venues upload returns a 403`() {
        val lambda = env.configFor(RiskyVenuesUpload, "risky_venues_upload") as Upload
        assertThat(lambda.withSecureClient(Request(POST, lambda.healthEndpoint)), hasStatus(FORBIDDEN))
    }

    @Test
    fun `risky post districts upload returns a 403`() {
        val lambda = env.configFor(RiskyPostcodeDistrictsUpload, "risky_post_districts_upload") as Upload
        assertThat(lambda.withSecureClient(Request(POST, lambda.healthEndpoint)), hasStatus(FORBIDDEN))
    }
}
