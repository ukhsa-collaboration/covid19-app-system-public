@file:Suppress("TestFunctionName")

package uk.nhs.nhsx.keyfederation

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import strikt.api.expect
import strikt.api.expectThat
import strikt.assertions.elementAt
import strikt.assertions.first
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.S3Storage
import uk.nhs.nhsx.core.events.InfoEvent
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.domain.BatchTag
import uk.nhs.nhsx.domain.ReportType.CONFIRMED_TEST
import uk.nhs.nhsx.domain.TestType.LAB_RESULT
import uk.nhs.nhsx.keyfederation.download.DiagnosisKeysDownloadResponse
import uk.nhs.nhsx.keyfederation.download.DiagnosisKeysDownloadService
import uk.nhs.nhsx.keyfederation.download.ExposureDownload
import uk.nhs.nhsx.keyfederation.download.NoContent
import uk.nhs.nhsx.testhelper.assertions.S3ObjectAssertions.asString
import uk.nhs.nhsx.testhelper.assertions.S3ObjectAssertions.content
import uk.nhs.nhsx.testhelper.assertions.containsExactly
import uk.nhs.nhsx.testhelper.assertions.isEqualToJson
import uk.nhs.nhsx.testhelper.mocks.FakeS3
import uk.nhs.nhsx.testhelper.mocks.getBucket
import uk.nhs.nhsx.testhelper.mocks.isEmpty
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset.UTC
import java.time.temporal.ChronoUnit.HOURS
import java.util.*

class DiagnosisKeysDownloadServiceTest {

    private val clock = { Instant.parse("2020-09-15T00:00:00Z") }
    private val events = RecordingEvents()

    private val fakeS3 = FakeS3()
    private val bucketName = BucketName.of(UUID.randomUUID().toString())
    private val keyUploader = FederatedKeyUploader(fakeS3, bucketName = bucketName)

    private val rollingStartNumber = LocalDateTime
        .ofInstant(clock().minus(1, HOURS), UTC)
        .toEpochSecond(UTC).toInt() / 600

    private val sep01 = LocalDate.of(2020, 9, 1)
    private val sep14 = LocalDate.of(2020, 9, 14)
    private val sep15 = LocalDate.of(2020, 9, 15)

    private val batch = DiagnosisKeysDownloadResponse(
        BatchTag.of("abc"),
        listOf(
            ExposureDownload(
                keyData = "W2zb3BeMWt6Xr2u0ABG32Q==",
                rollingStartNumber = rollingStartNumber,
                transmissionRiskLevel = 0,
                rollingPeriod = 144,
                origin = "GB-EAW",
                regions = listOf("GB-EAW"),
                testType = LAB_RESULT,
                reportType = CONFIRMED_TEST,
                daysSinceOnset = 0
            )
        )
    )

    private val storedPayload1 =
        """
        {
            "temporaryExposureKeys": [{
                "key": "W2zb3BeMWt6Xr2u0ABG32Q==",
                "rollingStartNumber":$rollingStartNumber,
                "rollingPeriod": 144,
                "transmissionRisk": 0,
                "daysSinceOnsetOfSymptoms": 0
            }]
        }
        """.trimIndent()

    private val storedPayload2 =
        """
        {
            "temporaryExposureKeys": [{
                "key": "kzQt9Lf3xjtAlMtm7jkSqw==",
                "rollingStartNumber": $rollingStartNumber,
                "rollingPeriod": 144,
                "transmissionRisk": 1,
                "daysSinceOnsetOfSymptoms": 0
            }]
        }
        """.trimIndent()

    @Test
    fun `test post download transformations`() {
        val service = DiagnosisKeysDownloadService(
            interopClient = mockk(),
            downloadRiskLevelDefaultEnabled = true,
            downloadRiskLevelDefault = 7,
            maxSubsequentBatchDownloadCount = 100,
        )

        val transformed: ExposureDownload = service.postDownloadTransformations(
            ExposureDownload(
                keyData = "key",
                rollingStartNumber = 0,
                transmissionRiskLevel = 2,
                rollingPeriod = 144,
                origin = "origin",
                regions = listOf("GB-EAW"),
                testType = LAB_RESULT,
                reportType = CONFIRMED_TEST,
                daysSinceOnset = 0
            )
        )

        expectThat(transformed)
            .get(ExposureDownload::transmissionRiskLevel)
            .isEqualTo(7)
    }

