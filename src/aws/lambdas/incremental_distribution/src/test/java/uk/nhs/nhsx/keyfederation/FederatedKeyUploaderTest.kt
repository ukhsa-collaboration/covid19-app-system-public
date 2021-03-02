package uk.nhs.nhsx.keyfederation

import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKey
import uk.nhs.nhsx.keyfederation.download.DiagnosisKeysDownloadResponse
import uk.nhs.nhsx.keyfederation.download.ExposureDownload
import uk.nhs.nhsx.testhelper.data.TestData
import uk.nhs.nhsx.testhelper.mocks.FakeS3StorageMultipleObjects
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.function.Supplier


class FederatedKeyUploaderTest {

    private val bucketName = BucketName.of("some-bucket-name")
    private val s3Storage = FakeS3StorageMultipleObjects()
    private val clock = Supplier { Instant.parse("2020-09-15T00:00:00Z") }
    private val validOrigins = listOf("NI", "IE")
    private val events = RecordingEvents()

    private val uploader = FederatedKeyUploader(s3Storage, bucketName, "federatedKeyPrefix", clock, validOrigins, events)

    @Test
    fun convertToStoredModelTest() {
        val federatedKey1 = ExposureDownload("W2zb3BeMWt6Xr2u0ABG32Q==", 5, 3, 2, "NI", listOf("NI"))
        val federatedKey2 = ExposureDownload("B3xb3BeMWt6Xr2u0ABG45F==", 2, 6, 4, "NI", listOf("NI"))
        val federatedKey3 = ExposureDownload("kzQt9Lf3xjtAlMtm7jkSqw==", 134, 4, 222, "IE", listOf("IE"))
        val storedKey1 = StoredTemporaryExposureKey("W2zb3BeMWt6Xr2u0ABG32Q==", 5, 2, 3)
        val storedKey2 = StoredTemporaryExposureKey("B3xb3BeMWt6Xr2u0ABG45F==", 2, 4, 6)
        val storedKey3 = StoredTemporaryExposureKey("kzQt9Lf3xjtAlMtm7jkSqw==", 134, 222, 4)
        val federatedKeys: List<ExposureDownload> = listOf(federatedKey1, federatedKey2, federatedKey3)
        val payload = DiagnosisKeysDownloadResponse("batch-tag", federatedKeys)
        val niKeys: List<StoredTemporaryExposureKey> = listOf(storedKey1, storedKey2)
        val ieKeys: List<StoredTemporaryExposureKey> = listOf(storedKey3)
        val expectedResponsesMap = HashMap<String, List<StoredTemporaryExposureKey>>()
        expectedResponsesMap["NI"] = niKeys
        expectedResponsesMap["IE"] = ieKeys
        val responsesMap = uploader.indexStoreModelKeysPerOrigin(payload)
        val regions = responsesMap.keys
        regions.forEach { region ->
            assertThat(expectedResponsesMap.containsKey(region)).isNotEqualTo(null)
            val expectedKeys: List<StoredTemporaryExposureKey>? = expectedResponsesMap[region]
            val actualKeys: List<StoredTemporaryExposureKey>? = responsesMap[region]
            assertThat(expectedKeys).containsExactlyInAnyOrderElementsOf(actualKeys)
        }
    }

