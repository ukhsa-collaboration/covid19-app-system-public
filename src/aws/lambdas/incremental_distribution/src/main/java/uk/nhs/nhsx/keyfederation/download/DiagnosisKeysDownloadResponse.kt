package uk.nhs.nhsx.keyfederation.download

sealed class InteropDownloadResponse
data class DiagnosisKeysDownloadResponse(val batchTag: String, val exposures: List<ExposureDownload>): InteropDownloadResponse()
object NoContent: InteropDownloadResponse()
