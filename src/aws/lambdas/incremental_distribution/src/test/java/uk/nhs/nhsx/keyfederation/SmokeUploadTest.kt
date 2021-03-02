package uk.nhs.nhsx.keyfederation

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.SystemClock
import uk.nhs.nhsx.core.aws.s3.AwsS3Client
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.xray.Tracing
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.diagnosiskeydist.s3.SubmissionFromS3Repository
import uk.nhs.nhsx.keyfederation.TestKeyPairs.ecPrime256r1
import uk.nhs.nhsx.keyfederation.upload.DiagnosisKeysUploadService
import uk.nhs.nhsx.keyfederation.upload.JWS
import uk.nhs.nhsx.keyfederation.upload.KmsCompatibleSigner
import uk.nhs.nhsx.testhelper.ContextBuilder
import uk.nhs.nhsx.testhelper.mocks.FakeSubmissionRepository
import java.time.Instant

@Disabled
class SmokeUploadTest {

    private val interopBaseUrl = "https://localhost:8080"
    private val authToken = "TBD"

    init {
        Tracing.disableXRayComplaintsForMainClasses()
    }

    private val recordingEvents = RecordingEvents()

    @Test
    fun `upload keys from s3 repository`() {

        DiagnosisKeysUploadService(
            SystemClock.CLOCK,
            InteropClient(interopBaseUrl, authToken, JWS(KmsCompatibleSigner(ecPrime256r1.private)), recordingEvents),
            SubmissionFromS3Repository(
                AwsS3Client(recordingEvents),
                { true },
                BucketName.of("te-qa-diagnosis-keys-submission"),
                recordingEvents
            ),
            InMemoryBatchTagService(),
            "GB-EAW", false,
            -1, 14,
            0,
            100,
            ContextBuilder.aContext(),
            recordingEvents
        ).loadKeysAndUploadToFederatedServer()
    }

    @Test
    fun `upload keys from s3 mock`() {
        DiagnosisKeysUploadService(
            SystemClock.CLOCK,
            InteropClient(interopBaseUrl, authToken, JWS(KmsCompatibleSigner(ecPrime256r1.private)), recordingEvents),
            FakeSubmissionRepository(listOf(Instant.now())),
            InMemoryBatchTagService(),
            "GB-EAW", false,
            -1, 14,
            0,
            100,
            ContextBuilder.aContext(),
            recordingEvents
        ).loadKeysAndUploadToFederatedServer()
    }
}