    @Test
    fun `accept temporary exposure keys from federated server`() {
        val objectKeyIE = ObjectKey.of("nearform/IE/20200915/batchTag.json")
        val objectKeyNI = ObjectKey.of("nearform/NI/20200915/batchTag.json")
        val keyUploader = FederatedKeyUploader(s3Storage, bucketName, "nearform", clock, validOrigins, events)
        val rollingStartNumber1 = LocalDateTime.ofInstant(clock.get().minus(1, ChronoUnit.DAYS), ZoneOffset.UTC).toEpochSecond(ZoneOffset.UTC) / 600L
        val rollingStartNumber2 = LocalDateTime.ofInstant(clock.get().minus(1, ChronoUnit.HOURS), ZoneOffset.UTC).toEpochSecond(ZoneOffset.UTC) / 600L
        val rollingStartNumber3 = LocalDateTime.ofInstant(clock.get().minus(2, ChronoUnit.HOURS), ZoneOffset.UTC).toEpochSecond(ZoneOffset.UTC) / 600L
        val exposures: List<ExposureDownload> = listOf(
            ExposureDownload("W2zb3BeMWt6Xr2u0ABG32Q==", rollingStartNumber1.toInt(), 3, 144, "NI", listOf("NI")),
            ExposureDownload("B3xb3BeMWt6Xr2u0ABG45F==", rollingStartNumber2.toInt(), 6, 144, "NI", listOf("NI")),
            ExposureDownload("kzQt9Lf3xjtAlMtm7jkSqw==", rollingStartNumber3.toInt(), 4, 144, "IE", listOf("IE"))
        )
        val payload = DiagnosisKeysDownloadResponse("batchTag", exposures)
        keyUploader.acceptKeysFromFederatedServer(payload)

        events.containsExactly(DownloadedFederatedDiagnosisKeys::class, DownloadedFederatedDiagnosisKeys::class)

        val firstUpload = s3Storage.fakeS3Objects[0]
        val secondUpload = s3Storage.fakeS3Objects[1]
        assertThat(s3Storage.count, equalTo(2))
        assertThat(s3Storage.bucket, equalTo<BucketName>(bucketName))
        assertThat(firstUpload.name, equalTo(objectKeyNI))
        assertThat(secondUpload.name, equalTo(objectKeyIE))
        assertThat(firstUpload.bytes.toUtf8String(), equalTo(TestData.STORED_FEDERATED_KEYS_PAYLOAD_NI))
        assertThat(secondUpload.bytes.toUtf8String(), equalTo(TestData.STORED_FEDERATED_KEYS_PAYLOAD_IE))
    }

    @Test
    fun `reject key longer than 32 bytes`() {
        val rollingStartNumber1 = LocalDateTime.ofInstant(clock.get().minus(1, ChronoUnit.DAYS), ZoneOffset.UTC).toEpochSecond(ZoneOffset.UTC) / 600L
        val rollingStartNumber2 = LocalDateTime.ofInstant(clock.get().minus(1, ChronoUnit.HOURS), ZoneOffset.UTC).toEpochSecond(ZoneOffset.UTC) / 600L
        val payload = DiagnosisKeysDownloadResponse(
            "batchTag",
            listOf(
                ExposureDownload("W2zb3BeMWt6Xr2u0ABG32Q==", rollingStartNumber1.toInt(), 7, 144, "NI", listOf("NI")),
                ExposureDownload("YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXpBQkNERUZHCg==", rollingStartNumber2.toInt(), 4, 144, "NI", listOf("NI"))
            )
        )
        uploader.acceptKeysFromFederatedServer(payload)

        assertThat(s3Storage.count, equalTo(1))
    }

    @Test
    fun rejectKeysFromFuture() {
        val futureInstant1 = LocalDateTime.ofInstant(clock.get().plus(1, ChronoUnit.DAYS), ZoneOffset.UTC).toEpochSecond(ZoneOffset.UTC) / 600L
        val futureInstant2 = LocalDateTime.ofInstant(clock.get().plus(1, ChronoUnit.HOURS), ZoneOffset.UTC).toEpochSecond(ZoneOffset.UTC) / 600L
        val payload = DiagnosisKeysDownloadResponse(
            "batchTag",
            listOf(
                ExposureDownload("W2zb3BeMWt6Xr2u0ABG32Q==", futureInstant1.toInt(), 7, 144, "NI", listOf("NI")),
                ExposureDownload("W2zb3BeMWt6Xr2u0ABG32Q==", futureInstant2.toInt(), 4, 144, "NI", listOf("NI"))
            )
        )
        uploader.acceptKeysFromFederatedServer(payload)

        assertThat(s3Storage.count, equalTo(0))
    }

    @Test
    fun `reject expired keys`() {
        val pastInstant1 = LocalDateTime.ofInstant(clock.get().minus(20, ChronoUnit.DAYS), ZoneOffset.UTC).toEpochSecond(ZoneOffset.UTC) / 600L
        val pastInstant2 = LocalDateTime.ofInstant(clock.get().minus((24 * 15) + 1, ChronoUnit.HOURS), ZoneOffset.UTC).toEpochSecond(ZoneOffset.UTC) / 600L
        val payload = DiagnosisKeysDownloadResponse(
            "batchTag",
            listOf(
                ExposureDownload("W2zb3BeMWt6Xr2u0ABG32Q==", pastInstant1.toInt(), 7, 144, "NI", listOf("NI")),
                ExposureDownload("kzQt9Lf3xjtAlMtm7jkSqw==", pastInstant2.toInt(), 4, 144, "NI", listOf("NI"))
            )
        )
        uploader.acceptKeysFromFederatedServer(payload)

        assertThat(s3Storage.count, equalTo(0))
    }

}

