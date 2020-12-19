package uk.nhs.nhsx.keyfederation

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.aws.s3.AwsS3Client
import uk.nhs.nhsx.core.aws.xray.Tracing
import uk.nhs.nhsx.diagnosiskeydist.s3.SubmissionFromS3Repository
import uk.nhs.nhsx.keyfederation.upload.DiagnosisKeysUploadService
import uk.nhs.nhsx.keyfederation.upload.JWS
import uk.nhs.nhsx.keyfederation.upload.KeyFederationUploadHandlerTest
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

    private val privateKey = TestKeyPairs.ecPrime256r1.private

    @Test
    fun `upload keys from s3 repository`() {
        // This test needs environment variable set to bucket name e.g. SUBMISSION_BUCKET_NAME=te-qa-diagnosis-keys-submission
        DiagnosisKeysUploadService(
            InteropClient(INTEROP_BASE_URL, AUTH_TOKEN, JWS(KmsCompatibleSigner(KeyFederationUploadHandlerTest.keyPair.private))),
            SubmissionFromS3Repository(AwsS3Client()) { true },
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
            InteropClient(INTEROP_BASE_URL, AUTH_TOKEN, JWS(KmsCompatibleSigner(KeyFederationUploadHandlerTest.keyPair.private))),
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
        val decode = Base64.getDecoder().decode("ogNW4Ra+Zdds1ShN56yv3w==")
        print(decode.size)
    }

}

