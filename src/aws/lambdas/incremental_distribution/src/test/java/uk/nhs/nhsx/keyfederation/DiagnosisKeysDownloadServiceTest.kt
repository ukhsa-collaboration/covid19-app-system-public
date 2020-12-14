package uk.nhs.nhsx.keyfederation

import com.amazonaws.services.lambda.runtime.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.keyfederation.download.DiagnosisKeysDownloadResponse
import uk.nhs.nhsx.keyfederation.download.DiagnosisKeysDownloadService
import uk.nhs.nhsx.keyfederation.download.ExposureDownload
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.Optional.empty
import java.util.function.Supplier

class DiagnosisKeysDownloadServiceTest {

    private val fakeS3Storage = FakeS3StorageMultipleObjects()
    private val clock = Supplier { Instant.parse("2020-09-15T00:00:00Z") }

    private val keyUploader = FederatedKeyUploader(
        fakeS3Storage,
        BucketName.of("some-bucket-name"),
        "federatedKeyPrefix",
        clock,
        listOf("GB-EAW")
    )

    private val interopClient = mockk<InteropClient>()
    private val context = mockk<Context>()

    private val rollingStartNumber = LocalDateTime
        .ofInstant(clock.get().minus(1, ChronoUnit.HOURS), ZoneOffset.UTC)
        .toEpochSecond(ZoneOffset.UTC).toInt() / 600

    private val sep01 = LocalDate.of(2020, 9, 1)
    private val sep14 = LocalDate.of(2020, 9, 14)
    private val sep15 = LocalDate.of(2020, 9, 15)

    private val batch = Optional.of(
        DiagnosisKeysDownloadResponse("abc",
            listOf(
                ExposureDownload("W2zb3BeMWt6Xr2u0ABG32Q==", rollingStartNumber, 0, 144, "GB-EAW", listOf("GB-EAW"))
            )
        )
    )

    private val storedPayload1 = "{\"temporaryExposureKeys\":[{\"key\":\"W2zb3BeMWt6Xr2u0ABG32Q==\",\"rollingStartNumber\":$rollingStartNumber,\"rollingPeriod\":144,\"transmissionRisk\":0}]}"
    private val storedPayload2 = "{\"temporaryExposureKeys\":[{\"key\":\"kzQt9Lf3xjtAlMtm7jkSqw==\",\"rollingStartNumber\":$rollingStartNumber,\"rollingPeriod\":144,\"transmissionRisk\":1}]}"

    @Test
    fun testPostDownloadTransformations() {
        val service = DiagnosisKeysDownloadService(
            null,
            null,
            null,
            null,
            true, 7,
            14,
            100,
            null
        )
        val transformed = service.postDownloadTransformations(ExposureDownload("key",
            0,
            2,
            144,
            "origin",
            null))
        assertEquals(7, transformed.transmissionRiskLevel)
    }

    @Test
    fun `download keys first time`() {
        every { interopClient.getExposureKeysBatch(any(), any()) } returns batch
        every { context.remainingTimeInMillis } returns 10000

        val currentDay = LocalDate.of(2020, 9, 15)
        val fourteenDaysPrior = LocalDate.of(2020, 9, 1)
        val batchTagService = InMemoryBatchTagService()
        val service = DiagnosisKeysDownloadService(clock, interopClient, keyUploader, batchTagService, false, -1, 14, 1, context)

        service.downloadFromFederatedServerAndStoreKeys()

        assertThat(batchTagService.batchTag!!.value).isEqualTo("abc")
        assertThat(batchTagService.batchDate).isEqualTo(currentDay)
        assertThat(fakeS3Storage.count).isEqualTo(1)
        assertThat(String(fakeS3Storage.fakeS3Objects.first().bytes.read(), StandardCharsets.UTF_8)).isEqualTo(storedPayload1)

        verify {
            interopClient.getExposureKeysBatch(fourteenDaysPrior, "")
        }
    }

    @Test
    fun `download keys with previous batch tag on previous day`() {
        every { interopClient.getExposureKeysBatch(any(), any()) } returns batch
        every {context.remainingTimeInMillis} returns 10000

        val batchTagService = InMemoryBatchTagService(BatchTag.of("xyz"), sep14)
        val service = DiagnosisKeysDownloadService(clock, interopClient, keyUploader, batchTagService, false, -1, 14, 1, context)

        service.downloadFromFederatedServerAndStoreKeys()

        assertThat(batchTagService.batchTag!!.value).isEqualTo("abc")
        assertThat(batchTagService.batchDate).isEqualTo(sep15)
        assertThat(fakeS3Storage.count).isEqualTo(1)
        assertThat(String(fakeS3Storage.fakeS3Objects.first().bytes.read(), StandardCharsets.UTF_8)).isEqualTo(storedPayload1)

        verify {
            interopClient.getExposureKeysBatch(sep14, "?batchTag=xyz")
        }
    }

