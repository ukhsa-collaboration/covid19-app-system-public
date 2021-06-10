package uk.nhs.nhsx.keyfederation

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.diagnosiskeydist.agspec.ENIntervalNumber.Companion.enIntervalNumberFromTimestamp
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKey
import uk.nhs.nhsx.domain.BatchTag
import uk.nhs.nhsx.keyfederation.download.DiagnosisKeysDownloadResponse
import uk.nhs.nhsx.keyfederation.download.ExposureDownload
import uk.nhs.nhsx.domain.ReportType.CONFIRMED_CLINICAL_DIAGNOSIS
import uk.nhs.nhsx.domain.ReportType.CONFIRMED_TEST
import uk.nhs.nhsx.domain.ReportType.UNKNOWN
import uk.nhs.nhsx.domain.TestType.LAB_RESULT
import uk.nhs.nhsx.domain.TestType.RAPID_RESULT
import uk.nhs.nhsx.testhelper.data.TestData
import uk.nhs.nhsx.testhelper.mocks.FakeS3StorageMultipleObjects
import java.time.Duration
import java.time.Instant

class FederatedKeyUploaderTest {

    private val bucketName = BucketName.of("some-bucket-name")
    private val s3Storage = FakeS3StorageMultipleObjects()
    private val clock = { Instant.parse("2020-09-15T00:00:00Z") }
    private val validOrigins = listOf("NI", "IE")
    private val events = RecordingEvents()

    private val uploader = FederatedKeyUploader(
        s3Storage,
        bucketName,
        "federatedKeyPrefix",
        clock,
        validOrigins,
        events
    )

    private val now = clock()

    private val rollingStartNumber1 = enIntervalNumberFromTimestamp(now.minus(Duration.ofDays(1))).enIntervalNumber
    private val rollingStartNumber2 = enIntervalNumberFromTimestamp(now.minus(Duration.ofHours(1))).enIntervalNumber
    private val rollingStartNumber3 = enIntervalNumberFromTimestamp(now.minus(Duration.ofHours(2))).enIntervalNumber

    @Test
    fun `emits events`() {
        val exposures: List<ExposureDownload> = listOf(
            ExposureDownload(
                "W2zb3BeMWt6Xr2u0ABG32Q==",
                rollingStartNumber1.toInt(),
                3,
                144,
                "NI",
                listOf("NI"),
                LAB_RESULT,
                CONFIRMED_TEST,
                0
            ), ExposureDownload(
                "B3xb3BeMWt6Xr2u0ABG45F==",
                rollingStartNumber2.toInt(),
                6,
                144,
                "NI",
                listOf("NI"),
                LAB_RESULT,
                CONFIRMED_TEST,
                0
            ),
            ExposureDownload(
                "kzQt9Lf3xjtAlMtm7jkSqw==",
                rollingStartNumber3.toInt(),
                4,
                144,
                "IE",
                listOf("IE"),
                LAB_RESULT,
                CONFIRMED_TEST,
                0
            )
        )

        uploader.acceptKeysFromFederatedServer(DiagnosisKeysDownloadResponse(BatchTag.of("batchTag"), exposures))

        val iterator = events.iterator()
        val firstEvent = iterator.next() as DownloadedFederatedDiagnosisKeys
        val secondEvent = iterator.next() as DownloadedFederatedDiagnosisKeys

        assertThat(
            firstEvent,
            equalTo(DownloadedFederatedDiagnosisKeys(testType = LAB_RESULT, validKeys = 2, invalidKeys = 0, origin = "NI"))
        )
        assertThat(
            secondEvent,
            equalTo(DownloadedFederatedDiagnosisKeys(testType = LAB_RESULT, validKeys = 1, invalidKeys = 0, origin = "IE"))
        )
    }

    @Test
    fun `convert to stored model test`() {
        val federatedKey1 = ExposureDownload(
            "W2zb3BeMWt6Xr2u0ABG32Q==",
            5,
            3,
            2,
            "NI",
            listOf("NI"),
            LAB_RESULT,
            CONFIRMED_TEST,
            0
        )
        val federatedKey2 = ExposureDownload(
            "B3xb3BeMWt6Xr2u0ABG45F==",
            2,
            6,
            4,
            "NI",
            listOf("NI"),
            LAB_RESULT,
            CONFIRMED_TEST,
            0
        )
        val federatedKey3 = ExposureDownload(
            "kzQt9Lf3xjtAlMtm7jkSqw==",
            134,
            4,
            222,
            "IE",
            listOf("IE"),
            LAB_RESULT,
            CONFIRMED_TEST,
            0
        )

        val federatedKeys: List<ExposureDownload> = listOf(federatedKey1, federatedKey2, federatedKey3)
        val payload = DiagnosisKeysDownloadResponse(BatchTag.of("batch-tag"), federatedKeys)
        val niKeys: List<ExposureDownload> = listOf(federatedKey1, federatedKey2)
        val ieKeys: List<ExposureDownload> = listOf(federatedKey3)
        val expectedResponsesMap = HashMap<String, List<ExposureDownload>>()
        expectedResponsesMap["NI"] = niKeys
        expectedResponsesMap["IE"] = ieKeys

        val responsesMap = uploader.groupByOrigin(payload)
        responsesMap.keys.forEach { region ->
            assertThat(expectedResponsesMap.containsKey(region)).isNotEqualTo(null)
            val expectedKeys: List<ExposureDownload>? = expectedResponsesMap[region]
            val actualKeys: List<ExposureDownload>? = responsesMap[region]
            assertThat(expectedKeys).containsExactlyInAnyOrderElementsOf(actualKeys)
        }
    }

