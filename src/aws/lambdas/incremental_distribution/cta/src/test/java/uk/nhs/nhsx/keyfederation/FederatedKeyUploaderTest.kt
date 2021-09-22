package uk.nhs.nhsx.keyfederation

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.containsKeys
import strikt.assertions.getValue
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.withElementAt
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.diagnosiskeydist.agspec.ENIntervalNumber.Companion.enIntervalNumberFromTimestamp
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKey
import uk.nhs.nhsx.domain.BatchTag
import uk.nhs.nhsx.domain.ReportType.CONFIRMED_CLINICAL_DIAGNOSIS
import uk.nhs.nhsx.domain.ReportType.CONFIRMED_TEST
import uk.nhs.nhsx.domain.ReportType.UNKNOWN
import uk.nhs.nhsx.domain.TestType.LAB_RESULT
import uk.nhs.nhsx.domain.TestType.RAPID_RESULT
import uk.nhs.nhsx.keyfederation.download.DiagnosisKeysDownloadResponse
import uk.nhs.nhsx.keyfederation.download.ExposureDownload
import uk.nhs.nhsx.testhelper.assertions.S3ObjectAssertions.asString
import uk.nhs.nhsx.testhelper.assertions.S3ObjectAssertions.content
import uk.nhs.nhsx.testhelper.assertions.S3ObjectAssertions.key
import uk.nhs.nhsx.testhelper.assertions.containsExactly
import uk.nhs.nhsx.testhelper.assertions.isEqualToJson
import uk.nhs.nhsx.testhelper.assertions.isSameAs
import uk.nhs.nhsx.testhelper.data.TestData.STORED_FEDERATED_KEYS_PAYLOAD_IE
import uk.nhs.nhsx.testhelper.data.TestData.STORED_FEDERATED_KEYS_PAYLOAD_NI
import uk.nhs.nhsx.testhelper.mocks.FakeS3
import uk.nhs.nhsx.testhelper.mocks.getBucket
import uk.nhs.nhsx.testhelper.mocks.isEmpty
import java.time.Duration
import java.time.Instant
import java.util.*

class FederatedKeyUploaderTest {

    private val bucketName = BucketName.of(UUID.randomUUID().toString())
    private val s3Storage = FakeS3()
    private val clock = { Instant.parse("2020-09-15T00:00:00Z") }
    private val validOrigins = listOf("NI", "IE")
    private val events = RecordingEvents()

    private val uploader = FederatedKeyUploader(
        s3Storage = s3Storage,
        bucketName = bucketName,
        federatedKeySourcePrefix = "federatedKeyPrefix",
        clock = clock,
        validOrigins = validOrigins,
        events = events
    )

    private val now = clock()

    private val rollingStartNumber1 = enIntervalNumberFromTimestamp(now.minus(Duration.ofDays(1))).enIntervalNumber
    private val rollingStartNumber2 = enIntervalNumberFromTimestamp(now.minus(Duration.ofHours(1))).enIntervalNumber
    private val rollingStartNumber3 = enIntervalNumberFromTimestamp(now.minus(Duration.ofHours(2))).enIntervalNumber

    @Test
    fun `emits events`() {
        val exposures = listOf(
            ExposureDownload(
                keyData = "W2zb3BeMWt6Xr2u0ABG32Q==",
                rollingStartNumber = rollingStartNumber1.toInt(),
                transmissionRiskLevel = 3,
                rollingPeriod = 144,
                origin = "NI",
                regions = listOf("NI"),
                testType = LAB_RESULT,
                reportType = CONFIRMED_TEST,
                daysSinceOnset = 0
            ), ExposureDownload(
                keyData = "B3xb3BeMWt6Xr2u0ABG45F==",
                rollingStartNumber = rollingStartNumber2.toInt(),
                transmissionRiskLevel = 6,
                rollingPeriod = 144,
                origin = "NI",
                regions = listOf("NI"),
                testType = LAB_RESULT,
                reportType = CONFIRMED_TEST,
                daysSinceOnset = 0
            ),
            ExposureDownload(
                keyData = "kzQt9Lf3xjtAlMtm7jkSqw==",
                rollingStartNumber = rollingStartNumber3.toInt(),
                transmissionRiskLevel = 4,
                rollingPeriod = 144,
                origin = "IE",
                regions = listOf("IE"),
                testType = LAB_RESULT,
                reportType = CONFIRMED_TEST,
                daysSinceOnset = 0
            )
        )

        uploader.acceptKeysFromFederatedServer(DiagnosisKeysDownloadResponse(BatchTag.of("batchTag"), exposures))

        expectThat(events)
            .withElementAt(0) {
                isA<DownloadedFederatedDiagnosisKeys>().isEqualTo(
                    DownloadedFederatedDiagnosisKeys(
                        testType = LAB_RESULT,
                        validKeys = 2,
                        invalidKeys = 0,
                        origin = "NI"
                    )
                )
            }
            .withElementAt(1) {
                isA<DownloadedFederatedDiagnosisKeys>().isEqualTo(
                    DownloadedFederatedDiagnosisKeys(
                        testType = LAB_RESULT,
                        validKeys = 1,
                        invalidKeys = 0,
                        origin = "IE"
                    )
                )
            }
    }

