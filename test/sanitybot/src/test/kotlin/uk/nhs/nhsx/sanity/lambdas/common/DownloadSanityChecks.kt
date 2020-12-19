package uk.nhs.nhsx.sanity.lambdas.common

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import uk.nhs.nhsx.sanity.lambdas.LambdaSanityCheck
import uk.nhs.nhsx.sanity.lambdas.config.Download

class DownloadSanityChecks : LambdaSanityCheck() {

    //Check risky venue messages download - GET - match static ✅

    @MethodSource("download")
    @ParameterizedTest(name = "{arguments}")
    fun `Download endpoint returns a 200 and matches resource`(download: Download) {
        assertThat(insecureClient(Request(GET, download.endpointUri)), hasStatus(OK).and(download.resource.contentMatcher()))
    }

    @Suppress("unused")
    companion object {
        @JvmStatic
        private fun download(): List<Any> = endpoints().filterIsInstance<Download>()
    }
}
