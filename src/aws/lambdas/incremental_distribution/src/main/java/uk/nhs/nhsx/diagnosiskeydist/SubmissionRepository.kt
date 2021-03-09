package uk.nhs.nhsx.diagnosiskeydist

import uk.nhs.nhsx.core.Jackson.readJson
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKeyPayload
import java.io.IOException
import java.io.InputStream

interface SubmissionRepository {

    @Throws(Exception::class)
    fun loadAllSubmissions(
        minimalSubmissionTimeEpocMillisExclusive: Long,
        limit: Int,
        maxResults: Int
    ): List<Submission>

    companion object {
        @JvmStatic
        @Throws(IOException::class)
        fun getTemporaryExposureKeys(jsonInputStream: InputStream?) =
            readJson(jsonInputStream, StoredTemporaryExposureKeyPayload::class.java)
    }
}

@Throws(Exception::class)
fun SubmissionRepository.loadAllSubmissions() = loadAllSubmissions(0, Int.MAX_VALUE, Int.MAX_VALUE)
