package uk.nhs.nhsx.keyfederation

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.keyfederation.download.DiagnosisKeysDownloadResponse
import uk.nhs.nhsx.keyfederation.download.DiagnosisKeysDownloadService
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.function.Supplier

class DiagnosisKeysDownloadServiceTest {

    private val fakeS3Storage = FakeS3StorageMultipleObjects()

    private val clock = Supplier { Instant.parse("2020-09-15T00:00:00Z")}

    private val keyUploader = FederatedKeyUploader(
        fakeS3Storage,
        BucketName.of("some-bucket-name"),
        "federatedKeyPrefix",
        clock,
        listOf("GB-EAW")
    )

    private val interopClient = mockk<InteropClient>()

    private val rollingStartNumber = LocalDateTime.ofInstant(
        clock.get().minus(1, ChronoUnit.HOURS), ZoneOffset.UTC
    ).toEpochSecond(ZoneOffset.UTC).toInt() / 600


    private val sep01 = LocalDate.of(2020, 9, 1)
    private val sep14 = LocalDate.of(2020, 9, 14)
    private val sep15 = LocalDate.of(2020, 9, 15)

    private val batches = listOf(DiagnosisKeysDownloadResponse("abc", listOf(
        Exposure("W2zb3BeMWt6Xr2u0ABG32Q==", rollingStartNumber, 0, 144, listOf("GB-EAW"))
    )))

    private val storedPayload1 = "{\"temporaryExposureKeys\":[{\"key\":\"W2zb3BeMWt6Xr2u0ABG32Q==\",\"rollingStartNumber\":$rollingStartNumber,\"rollingPeriod\":144,\"transmissionRisk\":0}]}"
    private val storedPayload2 = "{\"temporaryExposureKeys\":[{\"key\":\"kzQt9Lf3xjtAlMtm7jkSqw==\",\"rollingStartNumber\":$rollingStartNumber,\"rollingPeriod\":144,\"transmissionRisk\":1}]}"

    @Test
    fun `download keys first time`() {
        every { interopClient.downloadKeys(any()) } returns batches

        val currentDay = LocalDate.of(2020, 9, 15)
        val fourteenDaysPrior = LocalDate.of(2020, 9, 1)
        val batchTagService = InMemoryBatchTagService()
        val service = DiagnosisKeysDownloadService(clock, interopClient, keyUploader, batchTagService)

        service.downloadAndSave()

        assertThat(batchTagService.batchTag!!.value).isEqualTo("abc")
        assertThat(batchTagService.batchDate).isEqualTo(currentDay)
        assertThat(fakeS3Storage.count).isEqualTo(1)
        assertThat(String(fakeS3Storage.fakeS3Objects.first().bytes.read(), StandardCharsets.UTF_8)).isEqualTo(storedPayload1)

        verify {
            interopClient.downloadKeys(fourteenDaysPrior)
        }

    }

    @Test
    fun `download keys with previous batch tag`() {
        every { interopClient.downloadKeys(any(), any()) } returns batches

        val batchTagService = InMemoryBatchTagService(BatchTag.of("xyz"), sep14)
        val service = DiagnosisKeysDownloadService(clock, interopClient, keyUploader, batchTagService)

        service.downloadAndSave()

        assertThat(batchTagService.batchTag!!.value).isEqualTo("abc")
        assertThat(batchTagService.batchDate).isEqualTo(sep15)
        assertThat(fakeS3Storage.count).isEqualTo(1)
        assertThat(String(fakeS3Storage.fakeS3Objects.first().bytes.read(), StandardCharsets.UTF_8)).isEqualTo(storedPayload1)
        verify {
            interopClient.downloadKeys(sep14, BatchTag.of("xyz"))
        }

    }


    @Test
    fun `download keys with previous batch tag on previous day`() {
        every { interopClient.downloadKeys(any(), any()) } returns batches

        val batchTagService = InMemoryBatchTagService(BatchTag.of("xyz"), sep14)
        val service = DiagnosisKeysDownloadService(clock, interopClient, keyUploader, batchTagService)

        service.downloadAndSave()

        assertThat(batchTagService.batchTag!!.value).isEqualTo("abc")
        assertThat(batchTagService.batchDate).isEqualTo(sep15)
        assertThat(fakeS3Storage.count).isEqualTo(1)
        assertThat(String(fakeS3Storage.fakeS3Objects.first().bytes.read(), StandardCharsets.UTF_8)).isEqualTo(storedPayload1)

        verify {
            interopClient.downloadKeys(sep14, BatchTag.of("xyz"))
        }

    }

