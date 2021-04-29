package uk.nhs.nhsx.keyfederation

import com.amazonaws.services.lambda.runtime.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.events.InfoEvent
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.domain.BatchTag
import uk.nhs.nhsx.keyfederation.download.DiagnosisKeysDownloadResponse
import uk.nhs.nhsx.keyfederation.download.DiagnosisKeysDownloadService
import uk.nhs.nhsx.keyfederation.download.ExposureDownload
import uk.nhs.nhsx.keyfederation.download.NoContent
import uk.nhs.nhsx.domain.ReportType
import uk.nhs.nhsx.domain.TestType
import uk.nhs.nhsx.testhelper.mocks.FakeS3StorageMultipleObjects
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

class DiagnosisKeysDownloadServiceTest {

    private val fakeS3Storage = FakeS3StorageMultipleObjects()
    private val clock = { Instant.parse("2020-09-15T00:00:00Z") }
    private val recordingEvents = RecordingEvents()

    private val keyUploader = FederatedKeyUploader(
        fakeS3Storage,
        BucketName.of("some-bucket-name"),
        "federatedKeyPrefix",
        clock,
        listOf("GB-EAW"),
        recordingEvents
    )

    private val interopClient = mockk<InteropClient>()
    private val context = mockk<Context>()

    private val rollingStartNumber = LocalDateTime
        .ofInstant(clock().minus(1, ChronoUnit.HOURS), ZoneOffset.UTC)
        .toEpochSecond(ZoneOffset.UTC).toInt() / 600

    private val sep01 = LocalDate.of(2020, 9, 1)
    private val sep14 = LocalDate.of(2020, 9, 14)
    private val sep15 = LocalDate.of(2020, 9, 15)

    private val batch = DiagnosisKeysDownloadResponse(
        BatchTag.of("abc"),
        listOf(
            ExposureDownload("W2zb3BeMWt6Xr2u0ABG32Q==", rollingStartNumber, 0, 144, "GB-EAW", listOf("GB-EAW"), TestType.LAB_RESULT,
                ReportType.CONFIRMED_TEST,0)
        )
    )


    private val storedPayload1 = """{"temporaryExposureKeys":[{"key":"W2zb3BeMWt6Xr2u0ABG32Q==","rollingStartNumber":$rollingStartNumber,"rollingPeriod":144,"transmissionRisk":0,"daysSinceOnsetOfSymptoms":0}]}"""
    private val storedPayload2 = """{"temporaryExposureKeys":[{"key":"kzQt9Lf3xjtAlMtm7jkSqw==","rollingStartNumber":$rollingStartNumber,"rollingPeriod":144,"transmissionRisk":1,"daysSinceOnsetOfSymptoms":0}]}"""

    @Test
    fun testPostDownloadTransformations() {

        val service = DiagnosisKeysDownloadService(
            clock,
            interopClient,
            keyUploader,
            InMemoryBatchTagService(),
            true, 7,
            14,
            100,
            context,
            recordingEvents
        )
        val transformed = service.postDownloadTransformations(ExposureDownload("key",
            0,
            2,
            144,
            "origin",
            listOf("GB-EAW"),
            TestType.LAB_RESULT,
            ReportType.CONFIRMED_TEST,
            0))
        assertEquals(7, transformed.transmissionRiskLevel)
    }

    @Test
    fun `download keys first time and emits event`() {
        every { interopClient.downloadKeys(any()) } returns batch
        every { interopClient.downloadKeys(any(), any()) } returns NoContent
        every { context.remainingTimeInMillis } returns 10000

        val currentDay = LocalDate.of(2020, 9, 15)
        val fourteenDaysPrior = LocalDate.of(2020, 9, 1)
        val batchTagService = InMemoryBatchTagService()
        val service = DiagnosisKeysDownloadService(clock, interopClient, keyUploader, batchTagService, false, -1, 14, 1, context, recordingEvents)

        service.downloadFromFederatedServerAndStoreKeys()

        recordingEvents.containsExactly(DownloadedFederatedDiagnosisKeys::class, DownloadedExposures::class, InfoEvent::class)

        assertThat(batchTagService.batchTag!!.value).isEqualTo("abc")
        assertThat(batchTagService.batchDate).isEqualTo(currentDay)
        assertThat(fakeS3Storage.count).isEqualTo(1)
        assertThat(fakeS3Storage.fakeS3Objects.first().bytes.toUtf8String()).isEqualTo(storedPayload1)

        verify {
            interopClient.downloadKeys(fourteenDaysPrior)
        }
    }