    @Test
    fun `convert to stored model test`() {
        val federatedKey1 = ExposureDownload(
            keyData = "W2zb3BeMWt6Xr2u0ABG32Q==",
            rollingStartNumber = 5,
            transmissionRiskLevel = 3,
            rollingPeriod = 2,
            origin = "NI",
            regions = listOf("NI"),
            testType = LAB_RESULT,
            reportType = CONFIRMED_TEST,
            daysSinceOnset = 0
        )

        val federatedKey2 = ExposureDownload(
            keyData = "B3xb3BeMWt6Xr2u0ABG45F==",
            rollingStartNumber = 2,
            transmissionRiskLevel = 6,
            rollingPeriod = 4,
            origin = "NI",
            regions = listOf("NI"),
            testType = LAB_RESULT,
            reportType = CONFIRMED_TEST,
            daysSinceOnset = 0
        )

        val federatedKey3 = ExposureDownload(
            keyData = "kzQt9Lf3xjtAlMtm7jkSqw==",
            rollingStartNumber = 134,
            transmissionRiskLevel = 4,
            rollingPeriod = 222,
            origin = "IE",
            regions = listOf("IE"),
            testType = LAB_RESULT,
            reportType = CONFIRMED_TEST,
            daysSinceOnset = 0
        )

        val payload = DiagnosisKeysDownloadResponse(
            batchTag = BatchTag.of("batch-tag"),
            exposures = listOf(federatedKey1, federatedKey2, federatedKey3)
        )

        expectThat(uploader.groupByOrigin(payload)) {
            containsKeys("NI", "IE")
            getValue("NI").containsExactlyInAnyOrder(federatedKey1, federatedKey2)
            getValue("IE").containsExactlyInAnyOrder(federatedKey3)
        }
    }

    @Test
    fun converts() {
        val federatedKey1 = ExposureDownload(
            keyData = "W2zb3BeMWt6Xr2u0ABG32Q==",
            rollingStartNumber = 5,
            transmissionRiskLevel = 3,
            rollingPeriod = 2,
            origin = "NI",
            regions = listOf("NI"),
            testType = LAB_RESULT,
            reportType = CONFIRMED_TEST,
            daysSinceOnset = 0
        )

        expectThat(StoredTemporaryExposureKeyTransform(federatedKey1)).isEqualTo(
            StoredTemporaryExposureKey(
                key = "W2zb3BeMWt6Xr2u0ABG32Q==",
                rollingStartNumber = 5,
                rollingPeriod = 2,
                transmissionRisk = 3,
                daysSinceOnsetOfSymptoms = 0
            )
        )
    }