    @Test
    fun `download keys first time and emits event`() {
        val interopClient = mockk<InteropClient> {
            every { downloadKeys(any()) } returns batch
            every { downloadKeys(any(), any()) } returns NoContent
        }

        val batchTagService = InMemoryBatchTagService()

        val service = DiagnosisKeysDownloadService(
            interopClient = interopClient,
            keyUploader = keyUploader,
            batchTagService = batchTagService,
            downloadRiskLevelDefaultEnabled = false,
            downloadRiskLevelDefault = -1,
            maxSubsequentBatchDownloadCount = 1
        )

        service.downloadFromFederatedServerAndStoreKeys()

        val currentDay = LocalDate.of(2020, 9, 15)
        val fourteenDaysPrior = LocalDate.of(2020, 9, 1)

        expect {
            that(batchTagService) {
                get(InMemoryBatchTagService::batchTag).isEqualTo(BatchTag.of("abc"))
                get(InMemoryBatchTagService::batchDate).isEqualTo(currentDay)
            }

            that(fakeS3) {
                getBucket(bucketName)
                    .hasSize(1)
                    .first()
                    .content
                    .asString()
                    .isEqualToJson(storedPayload1)
            }
        }

        verify {
            interopClient.downloadKeys(fourteenDaysPrior)
        }

        expectThat(events).containsExactly(
            DownloadedFederatedDiagnosisKeys::class,
            DownloadedExposures::class,
            InfoEvent::class
        )
    }

    @Test
    fun `download keys with previous batch tag on previous day`() {
        val interopClient = mockk<InteropClient> {
            every { downloadKeys(any(), any()) } returns batch
        }

        val batchTag = BatchTag.of("xyz")

        val batchTagService = InMemoryBatchTagService(
            batchTag = batchTag,
            batchDate = sep14
        )

        val service = DiagnosisKeysDownloadService(
            interopClient = interopClient,
            keyUploader = keyUploader,
            batchTagService = batchTagService,
            downloadRiskLevelDefaultEnabled = false,
            downloadRiskLevelDefault = -1,
            maxSubsequentBatchDownloadCount = 1
        )

        service.downloadFromFederatedServerAndStoreKeys()

        expect {
            that(batchTagService) {
                get(InMemoryBatchTagService::batchTag).isEqualTo(BatchTag.of("abc"))
                get(InMemoryBatchTagService::batchDate).isEqualTo(sep15)
            }

            that(fakeS3) {
                getBucket(bucketName)
                    .hasSize(1)
                    .first()
                    .content
                    .asString()
                    .isEqualToJson(storedPayload1)
            }
        }

        verify {
            interopClient.downloadKeys(sep14, batchTag)
        }

        expectThat(events).containsExactly(
            DownloadedFederatedDiagnosisKeys::class,
            DownloadedExposures::class,
            InfoEvent::class
        )
    }

    @Test
    fun `download keys with previous batch tag on same day`() {
        val interopClient = mockk<InteropClient> {
            every { downloadKeys(any(), any()) } returns batch
        }

        val batchTag = BatchTag.of("xyz")

        val batchTagService = InMemoryBatchTagService(
            batchTag = batchTag,
            batchDate = sep15
        )

        val service = DiagnosisKeysDownloadService(
            interopClient = interopClient,
            keyUploader = keyUploader,
            batchTagService = batchTagService,
            downloadRiskLevelDefaultEnabled = false,
            downloadRiskLevelDefault = -1,
            maxSubsequentBatchDownloadCount = 1
        )

        service.downloadFromFederatedServerAndStoreKeys()

        expect {
            that(batchTagService) {
                get(InMemoryBatchTagService::batchTag).isEqualTo(BatchTag.of("abc"))
                get(InMemoryBatchTagService::batchDate).isEqualTo(sep15)
            }

            that(fakeS3) {
                getBucket(bucketName)
                    .hasSize(1)
                    .first()
                    .content
                    .asString()
                    .isEqualToJson(storedPayload1)
            }
        }

        verify {
            interopClient.downloadKeys(sep15, batchTag)
        }

        expectThat(events).containsExactly(
            DownloadedFederatedDiagnosisKeys::class,
            DownloadedExposures::class,
            InfoEvent::class
        )
    }