    @Test
    fun `download keys with previous batch tag on previous day`() {
        every { interopClient.downloadKeys(any(), any()) } returns batch
        every { context.remainingTimeInMillis } returns 10000

        val batchTagService = InMemoryBatchTagService(BatchTag.of("xyz"), sep14)
        val service = DiagnosisKeysDownloadService(clock, interopClient, keyUploader, batchTagService, false, -1, 14, 1, context, recordingEvents)

        service.downloadFromFederatedServerAndStoreKeys()

        recordingEvents.containsExactly(DownloadedFederatedDiagnosisKeys::class, DownloadedExposures::class, InfoEvent::class)

        assertThat(batchTagService.batchTag!!.value).isEqualTo("abc")
        assertThat(batchTagService.batchDate).isEqualTo(sep15)
        assertThat(fakeS3Storage.count).isEqualTo(1)
        assertThat(fakeS3Storage.fakeS3Objects.first().bytes.toUtf8String()).isEqualTo(storedPayload1)

        verify {
            interopClient.downloadKeys(sep14, BatchTag.of("xyz"))
        }
    }

    @Test
    fun `download keys with previous batch tag on same day`() {
        every { interopClient.downloadKeys(any(), any()) } returns batch
        every { context.remainingTimeInMillis } returns 10000

        val batchTagService = InMemoryBatchTagService(BatchTag.of("xyz"), sep15)
        val service = DiagnosisKeysDownloadService(clock, interopClient, keyUploader, batchTagService, false, -1, 14, 1, context, recordingEvents)

        service.downloadFromFederatedServerAndStoreKeys()

        recordingEvents.containsExactly(DownloadedFederatedDiagnosisKeys::class, DownloadedExposures::class, InfoEvent::class)

        assertThat(batchTagService.batchTag!!.value).isEqualTo("abc")
        assertThat(batchTagService.batchDate).isEqualTo(sep15)
        assertThat(fakeS3Storage.count).isEqualTo(1)
        assertThat(fakeS3Storage.fakeS3Objects.first().bytes.toUtf8String()).isEqualTo(storedPayload1)
        verify {
            interopClient.downloadKeys(sep15, BatchTag.of("xyz"))
        }
    }

    @Test
    fun `download keys has no keys`() {
        every { interopClient.downloadKeys(any(), any()) } returns NoContent
        every { context.remainingTimeInMillis } returns 10000

        val batchTagService = InMemoryBatchTagService(BatchTag.of("xyz"), sep01)
        val service = DiagnosisKeysDownloadService(clock, interopClient, keyUploader, batchTagService, false, -1, 14, 100, context, recordingEvents)

        service.downloadFromFederatedServerAndStoreKeys()

        assertThat(batchTagService.batchTag!!.value).isEqualTo("xyz")
        assertThat(fakeS3Storage.count).isEqualTo(0)
        verify {
            interopClient.downloadKeys(sep01, BatchTag.of("xyz"))
        }
    }

    @Test
    fun `download keys and filter one valid region`() {
        every { interopClient.downloadKeys(any(), any()) } returns DiagnosisKeysDownloadResponse(
            BatchTag.of("abc"), listOf(
            ExposureDownload("W2zb3BeMWt6Xr2u0ABG32Q==", rollingStartNumber, 0, 144, "GB-EAW", listOf("GB-EAW"), TestType.LAB_RESULT,
                ReportType.CONFIRMED_TEST,0),
            ExposureDownload("kzQt9Lf3xjtAlMtm7jkSqw==", rollingStartNumber, 1, 144, "some-origin", listOf("UNKNOWN"), TestType.LAB_RESULT,
                ReportType.CONFIRMED_TEST,0),
            ExposureDownload("QHtCeDEgfmiPUtJWmyIzrw==", rollingStartNumber, 2, 144, "some-origin", listOf("UNKNOWN"), TestType.LAB_RESULT,
                ReportType.CONFIRMED_TEST,0)
        ))
        every { context.remainingTimeInMillis } returns 10000

        val batchTagService = InMemoryBatchTagService(BatchTag.of("xyz"), sep15)
        val service = DiagnosisKeysDownloadService(clock, interopClient, keyUploader, batchTagService, false, -1, 14, 1, context, recordingEvents)

        service.downloadFromFederatedServerAndStoreKeys()

        assertThat(batchTagService.batchTag!!.value).isEqualTo("abc")
        assertThat(batchTagService.batchDate).isEqualTo(sep15)
        assertThat(fakeS3Storage.count).isEqualTo(1)
        assertThat(fakeS3Storage.fakeS3Objects.first().bytes.toUtf8String()).isEqualTo(storedPayload1)
        verify {
            interopClient.downloadKeys(sep15, BatchTag.of("xyz"))
        }
    }

