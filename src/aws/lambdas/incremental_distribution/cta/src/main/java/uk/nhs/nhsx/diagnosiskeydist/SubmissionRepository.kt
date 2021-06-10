package uk.nhs.nhsx.diagnosiskeydist

import uk.nhs.nhsx.core.Json.readJsonOrThrow
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKeyPayload
import java.io.InputStream

interface SubmissionRepository {

    fun loadAllSubmissions(
        minimalSubmissionTimeEpochMillisExclusive: Long = 0,
        limit: Int = Int.MAX_VALUE,
        maxResults: Int = Int.MAX_VALUE
    ): List<Submission>

    companion object {
        fun getTemporaryExposureKeys(jsonInputStream: InputStream?): StoredTemporaryExposureKeyPayload =
            readJsonOrThrow(jsonInputStream)
    }
}