    @Test
    fun `download keys has no keys`() {
        val interopClient = mockk<InteropClient> {
            every { downloadKeys(any(), any()) } returns NoContent
        }

        val batchTag = BatchTag.of("xyz")

        val batchTagService = InMemoryBatchTagService(
            batchTag = batchTag,
            batchDate = sep01
        )

        val service = DiagnosisKeysDownloadService(
            interopClient = interopClient,
            keyUploader = keyUploader,
            batchTagService = batchTagService,
            downloadRiskLevelDefaultEnabled = false,
            downloadRiskLevelDefault = -1,
            maxSubsequentBatchDownloadCount = 100,
        )

        service.downloadFromFederatedServerAndStoreKeys()

        expect {
            that(batchTagService) {
                get(InMemoryBatchTagService::batchTag).isEqualTo(batchTag)
            }

            that(fakeS3).isEmpty(bucketName)
        }

        verify {
            interopClient.downloadKeys(sep01, batchTag)
        }
    }

    @Test
    fun `download keys and filter one valid region`() {
        val interopClient = mockk<InteropClient> {
            every { downloadKeys(any(), any()) } returns DiagnosisKeysDownloadResponse(
                batchTag = BatchTag.of("abc"),
                exposures = listOf(
                    ExposureDownload(
                        keyData = "W2zb3BeMWt6Xr2u0ABG32Q==",
                        rollingStartNumber = rollingStartNumber,
                        transmissionRiskLevel = 0,
                        rollingPeriod = 144,
                        origin = "GB-EAW",
                        regions = listOf("GB-EAW"),
                        testType = LAB_RESULT,
                        reportType = CONFIRMED_TEST,
                        daysSinceOnset = 0
                    ),
                    ExposureDownload(
                        keyData = "kzQt9Lf3xjtAlMtm7jkSqw==",
                        rollingStartNumber = rollingStartNumber,
                        transmissionRiskLevel = 1,
                        rollingPeriod = 144,
                        origin = "some-origin",
                        regions = listOf("UNKNOWN"),
                        testType = LAB_RESULT,
                        reportType = CONFIRMED_TEST,
                        daysSinceOnset = 0
                    ),
                    ExposureDownload(
                        keyData = "QHtCeDEgfmiPUtJWmyIzrw==",
                        rollingStartNumber = rollingStartNumber,
                        transmissionRiskLevel = 2,
                        rollingPeriod = 144,
                        origin = "some-origin",
                        regions = listOf("UNKNOWN"),
                        testType = LAB_RESULT,
                        reportType = CONFIRMED_TEST,
                        daysSinceOnset = 0
                    )
                )
            )
        }

        val batchTag = BatchTag.of("xyz")
        val batchTagService = InMemoryBatchTagService(batchTag, sep15)
        val service = DiagnosisKeysDownloadService(
            interopClient = interopClient,
            keyUploader = keyUploader,
            batchTagService = batchTagService,
            downloadRiskLevelDefaultEnabled = false,
            downloadRiskLevelDefault = -1,
            maxSubsequentBatchDownloadCount = 1
        )

        service.downloadFromFederatedServerAndStoreKeys()

        expect {
            that(batchTagService) {
                get(InMemoryBatchTagService::batchTag).isEqualTo(BatchTag.of("abc"))
                get(InMemoryBatchTagService::batchDate).isEqualTo(sep15)
            }

            that(fakeS3) {
                getBucket(bucketName)
                    .hasSize(1)
                    .first()
                    .content
                    .asString()
                    .isEqualToJson(storedPayload1)
            }
        }

        verify {
            interopClient.downloadKeys(sep15, batchTag)
        }
    }