    @Test
    fun `download keys and filter multiple valid regions`() {
        every { interopClient.downloadKeys(any(), any()) } returns DiagnosisKeysDownloadResponse(
            BatchTag.of("abc"), listOf(
            ExposureDownload("W2zb3BeMWt6Xr2u0ABG32Q==", rollingStartNumber, 0, 144, "GB-EAW", listOf("GB-EAW"), TestType.LAB_RESULT,
                ReportType.CONFIRMED_TEST,0),
            ExposureDownload("kzQt9Lf3xjtAlMtm7jkSqw==", rollingStartNumber, 1, 144, "GB-SCO", listOf("GB-SCO"), TestType.LAB_RESULT,
                ReportType.CONFIRMED_TEST,0),
            ExposureDownload("QHtCeDEgfmiPUtJWmyIzrw==", rollingStartNumber, 2, 144, "some-origin", listOf("UNKNOWN"), TestType.LAB_RESULT,
                ReportType.CONFIRMED_TEST,0)
        ))
        every { context.remainingTimeInMillis } returns 10000

        val batchTagService = InMemoryBatchTagService(BatchTag.of("xyz"), sep15)
        val service = DiagnosisKeysDownloadService(clock, interopClient, FederatedKeyUploader(
            fakeS3Storage,
            BucketName.of("some-bucket-name"),
            "federatedKeyPrefix",
            clock,
            listOf("GB-EAW", "GB-SCO"),
            recordingEvents
        ), batchTagService, false, -1, 14, 1, context, recordingEvents)

        service.downloadFromFederatedServerAndStoreKeys()

        assertThat(batchTagService.batchTag!!.value).isEqualTo("abc")
        assertThat(batchTagService.batchDate).isEqualTo(sep15)
        assertThat(fakeS3Storage.count).isEqualTo(2)
        assertThat(fakeS3Storage.fakeS3Objects.map { it.bytes.toUtf8String() }).contains(storedPayload1, storedPayload2)
        verify {
            interopClient.downloadKeys(sep15, BatchTag.of("xyz"))
            interopClient.downloadKeys(sep15, BatchTag.of("abc"))
        }
    }


    @Test
    fun `stop the download loop if the remaining time is not sufficient`() {
        val currentDay = LocalDate.of(2020, 9, 15)
        val fourteenDaysPrior = LocalDate.of(2020, 9, 1)
        every { interopClient.downloadKeys(fourteenDaysPrior) } returns DiagnosisKeysDownloadResponse(
            BatchTag.of("75b326f7-ae6f-42f6-9354-00c0a6b797b3"), listOf(
            ExposureDownload("ogNW4Ra+Zdds1ShN56yv3w==", rollingStartNumber, 0, 144, "GB-EAW", listOf("some-region"), TestType.LAB_RESULT,
                ReportType.CONFIRMED_TEST,0),
            ExposureDownload("EwoHez3CQgdslvdxaf+ztw==", rollingStartNumber, 0, 144, "GB-EAW", listOf("some-region"), TestType.LAB_RESULT,
                ReportType.CONFIRMED_TEST,0)
        ))

        every { interopClient.downloadKeys(fourteenDaysPrior, BatchTag.of("75b326f7-ae6f-42f6-9354-00c0a6b797b3")) } returns DiagnosisKeysDownloadResponse(
            BatchTag.of("80e77dc6-8c27-42fb-8e38-1a0b1f51bf01"), listOf(
            ExposureDownload("xnGNbiVKd7xarkv9Gbdi5w==", rollingStartNumber, 0, 144, "GB-EAW", listOf("some-region"), TestType.LAB_RESULT,
                ReportType.CONFIRMED_TEST,0),
            ExposureDownload("ui0wpyxH4QaeIo9f6A6f7A==", rollingStartNumber, 0, 144, "GB-EAW", listOf("some-region"), TestType.LAB_RESULT,
                ReportType.CONFIRMED_TEST,0),
            ExposureDownload("MLSUh0NsJG/XIExJQJiqkg==", rollingStartNumber, 0, 144, "GB-EAW", listOf("some-region"), TestType.LAB_RESULT,
                ReportType.CONFIRMED_TEST,0)
        ))
        every { interopClient.downloadKeys(fourteenDaysPrior, BatchTag.of("80e77dc6-8c27-42fb-8e38-1a0b1f51bf01")) } returns NoContent
        every { context.remainingTimeInMillis } returns -2

        val batchTagService = InMemoryBatchTagService(null, currentDay)
        val downloadService = DiagnosisKeysDownloadService(clock, interopClient, FederatedKeyUploader(
            fakeS3Storage,
            BucketName.of("some-bucket-name"),
            "federatedKeyPrefix",
            clock,
            listOf("GB-EAW"),
            recordingEvents
        ), batchTagService, false, -1, 14, 5, context, recordingEvents)

        val batchesProcessed = downloadService.downloadFromFederatedServerAndStoreKeys()

        assertThat(batchTagService.batchTag!!.value).isEqualTo("75b326f7-ae6f-42f6-9354-00c0a6b797b3")
        assertThat(batchesProcessed).isEqualTo(1)
    }

