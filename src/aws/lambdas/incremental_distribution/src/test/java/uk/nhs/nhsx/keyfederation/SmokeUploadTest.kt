package uk.nhs.nhsx.keyfederation

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.ContextBuilder
import uk.nhs.nhsx.core.aws.s3.AwsS3Client
import uk.nhs.nhsx.core.aws.xray.Tracing
import uk.nhs.nhsx.diagnosiskeydist.Submission
import uk.nhs.nhsx.diagnosiskeydist.SubmissionRepository
import uk.nhs.nhsx.diagnosiskeydist.agspec.ENIntervalNumber
import uk.nhs.nhsx.diagnosiskeydist.s3.SubmissionFromS3Repository
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKey
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKeyPayload
import uk.nhs.nhsx.keyfederation.upload.DiagnosisKeysUploadService
import uk.nhs.nhsx.keyfederation.upload.JWS
import uk.nhs.nhsx.keyfederation.upload.KeyFederationUploadHandlerTest
import uk.nhs.nhsx.keyfederation.upload.KmsCompatibleSigner
import java.security.SecureRandom
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.Base64
import java.util.Date

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
            MockSubmissionRepository(listOf(Date.from(OffsetDateTime.now(ZoneOffset.UTC).toInstant()))),
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

class MockSubmissionRepository(submissionDates: List<Date>) : SubmissionRepository {
    private val submissions: List<Submission> = submissionDates.map { makeKeySet(it) }

    override fun loadAllSubmissions(minimalSubmissionTimeEpocMillisExclusive: Long, maxLimit: Int, maxResults: Int): List<Submission> = submissions

    private fun makeKeySet(submissionDate: Date): Submission {
        val mostRecentKeyRollingStart = ENIntervalNumber.enIntervalNumberFromTimestamp(submissionDate).enIntervalNumber / 144 * 144
        val keys = (0..14).map { makeKey(mostRecentKeyRollingStart - it * 144) }
        return Submission(submissionDate, StoredTemporaryExposureKeyPayload(keys))
    }

    private fun makeKey(keyStartTime: Long): StoredTemporaryExposureKey {
        val key = ByteArray(16)
        SecureRandom().nextBytes(key)
        val base64Key = Base64.getEncoder().encodeToString(key)
        return StoredTemporaryExposureKey(base64Key, Math.toIntExact(keyStartTime), 144, 7)
    }
}