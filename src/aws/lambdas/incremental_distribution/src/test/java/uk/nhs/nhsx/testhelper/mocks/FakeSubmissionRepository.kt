package uk.nhs.nhsx.testhelper.mocks

import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.diagnosiskeydist.Submission
import uk.nhs.nhsx.diagnosiskeydist.SubmissionRepository
import uk.nhs.nhsx.diagnosiskeydist.agspec.ENIntervalNumber
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKey
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKeyPayload
import uk.nhs.nhsx.domain.TestType
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64

class FakeSubmissionRepository(submissionDates: List<Instant>) : SubmissionRepository {

    private val submissions: List<Submission> = submissionDates.map { makeKeySet(it) }

    override fun loadAllSubmissions(minimalSubmissionTimeEpochMillisExclusive: Long,
                                    limit: Int,
                                    maxResults: Int): List<Submission> = submissions

    private fun makeKeySet(submissionDate: Instant): Submission {
        val mostRecentKeyRollingStart = ENIntervalNumber.enIntervalNumberFromTimestamp(submissionDate).enIntervalNumber / 144 * 144
        val keys = (0..14).map { makeKey(mostRecentKeyRollingStart - it * 144) }
        return Submission(submissionDate, ObjectKey.of("mobile/LAB_RESULT/abc"), StoredTemporaryExposureKeyPayload(keys))
    }

    private fun makeKey(keyStartTime: Long): StoredTemporaryExposureKey {
        val key = ByteArray(16)
        SecureRandom().nextBytes(key)
        val base64Key = Base64.getEncoder().encodeToString(key)
        return StoredTemporaryExposureKey(base64Key, Math.toIntExact(keyStartTime), 144, 7)
    }
}