    @Test
    fun `download keys and filter multiple valid regions`() {
        val interopClient = mockk<InteropClient> {
            every { downloadKeys(any(), any()) } returns DiagnosisKeysDownloadResponse(
                batchTag = BatchTag.of("abc"),
                exposures = listOf(
                    ExposureDownload(
                        keyData = "W2zb3BeMWt6Xr2u0ABG32Q==",
                        rollingStartNumber = rollingStartNumber,
                        transmissionRiskLevel = 0,
                        rollingPeriod = 144,
                        origin = "GB-EAW",
                        regions = listOf("GB-EAW"),
                        testType = LAB_RESULT,
                        reportType = CONFIRMED_TEST,
                        daysSinceOnset = 0
                    ),
                    ExposureDownload(
                        keyData = "kzQt9Lf3xjtAlMtm7jkSqw==",
                        rollingStartNumber = rollingStartNumber,
                        transmissionRiskLevel = 1,
                        rollingPeriod = 144,
                        origin = "GB-SCO",
                        regions = listOf("GB-SCO"),
                        testType = LAB_RESULT,
                        reportType = CONFIRMED_TEST,
                        daysSinceOnset = 0
                    ),
                    ExposureDownload(
                        keyData = "QHtCeDEgfmiPUtJWmyIzrw==",
                        rollingStartNumber = rollingStartNumber,
                        transmissionRiskLevel = 2,
                        rollingPeriod = 144,
                        origin = "some-origin",
                        regions = listOf("UNKNOWN"),
                        testType = LAB_RESULT,
                        reportType = CONFIRMED_TEST,
                        daysSinceOnset = 0
                    )
                )
            )
        }

        val bucketName = BucketName.of(UUID.randomUUID().toString())
        val batchTag = BatchTag.of("xyz")
        val batchTagService = InMemoryBatchTagService(batchTag, sep15)
        val keyUploader1 = FederatedKeyUploader(
            s3Storage = fakeS3,
            bucketName = bucketName,
            validOrigins = listOf("GB-EAW", "GB-SCO"),
        )

        val service = DiagnosisKeysDownloadService(
            interopClient = interopClient,
            keyUploader = keyUploader1,
            batchTagService = batchTagService,
            downloadRiskLevelDefaultEnabled = false,
            downloadRiskLevelDefault = -1,
            maxSubsequentBatchDownloadCount = 1
        )

        service.downloadFromFederatedServerAndStoreKeys()

        expect {
            that(batchTagService) {
                get(InMemoryBatchTagService::batchTag).isEqualTo(BatchTag.of("abc"))
                get(InMemoryBatchTagService::batchDate).isEqualTo(sep15)
            }

            that(fakeS3) {
                getBucket(bucketName).hasSize(2).and {
                    elementAt(0).content.asString().isEqualToJson(storedPayload1)
                    elementAt(1).content.asString().isEqualToJson(storedPayload2)
                }
            }
        }

        verify {
            interopClient.downloadKeys(sep15, batchTag)
            interopClient.downloadKeys(sep15, BatchTag.of("abc"))
        }
    }

