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
import uk.nhs.nhsx.keyfederation.client.HttpInteropClient
import uk.nhs.nhsx.keyfederation.upload.DiagnosisKeysUploadService
import uk.nhs.nhsx.keyfederation.upload.FederatedExposureUploadFactory
import uk.nhs.nhsx.keyfederation.upload.JWS
import uk.nhs.nhsx.keyfederation.upload.KmsCompatibleSigner
import uk.nhs.nhsx.testhelper.ContextBuilder
import uk.nhs.nhsx.testhelper.mocks.FakeSubmissionRepository
import java.time.Duration
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
            clock = SystemClock.CLOCK,
            interopClient = HttpInteropClient(
                interopBaseUrl = interopBaseUrl,
                authToken = authToken,
                jws = JWS(KmsCompatibleSigner(ecPrime256r1.private)),
                events = recordingEvents
            ),
            submissionRepository = SubmissionFromS3Repository(
                awsS3 = AwsS3Client(events = recordingEvents),
                objectKeyFilter = { true },
                submissionBucketName = BucketName.of("te-qa-diagnosis-keys-submission"),
                loadSubmissionsTimeout = Duration.ofMinutes(12),
                loadSubmissionsThreadPoolSize = 15,
                events = recordingEvents,
                clock = SystemClock.CLOCK
            ),
            batchTagService = InMemoryBatchTagService(),
            exposureUploadFactory = FederatedExposureUploadFactory("GB-EAW"),
            uploadRiskLevelDefaultEnabled = false,
            uploadRiskLevelDefault = -1,
            initialUploadHistoryDays = 14,
            maxUploadBatchSize = 0,
            maxSubsequentBatchUploadCount = 100,
            context = ContextBuilder.aContext(),
            events = recordingEvents
        ).loadKeysAndUploadToFederatedServer()
    }

    @Test
    fun `upload keys from s3 mock`() {
        DiagnosisKeysUploadService(
            clock = SystemClock.CLOCK,
            interopClient = HttpInteropClient(
                interopBaseUrl = interopBaseUrl,
                authToken = authToken,
                jws = JWS(KmsCompatibleSigner(ecPrime256r1.private)),
                events = recordingEvents
            ),
            submissionRepository = FakeSubmissionRepository(listOf(Instant.now())),
            batchTagService = InMemoryBatchTagService(),
            exposureUploadFactory = FederatedExposureUploadFactory("GB-EAW"),
            uploadRiskLevelDefaultEnabled = false,
            uploadRiskLevelDefault = -1,
            initialUploadHistoryDays = 14,
            maxUploadBatchSize = 0,
            maxSubsequentBatchUploadCount = 100,
            context = ContextBuilder.aContext(),
            events = recordingEvents
        ).loadKeysAndUploadToFederatedServer()
    }
}

