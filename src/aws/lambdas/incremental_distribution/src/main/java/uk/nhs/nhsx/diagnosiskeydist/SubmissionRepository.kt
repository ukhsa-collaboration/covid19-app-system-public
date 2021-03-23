package uk.nhs.nhsx.diagnosiskeydist

import uk.nhs.nhsx.core.Jackson.readJsonOrThrow
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKeyPayload
import java.io.InputStream

interface SubmissionRepository {

    fun loadAllSubmissions(
        minimalSubmissionTimeEpochMillisExclusive: Long,
        limit: Int,
        maxResults: Int
    ): List<Submission>

    companion object {
        fun getTemporaryExposureKeys(jsonInputStream: InputStream?) =
            readJsonOrThrow(jsonInputStream, StoredTemporaryExposureKeyPayload::class.java)
    }
}

fun SubmissionRepository.loadAllSubmissions() = loadAllSubmissions(0, Int.MAX_VALUE, Int.MAX_VALUE)
