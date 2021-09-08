package uk.nhs.nhsx.sanity.waf

import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import uk.nhs.nhsx.sanity.LambdaSanityCheck
import uk.nhs.nhsx.sanity.config.DeployedApiResource
import uk.nhs.nhsx.sanity.config.Upload

@DisabledIfEnvironmentVariable(named = "TARGET_ENVIRONMENT", matches = "dev")
class ThirdPartyUploadSanityChecks : LambdaSanityCheck() {

//    Check risky venue upload is blocked by WAF - post gets 403✅
//    Check risky post district upload - post gets 403✅
//    Check NPEx (England) test lab upload endpoint is blocked by WAF - post gets 403✅
//    Check Fiorano (Wales) test lab upload endpoint is blocked by WAF - post gets 403✅

    @ParameterizedTest(name = "{arguments}")
    @MethodSource("uploads")
    fun `Third-party upload returns a 403`(upload: Upload) {
        assertThat(upload.withSecureClient(Request(POST, upload.endpointUri)),
            hasStatus(FORBIDDEN))
    }

    @Suppress("unused")
    companion object {
        @JvmStatic
        private fun uploads(): List<Any> = endpoints().filterIsInstance<Upload>()

        @JvmStatic
        private fun tokenTestResultUpload(): List<Any> =
            DeployedApiResource.TestResultsUpload.endpointJsonNames
                .map { env.configFor(DeployedApiResource.TestResultsUpload, it) }
    }

    @ParameterizedTest(name = "england token endpoint returns a 403 {arguments}")
    @MethodSource("tokenTestResultUpload")
    fun `test results token endpoint returns a 403`(upload: Upload) {
        assertThat(upload.withSecureClient(Request(POST, upload.endpointUri)), hasStatus(FORBIDDEN))
    }
}
