package uk.nhs.nhsx.keyfederation

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.Test
import org.mockito.Mockito
import uk.nhs.nhsx.analyticssubmission.FakeS3Storage
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.diagnosiskeyssubmission.model.ClientTemporaryExposureKey
import uk.nhs.nhsx.keyfederation.download.DiagnosisKeysDownloadResponse
import uk.nhs.nhsx.keyfederation.download.DiagnosisKeysDownloadService


class DiagnosisKeysDownloadServiceTest {

    private val keyUploader = KeyUploader(
        FakeS3Storage(),
        BucketName.of("some-bucket-name"),
    "federatedKeyPrefix"
    )

    private val interopClient = Mockito.mock(InteropClient::class.java)

    @Test
    fun `convert diagnosis keys downloaded into stored format`() {
        val federatedKey1 = Exposure("W2zb3BeMWt6Xr2u0ABG32Q==", 5, 3, 2, listOf())
        val federatedKey2 = Exposure("kzQt9Lf3xjtAlMtm7jkSqw==", 134, 4, 222, listOf())
        val federatedKeys: List<Exposure> = listOf(federatedKey1, federatedKey2)
        val expectedKey1 = ClientTemporaryExposureKey("W2zb3BeMWt6Xr2u0ABG32Q==", 5,2)
        val expectedKey2 = ClientTemporaryExposureKey("kzQt9Lf3xjtAlMtm7jkSqw==", 134,222)
        val expectedKeys: List<ClientTemporaryExposureKey> = listOf(expectedKey1, expectedKey2)
        val storedKeys = DiagnosisKeysDownloadService(interopClient, keyUploader, InMemoryBatchTagService()).convert(DiagnosisKeysDownloadResponse("dummy-batch-tag", federatedKeys))
        assertThat(storedKeys.temporaryExposureKeys, equalTo(expectedKeys))
    }


}