    @Test
    fun `download keys with previous batch tag on same day`() {
        every { interopClient.downloadKeys(any(), any()) } returns batches

        val batchTagService = InMemoryBatchTagService(BatchTag.of("xyz"), sep15)
        val service = DiagnosisKeysDownloadService(clock, interopClient, keyUploader, batchTagService)

        service.downloadAndSave()

        assertThat(batchTagService.batchTag!!.value).isEqualTo("abc")
        assertThat(batchTagService.batchDate).isEqualTo(sep15)
        assertThat(fakeS3Storage.count).isEqualTo(1)
        assertThat(String(fakeS3Storage.fakeS3Objects.first().bytes.read(), StandardCharsets.UTF_8)).isEqualTo(storedPayload1)
        verify {
            interopClient.downloadKeys(sep15, BatchTag.of("xyz"))
        }

    }


    @Test
    fun `download keys has no keys`() {
        every { interopClient.downloadKeys(any(), any()) } returns emptyList()

        val batchTagService = InMemoryBatchTagService(BatchTag.of("xyz"), sep01)
        val service = DiagnosisKeysDownloadService(clock, interopClient, keyUploader, batchTagService)

        service.downloadAndSave()

        assertThat(batchTagService.batchTag!!.value).isEqualTo("xyz")
        assertThat(fakeS3Storage.count).isEqualTo(0)
        verify {
            interopClient.downloadKeys(sep01, BatchTag.of("xyz"))
        }

    }

    @Test
    fun `download keys and filter one valid region`() {
        every { interopClient.downloadKeys(any(), any()) } returns listOf(DiagnosisKeysDownloadResponse("abc", listOf(
            Exposure("W2zb3BeMWt6Xr2u0ABG32Q==", rollingStartNumber, 0, 144, listOf("GB-EAW")),
            Exposure("kzQt9Lf3xjtAlMtm7jkSqw==", rollingStartNumber, 1, 144, listOf("UNKNOWN")),
            Exposure("QHtCeDEgfmiPUtJWmyIzrw==", rollingStartNumber, 2, 144, listOf("UNKNOWN"))
        )))

        val batchTagService = InMemoryBatchTagService(BatchTag.of("xyz"), sep15)
        val service = DiagnosisKeysDownloadService(clock, interopClient, keyUploader, batchTagService)

        service.downloadAndSave()

        assertThat(batchTagService.batchTag!!.value).isEqualTo("abc")
        assertThat(batchTagService.batchDate).isEqualTo(sep15)
        assertThat(fakeS3Storage.count).isEqualTo(1)
        assertThat(String(fakeS3Storage.fakeS3Objects.first().bytes.read(), StandardCharsets.UTF_8)).isEqualTo(storedPayload1)
        verify {
            interopClient.downloadKeys(sep15, BatchTag.of("xyz"))
        }

    }

    @Test
    fun `download keys and filter multiple valid regions`() {
        every { interopClient.downloadKeys(any(), any()) } returns listOf(DiagnosisKeysDownloadResponse("abc", listOf(
            Exposure("W2zb3BeMWt6Xr2u0ABG32Q==", rollingStartNumber, 0, 144, listOf("GB-EAW")),
            Exposure("kzQt9Lf3xjtAlMtm7jkSqw==", rollingStartNumber, 1, 144, listOf("GB-SCO")),
            Exposure("QHtCeDEgfmiPUtJWmyIzrw==", rollingStartNumber, 2, 144, listOf("UNKNOWN"))
        )))

        val batchTagService = InMemoryBatchTagService(BatchTag.of("xyz"), sep15)
        val service = DiagnosisKeysDownloadService(clock, interopClient, FederatedKeyUploader(
            fakeS3Storage,
            BucketName.of("some-bucket-name"),
            "federatedKeyPrefix",
            clock,
            listOf("GB-EAW", "GB-SCO")
        ), batchTagService)

        service.downloadAndSave()

        assertThat(batchTagService.batchTag!!.value).isEqualTo("abc")
        assertThat(batchTagService.batchDate).isEqualTo(sep15)
        assertThat(fakeS3Storage.count).isEqualTo(2)
        assertThat(
            fakeS3Storage.fakeS3Objects.map { String(it.bytes.read(), StandardCharsets.UTF_8) }
        ).contains(storedPayload1, storedPayload2)
        verify {
            interopClient.downloadKeys(sep15, BatchTag.of("xyz"))
        }

    }

}