    @Test
    fun `download keys with previous batch tag on same day`() {
        every { interopClient.getExposureKeysBatch(any(), any()) } returns batch
        every {context.remainingTimeInMillis} returns 10000

        val batchTagService = InMemoryBatchTagService(BatchTag.of("xyz"), sep15)
        val service = DiagnosisKeysDownloadService(clock, interopClient, keyUploader, batchTagService, false, -1, 14, 1, context)

        service.downloadFromFederatedServerAndStoreKeys()

        assertThat(batchTagService.batchTag!!.value).isEqualTo("abc")
        assertThat(batchTagService.batchDate).isEqualTo(sep15)
        assertThat(fakeS3Storage.count).isEqualTo(1)
        assertThat(String(fakeS3Storage.fakeS3Objects.first().bytes.read(), StandardCharsets.UTF_8)).isEqualTo(storedPayload1)
        verify {
            interopClient.getExposureKeysBatch(sep15, "?batchTag=xyz")
        }
    }

    @Test
    fun `download keys has no keys`() {
        every { interopClient.getExposureKeysBatch(any(), any()) } returns empty()
        every {context.remainingTimeInMillis} returns 10000

        val batchTagService = InMemoryBatchTagService(BatchTag.of("xyz"), sep01)
        val service = DiagnosisKeysDownloadService(clock, interopClient, keyUploader, batchTagService, false, -1, 14, 100, context)

        service.downloadFromFederatedServerAndStoreKeys()

        assertThat(batchTagService.batchTag!!.value).isEqualTo("xyz")
        assertThat(fakeS3Storage.count).isEqualTo(0)
        verify {
            interopClient.getExposureKeysBatch(sep01, "?batchTag=xyz")
        }
    }

    @Test
    fun `download keys and filter one valid region`() {
        every { interopClient.getExposureKeysBatch(any(), any()) } returns Optional.of(DiagnosisKeysDownloadResponse("abc", listOf(
            ExposureDownload("W2zb3BeMWt6Xr2u0ABG32Q==", rollingStartNumber, 0, 144, "GB-EAW", listOf("GB-EAW")),
            ExposureDownload("kzQt9Lf3xjtAlMtm7jkSqw==", rollingStartNumber, 1, 144, "some-origin", listOf("UNKNOWN")),
            ExposureDownload("QHtCeDEgfmiPUtJWmyIzrw==", rollingStartNumber, 2, 144, "some-origin", listOf("UNKNOWN"))
        )))
        every {context.remainingTimeInMillis} returns 10000

        val batchTagService = InMemoryBatchTagService(BatchTag.of("xyz"), sep15)
        val service = DiagnosisKeysDownloadService(clock, interopClient, keyUploader, batchTagService, false, -1, 14, 1, context)

        service.downloadFromFederatedServerAndStoreKeys()

        assertThat(batchTagService.batchTag!!.value).isEqualTo("abc")
        assertThat(batchTagService.batchDate).isEqualTo(sep15)
        assertThat(fakeS3Storage.count).isEqualTo(1)
        assertThat(String(fakeS3Storage.fakeS3Objects.first().bytes.read(), StandardCharsets.UTF_8)).isEqualTo(storedPayload1)
        verify {
            interopClient.getExposureKeysBatch(sep15, "?batchTag=xyz")
        }
    }

    @Test
    fun `download keys and filter multiple valid regions`() {
        every { interopClient.getExposureKeysBatch(any(), any()) } returns Optional.of(DiagnosisKeysDownloadResponse("abc", listOf(
            ExposureDownload("W2zb3BeMWt6Xr2u0ABG32Q==", rollingStartNumber, 0, 144, "GB-EAW", listOf("GB-EAW")),
            ExposureDownload("kzQt9Lf3xjtAlMtm7jkSqw==", rollingStartNumber, 1, 144, "GB-SCO", listOf("GB-SCO")),
            ExposureDownload("QHtCeDEgfmiPUtJWmyIzrw==", rollingStartNumber, 2, 144, "some-origin", listOf("UNKNOWN"))
        )))
        every {context.remainingTimeInMillis} returns 10000

        val batchTagService = InMemoryBatchTagService(BatchTag.of("xyz"), sep15)
        val service = DiagnosisKeysDownloadService(clock, interopClient, FederatedKeyUploader(
            fakeS3Storage,
            BucketName.of("some-bucket-name"),
            "federatedKeyPrefix",
            clock,
            listOf("GB-EAW", "GB-SCO")
        ), batchTagService, false, -1, 14, 1, context)

        service.downloadFromFederatedServerAndStoreKeys()

        assertThat(batchTagService.batchTag!!.value).isEqualTo("abc")
        assertThat(batchTagService.batchDate).isEqualTo(sep15)
        assertThat(fakeS3Storage.count).isEqualTo(2)
        assertThat(
            fakeS3Storage.fakeS3Objects.map { String(it.bytes.read(), StandardCharsets.UTF_8) }
        ).contains(storedPayload1, storedPayload2)
        verify {
            interopClient.getExposureKeysBatch(sep15, "?batchTag=xyz")
            interopClient.getExposureKeysBatch(sep15, "?batchTag=abc")
        }
    }