    @Test
    fun `continue the download loop if we have time`() {
        val currentDay = LocalDate.of(2020, 9, 15)
        val fourteenDaysPrior = LocalDate.of(2020, 9, 1)
        every { interopClient.downloadKeys(fourteenDaysPrior) } returns DiagnosisKeysDownloadResponse(
            BatchTag.of("75b326f7-ae6f-42f6-9354-00c0a6b797b3"), listOf(
            ExposureDownload("ogNW4Ra+Zdds1ShN56yv3w==", rollingStartNumber, 0, 144, "GB-EAW", listOf("some-region"), TestType.LAB_RESULT,
                ReportType.CONFIRMED_TEST,0),
            ExposureDownload("EwoHez3CQgdslvdxaf+ztw==", rollingStartNumber, 0, 144, "GB-EAW", listOf("some-region"), TestType.LAB_RESULT,
                ReportType.CONFIRMED_TEST,0)
        ))

        every { interopClient.downloadKeys(fourteenDaysPrior, BatchTag.of("75b326f7-ae6f-42f6-9354-00c0a6b797b3")) } returns DiagnosisKeysDownloadResponse(
            BatchTag.of("80e77dc6-8c27-42fb-8e38-1a0b1f51bf01"), listOf(
            ExposureDownload("xnGNbiVKd7xarkv9Gbdi5w==", rollingStartNumber, 0, 144, "GB-EAW", listOf("some-region"), TestType.LAB_RESULT,
                ReportType.CONFIRMED_TEST,0),
            ExposureDownload("ui0wpyxH4QaeIo9f6A6f7A==", rollingStartNumber, 0, 144, "GB-EAW", listOf("some-region"), TestType.LAB_RESULT,
                ReportType.CONFIRMED_TEST,0),
            ExposureDownload("MLSUh0NsJG/XIExJQJiqkg==", rollingStartNumber, 0, 144, "GB-EAW", listOf("some-region"), TestType.LAB_RESULT,
                ReportType.CONFIRMED_TEST,0)
        ))
        every { interopClient.downloadKeys(fourteenDaysPrior, BatchTag.of("80e77dc6-8c27-42fb-8e38-1a0b1f51bf01")) } returns NoContent
        every { context.remainingTimeInMillis } returns 10000

        val batchTagService = InMemoryBatchTagService(null, currentDay)
        val downloadService = DiagnosisKeysDownloadService(clock, interopClient, FederatedKeyUploader(
            fakeS3Storage,
            BucketName.of("some-bucket-name"),
            "federatedKeyPrefix",
            clock,
            listOf("GB-EAW"),
            recordingEvents
        ), batchTagService, false, -1, 14, 5, context, recordingEvents)

        val batchesProcessed = downloadService.downloadFromFederatedServerAndStoreKeys()

        assertThat(batchTagService.batchTag!!.value).isEqualTo("80e77dc6-8c27-42fb-8e38-1a0b1f51bf01")
        assertThat(batchesProcessed).isEqualTo(2)
    }

}
