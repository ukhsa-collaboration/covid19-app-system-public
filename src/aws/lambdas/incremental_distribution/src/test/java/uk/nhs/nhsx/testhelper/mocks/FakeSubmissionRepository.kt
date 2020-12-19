package uk.nhs.nhsx.testhelper.mocks

import uk.nhs.nhsx.diagnosiskeydist.Submission
import uk.nhs.nhsx.diagnosiskeydist.SubmissionRepository
import uk.nhs.nhsx.diagnosiskeydist.agspec.ENIntervalNumber
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKey
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKeyPayload
import java.security.SecureRandom
import java.util.*

class FakeSubmissionRepository(submissionDates: List<Date>) : SubmissionRepository {

    private val submissions: List<Submission> = submissionDates.map { makeKeySet(it) }

    override fun loadAllSubmissions(minimalSubmissionTimeEpocMillisExclusive: Long,
                                    maxLimit: Int,
                                    maxResults: Int): List<Submission> = submissions

    private fun makeKeySet(submissionDate: Date): Submission {
        val mostRecentKeyRollingStart =
            ENIntervalNumber.enIntervalNumberFromTimestamp(submissionDate).enIntervalNumber / 144 * 144
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