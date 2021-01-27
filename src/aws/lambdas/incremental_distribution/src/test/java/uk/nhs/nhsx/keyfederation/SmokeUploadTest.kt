package uk.nhs.nhsx.keyfederation

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.aws.s3.AwsS3Client
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.xray.Tracing
import uk.nhs.nhsx.diagnosiskeydist.s3.SubmissionFromS3Repository
import uk.nhs.nhsx.keyfederation.TestKeyPairs.ecPrime256r1
import uk.nhs.nhsx.keyfederation.upload.DiagnosisKeysUploadService
import uk.nhs.nhsx.keyfederation.upload.JWS
import uk.nhs.nhsx.keyfederation.upload.KmsCompatibleSigner
import uk.nhs.nhsx.testhelper.ContextBuilder
import uk.nhs.nhsx.testhelper.mocks.FakeSubmissionRepository
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

@Disabled
class SmokeUploadTest {

    val INTEROP_BASE_URL = "https://localhost:8080"
    val AUTH_TOKEN = "TBD"

    init {
        Tracing.disableXRayComplaintsForMainClasses()
    }

    @Test
    fun `upload keys from s3 repository`() {
        DiagnosisKeysUploadService(
            InteropClient(INTEROP_BASE_URL, AUTH_TOKEN, JWS(KmsCompatibleSigner(ecPrime256r1.private))),
            SubmissionFromS3Repository(AwsS3Client(), { true }, BucketName.of("te-qa-diagnosis-keys-submission")),
            InMemoryBatchTagService(),
            "GB-EAW",
            false, -1,
            14, 0,
            100,
             ContextBuilder.aContext()
        ).loadKeysAndUploadToFederatedServer()
    }

    @Test
    fun `upload keys from s3 mock`() {
        DiagnosisKeysUploadService(
            InteropClient(INTEROP_BASE_URL, AUTH_TOKEN, JWS(KmsCompatibleSigner(ecPrime256r1.private))),
            FakeSubmissionRepository(listOf(Date.from(OffsetDateTime.now(ZoneOffset.UTC).toInstant()))),
            InMemoryBatchTagService(),
            "GB-EAW",
            false, -1,
            14, 0,
            100,
             ContextBuilder.aContext()
        ).loadKeysAndUploadToFederatedServer()
    }

    @Test
    fun decode() {
        print(Base64.getDecoder().decode("ogNW4Ra+Zdds1ShN56yv3w==").size)
    }
}

