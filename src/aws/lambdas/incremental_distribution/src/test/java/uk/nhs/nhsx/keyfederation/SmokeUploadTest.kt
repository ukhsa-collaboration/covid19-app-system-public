package uk.nhs.nhsx.keyfederation

import org.junit.Ignore
import org.junit.Test
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
import java.security.SecureRandom
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

@Ignore
class SmokeUploadTest {

    val INTEROP_BASE_URL = "https://localhost:8080"
    val PEM = "TBD"
    val AUTH_TOKEN = "TBD"

    init {
        Tracing.disableXRayComplaintsForMainClasses()
    }

    @Test
    fun `upload keys from s3 repository`() {
        // This test needs environment variable set to bucket name e.g. SUBMISSION_BUCKET_NAME=te-qa-diagnosis-keys-submission
        DiagnosisKeysUploadService(
            InteropClient(INTEROP_BASE_URL, AUTH_TOKEN, JWS(PEM)),
            SubmissionFromS3Repository(AwsS3Client()),
            InMemoryBatchTagService(),
            "GB-EAW"
        ).uploadRequest()
    }

    @Test
    fun `upload keys from s3 mock`() {
        DiagnosisKeysUploadService(
            InteropClient(INTEROP_BASE_URL, AUTH_TOKEN, JWS(PEM)),
            MockSubmissionRepository(listOf(Date.from(OffsetDateTime.now(ZoneOffset.UTC).toInstant()))),
            InMemoryBatchTagService(),
            "GB-EAW"
        ).uploadRequest()
    }

    @Test
    fun decode() {
        val decode = Base64.getDecoder().decode("ogNW4Ra+Zdds1ShN56yv3w==")
        print(decode.size)
    }

}

class MockSubmissionRepository(submissionDates: List<Date>) : SubmissionRepository {
    private val submissions: List<Submission> = submissionDates.map { makeKeySet(it) }

    override fun loadAllSubmissions(): List<Submission> = submissions

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