    @Test
    fun `stop the download loop if the remaining time is not sufficient`() {
        val currentDay = LocalDate.of(2020, 9, 15)
        val fourteenDaysPrior = LocalDate.of(2020, 9, 1)

        val batchTag = BatchTag.of("75b326f7-ae6f-42f6-9354-00c0a6b797b3")

        val interopClient = mockk<InteropClient> {
            every { downloadKeys(fourteenDaysPrior) } returns DiagnosisKeysDownloadResponse(
                batchTag = batchTag,
                exposures = listOf(
                    ExposureDownload(
                        keyData = "ogNW4Ra+Zdds1ShN56yv3w==",
                        rollingStartNumber = rollingStartNumber,
                        transmissionRiskLevel = 0,
                        rollingPeriod = 144,
                        origin = "GB-EAW",
                        regions = listOf("some-region"),
                        testType = LAB_RESULT,
                        reportType = CONFIRMED_TEST,
                        daysSinceOnset = 0
                    ),
                    ExposureDownload(
                        keyData = "EwoHez3CQgdslvdxaf+ztw==",
                        rollingStartNumber = rollingStartNumber,
                        transmissionRiskLevel = 0,
                        rollingPeriod = 144,
                        origin = "GB-EAW",
                        regions = listOf("some-region"),
                        testType = LAB_RESULT,
                        reportType = CONFIRMED_TEST,
                        daysSinceOnset = 0
                    )
                )
            )

            every {
                downloadKeys(fourteenDaysPrior, batchTag)
            } returns DiagnosisKeysDownloadResponse(
                batchTag = BatchTag.of("80e77dc6-8c27-42fb-8e38-1a0b1f51bf01"),
                exposures = listOf(
                    ExposureDownload(
                        keyData = "xnGNbiVKd7xarkv9Gbdi5w==",
                        rollingStartNumber = rollingStartNumber,
                        transmissionRiskLevel = 0,
                        rollingPeriod = 144,
                        origin = "GB-EAW",
                        regions = listOf("some-region"),
                        testType = LAB_RESULT,
                        reportType = CONFIRMED_TEST,
                        daysSinceOnset = 0
                    ),
                    ExposureDownload(
                        keyData = "ui0wpyxH4QaeIo9f6A6f7A==",
                        rollingStartNumber = rollingStartNumber,
                        transmissionRiskLevel = 0,
                        rollingPeriod = 144,
                        origin = "GB-EAW",
                        regions = listOf("some-region"),
                        testType = LAB_RESULT,
                        reportType = CONFIRMED_TEST,
                        daysSinceOnset = 0
                    ),
                    ExposureDownload(
                        keyData = "MLSUh0NsJG/XIExJQJiqkg==",
                        rollingStartNumber = rollingStartNumber,
                        transmissionRiskLevel = 0,
                        rollingPeriod = 144,
                        origin = "GB-EAW",
                        regions = listOf("some-region"),
                        testType = LAB_RESULT,
                        reportType = CONFIRMED_TEST,
                        daysSinceOnset = 0
                    )
                )
            )

            every {
                downloadKeys(fourteenDaysPrior, BatchTag.of("80e77dc6-8c27-42fb-8e38-1a0b1f51bf01"))
            } returns NoContent
        }

        val batchTagService = InMemoryBatchTagService(null, currentDay)
        val keyUploader1 = FederatedKeyUploader(fakeS3)
        val downloadService = DiagnosisKeysDownloadService(
            interopClient = interopClient,
            keyUploader = keyUploader1,
            batchTagService = batchTagService,
            downloadRiskLevelDefaultEnabled = false,
            downloadRiskLevelDefault = -1,
            maxSubsequentBatchDownloadCount = 5,
            contextRemainingTimeInMillis = -2
        )

        val batchesProcessed = downloadService.downloadFromFederatedServerAndStoreKeys()

        expect {
            that(batchTagService).get(InMemoryBatchTagService::batchTag).isEqualTo(batchTag)
            that(batchesProcessed).isEqualTo(1)
        }
    }

