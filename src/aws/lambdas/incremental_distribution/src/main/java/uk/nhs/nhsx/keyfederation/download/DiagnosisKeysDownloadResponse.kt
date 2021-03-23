package uk.nhs.nhsx.keyfederation.download

import uk.nhs.nhsx.keyfederation.BatchTag

sealed class InteropDownloadResponse
data class DiagnosisKeysDownloadResponse(val batchTag: BatchTag, val exposures: List<ExposureDownload>): InteropDownloadResponse()
object NoContent: InteropDownloadResponse()
