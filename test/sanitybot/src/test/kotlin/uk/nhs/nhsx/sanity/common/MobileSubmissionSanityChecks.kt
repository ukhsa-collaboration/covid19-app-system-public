package uk.nhs.nhsx.sanity.common

import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.SERVICE_UNAVAILABLE
import org.http4k.core.Uri
import org.http4k.core.extend
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import uk.nhs.nhsx.sanity.LambdaSanityCheck
import uk.nhs.nhsx.sanity.config.DeployedApiResource.IsolationPayment
import uk.nhs.nhsx.sanity.config.DeployedApiResource.AnalyticsSubmission
import uk.nhs.nhsx.sanity.config.DeployedApiResource.DiagnosisKeysSubmission
import uk.nhs.nhsx.sanity.config.DeployedApiResource.VirologyKit
import uk.nhs.nhsx.sanity.config.Resource
import uk.nhs.nhsx.sanity.config.Submission

class MobileSubmissionSanityChecks : LambdaSanityCheck() {

//    Check diagnosis key submission - POST 200✅
//    Check analytics submission - POST 200✅
//    Check analytics events submission - POST 400✅
//    Isolation payment create token - POST 400✅
//    Isolation payment update token - POST 400✅
//    Check Order Home Test kit endpoint - ${ENDPOINT}/home-kit/order gets 403✅
//    Check Register Home Test endpoint - ${ENDPOINT}/home-kit/register gets 403✅
//    Poll Test Results endpoint - ${ENDPOINT}/results gets 403✅
//    Exchange cta token endpoint - ${ENDPOINT}/cta-exchange gets 403✅

    @ParameterizedTest(name = "submission gets a 400 {arguments}")
    @MethodSource("submissionsWithoutIsolationPayment")
    fun `submission gets a 400`(submission: Submission) {
        assertThat(submission.withSecureClient(Request(POST, submission.endpointUri)),
            hasStatus(BAD_REQUEST))
    }

    @ParameterizedTest(name = "submission gets a 503 {arguments}")
    @MethodSource("isolationPaymentSubmissions")
    fun `submission gets a 503`(submission: Submission) {
        assertThat(submission.withSecureClient(Request(POST, submission.endpointUri)),
            hasStatus(SERVICE_UNAVAILABLE))
    }

    @Suppress("unused")
    companion object {
        @JvmStatic
        private fun submissionsWithoutIsolationPayment(): List<Any> = endpoints()
            .filterIsInstance<Submission>()
            .filterNot { it.resource == Resource.DynamicUrl || it.name == IsolationPayment.name }

        @JvmStatic
        private fun isolationPaymentSubmissions(): List<Any> = endpoints()
            .filterIsInstance<Submission>()
            .filterNot { it.resource == Resource.DynamicUrl || it.name != IsolationPayment.name }
    }

    @Test
    fun `diagnosis key submission gets a 200`() {
        val diagnosisKey = env.configFor(DiagnosisKeysSubmission, "diagnosis_keys_submission") as Submission
        assertThat(diagnosisKey.withSecureClient(Request(POST, diagnosisKey.endpointUri)),
            hasStatus(OK))
    }

    @Test
    fun `analytics submission gets a 200`() {
        val diagnosisKey = env.configFor(AnalyticsSubmission, "analytics_submission") as Submission
        assertThat(diagnosisKey.withSecureClient(Request(POST, diagnosisKey.endpointUri)),
            hasStatus(OK))
    }

    @Test
    fun `ordering a virology home test gets a 403`() {
        val virologyTest = env.configFor(VirologyKit, "virology_kit") as Submission
        assertThat(insecureClient(Request(POST, virologyTest.endpointUri.extend(Uri.of("/home-kit/order")))),
            hasStatus(FORBIDDEN))
    }

    @Test
    fun `registering a virology home test gets a 403`() {
        val virologyTest = env.configFor(VirologyKit, "virology_kit") as Submission
        assertThat(insecureClient(Request(POST, virologyTest.endpointUri.extend(Uri.of("/home-kit/register")))),
            hasStatus(FORBIDDEN))
    }

    @Test
    fun `poll for test result gets a 403`() {
        val virologyTest = env.configFor(VirologyKit, "virology_kit") as Submission
        assertThat(insecureClient(Request(POST, virologyTest.endpointUri.extend(Uri.of("/results")))),
            hasStatus(FORBIDDEN))
    }

    @Test
    fun `exchange cta token gets a 403`() {
        val virologyTest = env.configFor(VirologyKit, "virology_kit") as Submission
        assertThat(insecureClient(Request(POST, virologyTest.endpointUri.extend(Uri.of("/cta-exchange")))),
            hasStatus(FORBIDDEN))
    }
}
