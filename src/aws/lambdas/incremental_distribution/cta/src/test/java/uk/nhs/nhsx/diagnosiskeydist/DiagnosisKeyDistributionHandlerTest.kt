package uk.nhs.nhsx.diagnosiskeydist

import com.amazonaws.services.kms.AWSKMS
import com.amazonaws.services.kms.model.SignResult
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.http4k.asByteBuffer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.SystemClock
import uk.nhs.nhsx.core.TestEnvironments
import uk.nhs.nhsx.core.aws.cloudfront.AwsCloudFrontClient
import uk.nhs.nhsx.core.aws.ssm.AwsSsmParameters
import uk.nhs.nhsx.core.aws.xray.Tracing
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.core.signature.KeyId
import uk.nhs.nhsx.testhelper.ContextBuilder.TestContext
import uk.nhs.nhsx.testhelper.mocks.FakeS3
import java.util.*

class DiagnosisKeyDistributionHandlerTest {

    @BeforeEach
    fun setup() = Tracing.disableXRayComplaintsForMainClasses()

    @Test
    fun `distributes keys`() {
        val env = TestEnvironments.TEST.apply(
            mapOf(
                "ABORT_OUTSIDE_TIME_WINDOW" to "false",
                "DISTRIBUTION_BUCKET_NAME" to "distribution-bucket-name",
                "DISTRIBUTION_ID" to UUID.randomUUID().toString(),
                "DISTRIBUTION_PATTERN_DAILY" to "/distribution/daily/*",
                "DISTRIBUTION_PATTERN_2HOURLY" to "/distribution/two-hourly/*",
                "SSM_AG_SIGNING_KEY_ID_PARAMETER_NAME" to "/app/kms/SigningKeyArn",
                "SSM_METADATA_SIGNING_KEY_ID_PARAMETER_NAME" to "/app/kms/ContentSigningKeyArn",
                "DIAGNOSIS_KEY_SUBMISSION_PREFIXES" to "nearform/JE,nearform/GB-SCT,nearform/GB-NIR,nearform/GI",
                "SUBMISSION_BUCKET_NAME" to "submission-bucket-name",
                "MOBILE_APP_BUNDLE_ID" to "mobile-app-bundle-id"
            )
        )

        val clock = SystemClock.CLOCK
        val events = RecordingEvents()
        val s3Client = FakeS3()
        val parameters = mockk<AwsSsmParameters>()
        val awsCloudFrontClient = mockk<AwsCloudFrontClient>()
        val awsKmsClient = mockk<AWSKMS>()

        every { parameters.parameter<KeyId>(any(), any()) } returns { KeyId.of("param") }
        every { awsKmsClient.sign(any()) } returns SignResult().withSignature("foobar".asByteBuffer())
        every { awsCloudFrontClient.invalidateCache(any(), any()) } returns Unit

        val handler = DiagnosisKeyDistributionHandler(
            env,
            clock,
            events,
            parameters,
            awsCloudFrontClient,
            s3Client,
            awsKmsClient
        )

        val response: String = handler.handleRequest(ScheduledEvent(), TestContext())

        events.contains(KeysDistributed::class)

        assertThat(response).isEqualTo("uk.nhs.nhsx.diagnosiskeydist.KeysDistributed")
    }
}
