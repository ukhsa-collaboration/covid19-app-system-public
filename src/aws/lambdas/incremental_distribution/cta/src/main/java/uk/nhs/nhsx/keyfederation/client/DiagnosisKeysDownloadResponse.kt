package uk.nhs.nhsx.keyfederation.client

import uk.nhs.nhsx.domain.BatchTag
import uk.nhs.nhsx.keyfederation.download.ExposureDownload

sealed class InteropDownloadResponse
data class DiagnosisKeysDownloadResponse(
    val batchTag: BatchTag,
    val exposures: List<ExposureDownload>
): InteropDownloadResponse()
object NoContent: InteropDownloadResponse()