    @Test
    fun `stop the download loop if the remaining time is not sufficient`() {
        val currentDay = LocalDate.of(2020, 9, 15)
        val fourteenDaysPrior = LocalDate.of(2020, 9, 1)
        every { interopClient.getExposureKeysBatch(fourteenDaysPrior, "") } returns Optional.of(DiagnosisKeysDownloadResponse("75b326f7-ae6f-42f6-9354-00c0a6b797b3", listOf(
            ExposureDownload("ogNW4Ra+Zdds1ShN56yv3w==", rollingStartNumber, 0, 144, "GB-EAW", listOf("some-region")),
            ExposureDownload("EwoHez3CQgdslvdxaf+ztw==", rollingStartNumber, 0, 144, "GB-EAW", listOf("some-region"))
        )))

        every { interopClient.getExposureKeysBatch(fourteenDaysPrior, "?batchTag=75b326f7-ae6f-42f6-9354-00c0a6b797b3") } returns Optional.of(DiagnosisKeysDownloadResponse("80e77dc6-8c27-42fb-8e38-1a0b1f51bf01", listOf(
            ExposureDownload("xnGNbiVKd7xarkv9Gbdi5w==", rollingStartNumber, 0, 144, "GB-EAW", listOf("some-region")),
            ExposureDownload("ui0wpyxH4QaeIo9f6A6f7A==", rollingStartNumber, 0, 144, "GB-EAW", listOf("some-region")),
            ExposureDownload("MLSUh0NsJG/XIExJQJiqkg==", rollingStartNumber, 0, 144, "GB-EAW", listOf("some-region"))
        )))
        every { interopClient.getExposureKeysBatch(fourteenDaysPrior, "?batchTag=80e77dc6-8c27-42fb-8e38-1a0b1f51bf01") } returns empty()
        every {context.remainingTimeInMillis} returns -2

        val batchTagService = InMemoryBatchTagService(null, currentDay)
        val downloadService = DiagnosisKeysDownloadService(clock, interopClient, FederatedKeyUploader(
            fakeS3Storage,
            BucketName.of("some-bucket-name"),
            "federatedKeyPrefix",
            clock,
            listOf("GB-EAW")
        ), batchTagService, false, -1, 14, 5, context)

        val batchesProcessed = downloadService.downloadFromFederatedServerAndStoreKeys()

        assertThat(batchTagService.batchTag!!.value).isEqualTo("75b326f7-ae6f-42f6-9354-00c0a6b797b3")
        assertThat(batchesProcessed).isEqualTo(1)
    }

    @Test
    fun `continue the download loop if we have time`() {
        val currentDay = LocalDate.of(2020, 9, 15)
        val fourteenDaysPrior = LocalDate.of(2020, 9, 1)
        every { interopClient.getExposureKeysBatch(fourteenDaysPrior, "") } returns Optional.of(DiagnosisKeysDownloadResponse("75b326f7-ae6f-42f6-9354-00c0a6b797b3", listOf(
            ExposureDownload("ogNW4Ra+Zdds1ShN56yv3w==", rollingStartNumber, 0, 144, "GB-EAW", listOf("some-region")),
            ExposureDownload("EwoHez3CQgdslvdxaf+ztw==", rollingStartNumber, 0, 144, "GB-EAW", listOf("some-region"))
        )))

        every { interopClient.getExposureKeysBatch(fourteenDaysPrior, "?batchTag=75b326f7-ae6f-42f6-9354-00c0a6b797b3") } returns Optional.of(DiagnosisKeysDownloadResponse("80e77dc6-8c27-42fb-8e38-1a0b1f51bf01", listOf(
            ExposureDownload("xnGNbiVKd7xarkv9Gbdi5w==", rollingStartNumber, 0, 144, "GB-EAW", listOf("some-region")),
            ExposureDownload("ui0wpyxH4QaeIo9f6A6f7A==", rollingStartNumber, 0, 144, "GB-EAW", listOf("some-region")),
            ExposureDownload("MLSUh0NsJG/XIExJQJiqkg==", rollingStartNumber, 0, 144, "GB-EAW", listOf("some-region"))
        )))
        every { interopClient.getExposureKeysBatch(fourteenDaysPrior, "?batchTag=80e77dc6-8c27-42fb-8e38-1a0b1f51bf01") } returns empty()
        every {context.remainingTimeInMillis} returns 10000

        val batchTagService = InMemoryBatchTagService(null, currentDay)
        val downloadService = DiagnosisKeysDownloadService(clock, interopClient, FederatedKeyUploader(
            fakeS3Storage,
            BucketName.of("some-bucket-name"),
            "federatedKeyPrefix",
            clock,
            listOf("GB-EAW")
        ), batchTagService, false, -1, 14, 5, context)

        val batchesProcessed = downloadService.downloadFromFederatedServerAndStoreKeys()

        assertThat(batchTagService.batchTag!!.value).isEqualTo("80e77dc6-8c27-42fb-8e38-1a0b1f51bf01")
        assertThat(batchesProcessed).isEqualTo(2)
    }

}