    @Test
    fun `accept temporary exposure keys from federated server`() {
        val objectKeyIE = ObjectKey.of("nearform/IE/20200915/batchTag.json")
        val objectKeyNI = ObjectKey.of("nearform/NI/20200915/batchTag.json")
        val keyUploader = FederatedKeyUploader(
            s3Storage = s3Storage,
            bucketName = bucketName,
            federatedKeySourcePrefix = "nearform",
            clock = clock,
            validOrigins = validOrigins,
            events = events
        )

        val exposures = listOf(
            ExposureDownload(
                keyData = "W2zb3BeMWt6Xr2u0ABG32Q==",
                rollingStartNumber = rollingStartNumber1.toInt(),
                transmissionRiskLevel = 3,
                rollingPeriod = 144,
                origin = "NI",
                regions = listOf("NI"),
                testType = LAB_RESULT,
                reportType = CONFIRMED_TEST,
                daysSinceOnset = 0
            ), ExposureDownload(
                keyData = "B3xb3BeMWt6Xr2u0ABG45F==",
                rollingStartNumber = rollingStartNumber2.toInt(),
                transmissionRiskLevel = 6,
                rollingPeriod = 144,
                origin = "NI",
                regions = listOf("NI"),
                testType = LAB_RESULT,
                reportType = CONFIRMED_TEST,
                daysSinceOnset = 0
            ),
            ExposureDownload(
                keyData = "kzQt9Lf3xjtAlMtm7jkSqw==",
                rollingStartNumber = rollingStartNumber3.toInt(),
                transmissionRiskLevel = 4,
                rollingPeriod = 144,
                origin = "IE",
                regions = listOf("IE"),
                testType = LAB_RESULT,
                reportType = CONFIRMED_TEST,
                daysSinceOnset = 0
            )
        )

        DiagnosisKeysDownloadResponse(BatchTag.of("batchTag"), exposures).also {
            keyUploader.acceptKeysFromFederatedServer(it)
        }

        expectThat(events).containsExactly(
            DownloadedFederatedDiagnosisKeys::class,
            DownloadedFederatedDiagnosisKeys::class
        )

        expectThat(s3Storage)
            .getBucket(bucketName)
            .hasSize(2)
            .and {
                withElementAt(0) {
                    key.isSameAs(objectKeyNI)
                    content.asString().isEqualToJson(STORED_FEDERATED_KEYS_PAYLOAD_NI)
                }
                withElementAt(1) {
                    key.isSameAs(objectKeyIE)
                    content.asString().isEqualToJson(STORED_FEDERATED_KEYS_PAYLOAD_IE)
                }
            }
    }

    @Test
    fun `reject key longer than 32 bytes`() {
        val payload = DiagnosisKeysDownloadResponse(
            batchTag = BatchTag.of("batchTag"),
            exposures = listOf(
                ExposureDownload(
                    keyData = "W2zb3BeMWt6Xr2u0ABG32Q==",
                    rollingStartNumber = rollingStartNumber1.toInt(),
                    transmissionRiskLevel = 7,
                    rollingPeriod = 144,
                    origin = "NI",
                    regions = listOf("NI"),
                    testType = LAB_RESULT,
                    reportType = CONFIRMED_TEST,
                    daysSinceOnset = 0
                ),
                ExposureDownload(
                    keyData = "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXpBQkNERUZHCg==",
                    rollingStartNumber = rollingStartNumber2.toInt(),
                    transmissionRiskLevel = 4,
                    rollingPeriod = 144,
                    origin = "NI",
                    regions = listOf("NI"),
                    testType = LAB_RESULT,
                    reportType = CONFIRMED_TEST,
                    daysSinceOnset = 0
                )
            )
        )

        uploader.acceptKeysFromFederatedServer(payload)

        expectThat(s3Storage).getBucket(bucketName).hasSize(1)
    }

    @Test
    fun `reject keys from future`() {
        val futureInstant1 = enIntervalNumberFromTimestamp(now.plus(Duration.ofDays(1))).enIntervalNumber
        val futureInstant2 = enIntervalNumberFromTimestamp(now.plus(Duration.ofHours(1))).enIntervalNumber
        val payload = DiagnosisKeysDownloadResponse(
            batchTag = BatchTag.of("batchTag"),
            exposures = listOf(
                ExposureDownload(
                    keyData = "W2zb3BeMWt6Xr2u0ABG32Q==",
                    rollingStartNumber = futureInstant1.toInt(),
                    transmissionRiskLevel = 7,
                    rollingPeriod = 144,
                    origin = "NI",
                    regions = listOf("NI"),
                    testType = LAB_RESULT,
                    reportType = CONFIRMED_TEST,
                    daysSinceOnset = 0
                ),
                ExposureDownload(
                    keyData = "W2zb3BeMWt6Xr2u0ABG32Q==",
                    rollingStartNumber = futureInstant2.toInt(),
                    transmissionRiskLevel = 4,
                    rollingPeriod = 144,
                    origin = "NI",
                    regions = listOf("NI"),
                    testType = LAB_RESULT,
                    reportType = CONFIRMED_TEST,
                    daysSinceOnset = 0
                )
            )
        )

        uploader.acceptKeysFromFederatedServer(payload)

        expectThat(s3Storage).isEmpty(bucketName)
    }

