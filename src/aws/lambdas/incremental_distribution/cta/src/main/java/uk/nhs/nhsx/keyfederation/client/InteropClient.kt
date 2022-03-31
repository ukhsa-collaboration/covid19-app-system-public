package uk.nhs.nhsx.keyfederation.client

import uk.nhs.nhsx.domain.BatchTag
import java.time.LocalDate

interface InteropClient {
    fun downloadKeys(date: LocalDate, batchTag: BatchTag? = null): InteropDownloadResponse
    fun uploadKeys(keys: List<ExposureUpload>): InteropUploadResponse
}