    @Test
    fun converts() {
        val federatedKey1 = ExposureDownload(
            "W2zb3BeMWt6Xr2u0ABG32Q==",
            5,
            3,
            2,
            "NI",
            listOf("NI"),
            LAB_RESULT,
            CONFIRMED_TEST,
            0
        )
        assertThat(
            StoredTemporaryExposureKeyTransform(federatedKey1), equalTo(
                StoredTemporaryExposureKey(
                    "W2zb3BeMWt6Xr2u0ABG32Q==",
                    5,
                    2,
                    3,
                    0
                )
            )
        )
    }

    @Test
    fun `accept temporary exposure keys from federated server`() {
        val objectKeyIE = ObjectKey.of("nearform/IE/20200915/batchTag.json")
        val objectKeyNI = ObjectKey.of("nearform/NI/20200915/batchTag.json")
        val keyUploader = FederatedKeyUploader(s3Storage, bucketName, "nearform", clock, validOrigins, events)
        val exposures: List<ExposureDownload> = listOf(
            ExposureDownload(
                "W2zb3BeMWt6Xr2u0ABG32Q==",
                rollingStartNumber1.toInt(),
                3,
                144,
                "NI",
                listOf("NI"),
                LAB_RESULT,
                CONFIRMED_TEST,
                0
            ), ExposureDownload(
                "B3xb3BeMWt6Xr2u0ABG45F==",
                rollingStartNumber2.toInt(),
                6,
                144,
                "NI",
                listOf("NI"),
                LAB_RESULT,
                CONFIRMED_TEST,
                0
            ),
            ExposureDownload(
                "kzQt9Lf3xjtAlMtm7jkSqw==",
                rollingStartNumber3.toInt(),
                4,
                144,
                "IE",
                listOf("IE"),
                LAB_RESULT,
                CONFIRMED_TEST,
                0
            )
        )

        val payload = DiagnosisKeysDownloadResponse(BatchTag.of("batchTag"), exposures)
        keyUploader.acceptKeysFromFederatedServer(payload)

        events.containsExactly(DownloadedFederatedDiagnosisKeys::class, DownloadedFederatedDiagnosisKeys::class)

        val firstUpload = s3Storage.fakeS3Objects[0]
        val secondUpload = s3Storage.fakeS3Objects[1]
        assertThat(s3Storage.count, equalTo(2))
        assertThat(s3Storage.bucket, equalTo(bucketName))
        assertThat(firstUpload.name, equalTo(objectKeyNI))
        assertThat(secondUpload.name, equalTo(objectKeyIE))
        assertThat(firstUpload.bytes.toUtf8String(), equalTo(TestData.STORED_FEDERATED_KEYS_PAYLOAD_NI))
        assertThat(secondUpload.bytes.toUtf8String(), equalTo(TestData.STORED_FEDERATED_KEYS_PAYLOAD_IE))
    }

    @Test
    fun `reject key longer than 32 bytes`() {
        val payload = DiagnosisKeysDownloadResponse(
            BatchTag.of("batchTag"),
            listOf(
                ExposureDownload(
                    "W2zb3BeMWt6Xr2u0ABG32Q==",
                    rollingStartNumber1.toInt(),
                    7,
                    144,
                    "NI",
                    listOf("NI"),
                    LAB_RESULT,
                    CONFIRMED_TEST,
                    0
                ),
                ExposureDownload(
                    "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXpBQkNERUZHCg==",
                    rollingStartNumber2.toInt(),
                    4,
                    144,
                    "NI",
                    listOf("NI"),
                    LAB_RESULT,
                    CONFIRMED_TEST,
                    0
                )
            )
        )

        uploader.acceptKeysFromFederatedServer(payload)

        assertThat(s3Storage.count, equalTo(1))
    }