    @Test
    fun `reject expired keys`() {
        val pastInstant1 = enIntervalNumberFromTimestamp(now.minus(Duration.ofDays(20L))).enIntervalNumber
        val pastInstant2 = enIntervalNumberFromTimestamp(now.minus(Duration.ofHours((24L * 15L) + 1L))).enIntervalNumber
        val payload = DiagnosisKeysDownloadResponse(
            batchTag = BatchTag.of("batchTag"),
            exposures = listOf(
                ExposureDownload(
                    keyData = "W2zb3BeMWt6Xr2u0ABG32Q==",
                    rollingStartNumber = pastInstant1.toInt(),
                    transmissionRiskLevel = 7,
                    rollingPeriod = 144,
                    origin = "NI",
                    regions = listOf("NI"),
                    testType = LAB_RESULT,
                    reportType = CONFIRMED_TEST,
                    daysSinceOnset = 0
                ),
                ExposureDownload(
                    keyData = "kzQt9Lf3xjtAlMtm7jkSqw==",
                    rollingStartNumber = pastInstant2.toInt(),
                    transmissionRiskLevel = 4,
                    rollingPeriod = 144,
                    origin = "NI",
                    regions = listOf("NI"),
                    testType = LAB_RESULT,
                    reportType = CONFIRMED_TEST,
                    daysSinceOnset = 0
                )
            )
        )

        uploader.acceptKeysFromFederatedServer(payload)

        expectThat(s3Storage).isEmpty(bucketName)
    }

    @Test
    fun `reject non-PCR exposures`() {
        val payload = DiagnosisKeysDownloadResponse(
            batchTag = BatchTag.of("batchTag"),
            exposures = listOf(
                ExposureDownload(
                    keyData = "W2zb3BeMWt6Xr2u0ABG32Q==",
                    rollingStartNumber = rollingStartNumber1.toInt(),
                    transmissionRiskLevel = 3,
                    rollingPeriod = 144,
                    origin = "NI",
                    regions = listOf("NI"),
                    testType = RAPID_RESULT,
                    reportType = CONFIRMED_TEST,
                    daysSinceOnset = 0
                ),
                ExposureDownload(
                    keyData = "B3xb3BeMWt6Xr2u0ABG45F==",
                    rollingStartNumber = rollingStartNumber2.toInt(),
                    transmissionRiskLevel = 6,
                    rollingPeriod = 144,
                    origin = "NI",
                    regions = listOf("NI"),
                    testType = RAPID_RESULT,
                    reportType = CONFIRMED_TEST,
                    daysSinceOnset = 0
                ),
                ExposureDownload(
                    keyData = "kzQt9Lf3xjtAlMtm7jkSqw==",
                    rollingStartNumber = rollingStartNumber3.toInt(),
                    transmissionRiskLevel = 4,
                    rollingPeriod = 144,
                    origin = "IE",
                    regions = listOf("IE"),
                    testType = LAB_RESULT,
                    reportType = CONFIRMED_TEST,
                    daysSinceOnset = 0
                )
            )
        )

        uploader.acceptKeysFromFederatedServer(payload)

        expectThat(s3Storage).getBucket(bucketName).hasSize(1)
    }

    @Test
    fun `reject non-CONFIRMED_TEST exposures`() {
        val payload = DiagnosisKeysDownloadResponse(
            batchTag = BatchTag.of("batchTag"),
            exposures = listOf(
                ExposureDownload(
                    keyData = "W2zb3BeMWt6Xr2u0ABG32Q==",
                    rollingStartNumber = rollingStartNumber1.toInt(),
                    transmissionRiskLevel = 3,
                    rollingPeriod = 144,
                    origin = "NI",
                    regions = listOf("NI"),
                    testType = LAB_RESULT,
                    reportType = UNKNOWN,
                    daysSinceOnset = 0
                ),
                ExposureDownload(
                    keyData = "B3xb3BeMWt6Xr2u0ABG45F==",
                    rollingStartNumber = rollingStartNumber2.toInt(),
                    transmissionRiskLevel = 6,
                    rollingPeriod = 144,
                    origin = "NI",
                    regions = listOf("NI"),
                    testType = LAB_RESULT,
                    reportType = CONFIRMED_CLINICAL_DIAGNOSIS,
                    daysSinceOnset = 0
                ),
                ExposureDownload(
                    keyData = "kzQt9Lf3xjtAlMtm7jkSqw==",
                    rollingStartNumber = rollingStartNumber3.toInt(),
                    transmissionRiskLevel = 4,
                    rollingPeriod = 144,
                    origin = "IE",
                    regions = listOf("IE"),
                    testType = LAB_RESULT,
                    reportType = CONFIRMED_TEST,
                    daysSinceOnset = 0
                )
            )
        )

        uploader.acceptKeysFromFederatedServer(payload)

        expectThat(s3Storage).getBucket(bucketName).hasSize(1)
    }
}