    @Test
    fun `continue the download loop if we have time`() {
        val currentDay = LocalDate.of(2020, 9, 15)
        val fourteenDaysPrior = LocalDate.of(2020, 9, 1)

        val interopClient = mockk<InteropClient> {
            every { downloadKeys(fourteenDaysPrior) } returns DiagnosisKeysDownloadResponse(
                batchTag = BatchTag.of("75b326f7-ae6f-42f6-9354-00c0a6b797b3"),
                exposures = listOf(
                    ExposureDownload(
                        keyData = "ogNW4Ra+Zdds1ShN56yv3w==",
                        rollingStartNumber = rollingStartNumber,
                        transmissionRiskLevel = 0,
                        rollingPeriod = 144,
                        origin = "GB-EAW",
                        regions = listOf("some-region"),
                        testType = LAB_RESULT,
                        reportType = CONFIRMED_TEST,
                        daysSinceOnset = 0
                    ),
                    ExposureDownload(
                        keyData = "EwoHez3CQgdslvdxaf+ztw==",
                        rollingStartNumber = rollingStartNumber,
                        transmissionRiskLevel = 0,
                        rollingPeriod = 144,
                        origin = "GB-EAW",
                        regions = listOf("some-region"),
                        testType = LAB_RESULT,
                        reportType = CONFIRMED_TEST,
                        daysSinceOnset = 0
                    )
                )
            )

            every {
                downloadKeys(fourteenDaysPrior, BatchTag.of("75b326f7-ae6f-42f6-9354-00c0a6b797b3"))
            } returns DiagnosisKeysDownloadResponse(
                batchTag = BatchTag.of("80e77dc6-8c27-42fb-8e38-1a0b1f51bf01"),
                exposures = listOf(
                    ExposureDownload(
                        keyData = "xnGNbiVKd7xarkv9Gbdi5w==",
                        rollingStartNumber = rollingStartNumber,
                        transmissionRiskLevel = 0,
                        rollingPeriod = 144,
                        origin = "GB-EAW",
                        regions = listOf("some-region"),
                        testType = LAB_RESULT,
                        reportType = CONFIRMED_TEST,
                        daysSinceOnset = 0
                    ),
                    ExposureDownload(
                        keyData = "ui0wpyxH4QaeIo9f6A6f7A==",
                        rollingStartNumber = rollingStartNumber,
                        transmissionRiskLevel = 0,
                        rollingPeriod = 144,
                        origin = "GB-EAW",
                        regions = listOf("some-region"),
                        testType = LAB_RESULT,
                        reportType = CONFIRMED_TEST,
                        daysSinceOnset = 0
                    ),
                    ExposureDownload(
                        keyData = "MLSUh0NsJG/XIExJQJiqkg==",
                        rollingStartNumber = rollingStartNumber,
                        transmissionRiskLevel = 0,
                        rollingPeriod = 144,
                        origin = "GB-EAW",
                        regions = listOf("some-region"),
                        testType = LAB_RESULT,
                        reportType = CONFIRMED_TEST,
                        daysSinceOnset = 0
                    )
                )
            )

            every {
                downloadKeys(fourteenDaysPrior, BatchTag.of("80e77dc6-8c27-42fb-8e38-1a0b1f51bf01"))
            } returns NoContent
        }

        val batchTagService = InMemoryBatchTagService(null, currentDay)
        val keyUploader1 = FederatedKeyUploader(fakeS3)
        val downloadService = DiagnosisKeysDownloadService(
            interopClient = interopClient,
            keyUploader = keyUploader1,
            batchTagService = batchTagService,
            downloadRiskLevelDefaultEnabled = false,
            downloadRiskLevelDefault = -1,
            maxSubsequentBatchDownloadCount = 5
        )

        val batchesProcessed = downloadService.downloadFromFederatedServerAndStoreKeys()

        expect {
            that(batchTagService)
                .get(InMemoryBatchTagService::batchTag)
                .isEqualTo(BatchTag.of("80e77dc6-8c27-42fb-8e38-1a0b1f51bf01"))

            that(batchesProcessed).isEqualTo(2)
        }
    }

    private fun FederatedKeyUploader(
        s3Storage: S3Storage,
        validOrigins: List<String> = listOf("GB-EAW"),
        bucketName: BucketName = BucketName.of(UUID.randomUUID().toString())
    ) = FederatedKeyUploader(
        s3Storage = s3Storage,
        bucketName = bucketName,
        federatedKeySourcePrefix = "federatedKeyPrefix",
        validOrigins = validOrigins,
        clock = clock,
        events = events
    )

    private fun DiagnosisKeysDownloadService(
        interopClient: InteropClient,
        keyUploader: FederatedKeyUploader = FederatedKeyUploader(FakeS3()),
        batchTagService: InMemoryBatchTagService = InMemoryBatchTagService(),
        downloadRiskLevelDefaultEnabled: Boolean = false,
        downloadRiskLevelDefault: Int = -1,
        maxSubsequentBatchDownloadCount: Int = 5,
        contextRemainingTimeInMillis: Int = 10000
    ) = DiagnosisKeysDownloadService(
        clock = clock,
        interopClient = interopClient,
        keyUploader = keyUploader,
        batchTagService = batchTagService,
        downloadRiskLevelDefaultEnabled = downloadRiskLevelDefaultEnabled,
        downloadRiskLevelDefault = downloadRiskLevelDefault,
        initialDownloadHistoryDays = 14,
        maxSubsequentBatchDownloadCount = maxSubsequentBatchDownloadCount,
        context = mockk(relaxed = true) {
            every { remainingTimeInMillis } returns contextRemainingTimeInMillis
        },
        events = events
    )
}