    @Test
    fun `reject keys from future`() {
        val futureInstant1 = enIntervalNumberFromTimestamp(now.plus(Duration.ofDays(1))).enIntervalNumber
        val futureInstant2 = enIntervalNumberFromTimestamp(now.plus(Duration.ofHours(1))).enIntervalNumber
        val payload = DiagnosisKeysDownloadResponse(
            BatchTag.of("batchTag"),
            listOf(
                ExposureDownload(
                    "W2zb3BeMWt6Xr2u0ABG32Q==",
                    futureInstant1.toInt(),
                    7,
                    144,
                    "NI",
                    listOf("NI"),
                    LAB_RESULT,
                    CONFIRMED_TEST,
                    0
                ),
                ExposureDownload(
                    "W2zb3BeMWt6Xr2u0ABG32Q==",
                    futureInstant2.toInt(),
                    4,
                    144,
                    "NI",
                    listOf("NI"),
                    LAB_RESULT,
                    CONFIRMED_TEST,
                    0
                )
            )
        )
        uploader.acceptKeysFromFederatedServer(payload)

        assertThat(s3Storage.count, equalTo(0))
    }

    @Test
    fun `reject expired keys`() {
        val pastInstant1 = enIntervalNumberFromTimestamp(now.minus(Duration.ofDays(20))).enIntervalNumber
        val pastInstant2 = enIntervalNumberFromTimestamp(now.minus(Duration.ofHours((24 * 15) + 1))).enIntervalNumber
        val payload = DiagnosisKeysDownloadResponse(
            BatchTag.of("batchTag"),
            listOf(
                ExposureDownload(
                    "W2zb3BeMWt6Xr2u0ABG32Q==",
                    pastInstant1.toInt(),
                    7,
                    144,
                    "NI",
                    listOf("NI"),
                    LAB_RESULT,
                    CONFIRMED_TEST,
                    0
                ),
                ExposureDownload(
                    "kzQt9Lf3xjtAlMtm7jkSqw==",
                    pastInstant2.toInt(),
                    4,
                    144,
                    "NI",
                    listOf("NI"),
                    LAB_RESULT,
                    CONFIRMED_TEST,
                    0
                )
            )
        )
        uploader.acceptKeysFromFederatedServer(payload)

        assertThat(s3Storage.count, equalTo(0))
    }

    @Test
    fun `reject non-PCR exposures`() {
        val payload = DiagnosisKeysDownloadResponse(
            BatchTag.of("batchTag"),
            listOf(
                ExposureDownload(
                    "W2zb3BeMWt6Xr2u0ABG32Q==",
                    rollingStartNumber1.toInt(),
                    3,
                    144,
                    "NI",
                    listOf("NI"),
                    RAPID_RESULT,
                    CONFIRMED_TEST,
                    0
                ),
                ExposureDownload(
                    "B3xb3BeMWt6Xr2u0ABG45F==",
                    rollingStartNumber2.toInt(),
                    6,
                    144,
                    "NI",
                    listOf("NI"),
                    RAPID_RESULT,
                    CONFIRMED_TEST,
                    0
                ),
                ExposureDownload(
                    "kzQt9Lf3xjtAlMtm7jkSqw==",
                    rollingStartNumber3.toInt(),
                    4,
                    144,
                    "IE",
                    listOf("IE"),
                    LAB_RESULT,
                    CONFIRMED_TEST,
                    0
                )
            )
        )
        uploader.acceptKeysFromFederatedServer(payload)

        assertThat(s3Storage.count, equalTo(1))
    }

    @Test
    fun `reject non-CONFIRMED_TEST exposures`() {
        val payload = DiagnosisKeysDownloadResponse(
            BatchTag.of("batchTag"),
            listOf(
                ExposureDownload(
                    "W2zb3BeMWt6Xr2u0ABG32Q==",
                    rollingStartNumber1.toInt(),
                    3,
                    144,
                    "NI",
                    listOf("NI"),
                    LAB_RESULT,
                    UNKNOWN,
                    0
                ),
                ExposureDownload(
                    "B3xb3BeMWt6Xr2u0ABG45F==",
                    rollingStartNumber2.toInt(),
                    6,
                    144,
                    "NI",
                    listOf("NI"),
                    LAB_RESULT,
                    CONFIRMED_CLINICAL_DIAGNOSIS,
                    0
                ),
                ExposureDownload(
                    "kzQt9Lf3xjtAlMtm7jkSqw==",
                    rollingStartNumber3.toInt(),
                    4,
                    144,
                    "IE",
                    listOf("IE"),
                    LAB_RESULT,
                    CONFIRMED_TEST,
                    0
                )
            )
        )
        uploader.acceptKeysFromFederatedServer(payload)

        assertThat(s3Storage.count, equalTo(1))
    }
}

