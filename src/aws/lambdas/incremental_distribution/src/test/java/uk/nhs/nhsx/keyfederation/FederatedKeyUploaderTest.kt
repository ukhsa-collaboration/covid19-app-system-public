package uk.nhs.nhsx.keyfederation

import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import uk.nhs.nhsx.TestData
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKey
import uk.nhs.nhsx.keyfederation.download.DiagnosisKeysDownloadResponse
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.function.Supplier

class FederatedKeyUploaderTest {
    private val bucketName = BucketName.of("some-bucket-name")
    private val s3Storage = FakeS3StorageMultipleObjects()
    private val clock = Supplier { Instant.parse("2020-09-15T00:00:00Z")}
    private val validRegions = listOf("NI", "IE")

    @Test
    fun convertToStoredModelTest(){
        val  uploader = FederatedKeyUploader(s3Storage, bucketName, "federatedKeyPrefix", clock, validRegions)
        val federatedKey1 = Exposure("W2zb3BeMWt6Xr2u0ABG32Q==", 5, 3, 2, listOf("NI"))
        val federatedKey2 = Exposure("B3xb3BeMWt6Xr2u0ABG45F==", 2, 6, 4, listOf("NI"))
        val federatedKey3 = Exposure("kzQt9Lf3xjtAlMtm7jkSqw==", 134, 4, 222, listOf("IE"))
        val storedKey1 = StoredTemporaryExposureKey("W2zb3BeMWt6Xr2u0ABG32Q==", 5, 2, 3)
        val storedKey2 = StoredTemporaryExposureKey("B3xb3BeMWt6Xr2u0ABG45F==", 2, 4, 6)
        val storedKey3 = StoredTemporaryExposureKey("kzQt9Lf3xjtAlMtm7jkSqw==", 134, 222, 4)
        val federatedKeys: List<Exposure> = listOf(federatedKey1, federatedKey2, federatedKey3)
        val payload = DiagnosisKeysDownloadResponse("batch-tag", federatedKeys)
        val niKeys: List<StoredTemporaryExposureKey> = listOf(storedKey1, storedKey2)
        val ieKeys: List<StoredTemporaryExposureKey> = listOf(storedKey3)
        val expectedResponsesMap = HashMap<String, List<StoredTemporaryExposureKey>>()
        expectedResponsesMap["NI"] = niKeys
        expectedResponsesMap["IE"] = ieKeys
        val responsesMap = uploader.convertToStoredModel(payload)
        val regions = responsesMap.keys
        regions.forEach{ region ->
            assertThat(expectedResponsesMap.containsKey(region)).isNotEqualTo(null)
            val expectedKeys: List<StoredTemporaryExposureKey>? = expectedResponsesMap[region]
            val actualKeys: List<StoredTemporaryExposureKey>? = responsesMap[region]
            assertThat(expectedKeys).containsExactlyInAnyOrderElementsOf(actualKeys)
        }

    }


    @Test
    fun acceptTemporaryExposureKeysFromFederatedServer() {
        val objectKeyIE = ObjectKey.of("nearform/IE/20200915/batchTag.json")
        val objectKeyNI = ObjectKey.of("nearform/NI/20200915/batchTag.json")
        val keyUploader = FederatedKeyUploader(s3Storage, bucketName, "nearform", clock, validRegions)
        val rollingStartNumber1 = LocalDateTime.ofInstant(clock.get().minus(1, ChronoUnit.DAYS), ZoneOffset.UTC).toEpochSecond(ZoneOffset.UTC) / 600L
        val rollingStartNumber2 = LocalDateTime.ofInstant(clock.get().minus(1, ChronoUnit.HOURS), ZoneOffset.UTC).toEpochSecond(ZoneOffset.UTC) / 600L
        val rollingStartNumber3 = LocalDateTime.ofInstant(clock.get().minus(2, ChronoUnit.HOURS), ZoneOffset.UTC).toEpochSecond(ZoneOffset.UTC) / 600L
        val exposures: List<Exposure> = listOf(
            Exposure("W2zb3BeMWt6Xr2u0ABG32Q==", rollingStartNumber1.toInt(), 3, 144, listOf("NI")),
            Exposure("B3xb3BeMWt6Xr2u0ABG45F==", rollingStartNumber2.toInt(), 6, 144, listOf("NI")),
            Exposure("kzQt9Lf3xjtAlMtm7jkSqw==", rollingStartNumber3.toInt(), 4, 144, listOf("IE"))
        )
        val payload = DiagnosisKeysDownloadResponse("batchTag", exposures)
        keyUploader.acceptKeysFromFederatedServer(payload)

        val firstUpload = s3Storage.fakeS3Objects[0]
        val secondUpload = s3Storage.fakeS3Objects[1]
        assertThat(s3Storage.count, equalTo(2))
        assertThat(s3Storage.bucket, equalTo<BucketName>(bucketName))
        assertThat(firstUpload.name, equalTo<ObjectKey>(objectKeyNI))
        assertThat(secondUpload.name, equalTo<ObjectKey>(objectKeyIE))
        assertThat(String(firstUpload.bytes.read(), StandardCharsets.UTF_8), equalTo(TestData.STORED_FEDERATED_KEYS_PAYLOAD_NI))
        assertThat(String(secondUpload.bytes.read(), StandardCharsets.UTF_8), equalTo(TestData.STORED_FEDERATED_KEYS_PAYLOAD_IE))
    }

    @Test
    fun rejectKeyLongerThan32Bytes(){
        val keyUploader = FederatedKeyUploader(s3Storage, bucketName, "federatedKeyPrefix", clock, validRegions)
        val rollingStartNumber1 = LocalDateTime.ofInstant(clock.get().minus(1, ChronoUnit.DAYS), ZoneOffset.UTC).toEpochSecond(ZoneOffset.UTC) / 600L
        val rollingStartNumber2 = LocalDateTime.ofInstant(clock.get().minus(1, ChronoUnit.HOURS), ZoneOffset.UTC).toEpochSecond(ZoneOffset.UTC) / 600L
        val payload = DiagnosisKeysDownloadResponse(
            "batchTag",
            listOf(
                Exposure("W2zb3BeMWt6Xr2u0ABG32Q==", rollingStartNumber1.toInt(), 7,144, listOf("NI")),
                Exposure("YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXpBQkNERUZHCg==", rollingStartNumber2.toInt(), 4,144, listOf("NI"))
            )
        )
        keyUploader.acceptKeysFromFederatedServer(payload)

        assertThat(s3Storage.count, equalTo(1))
    }

    @Test
    fun rejectKeysFromFuture() {
        val keyUploader = FederatedKeyUploader(s3Storage, bucketName, "federatedKeyPrefix", clock, validRegions)
        val futureInstant1 = LocalDateTime.ofInstant(clock.get().plus(1, ChronoUnit.DAYS), ZoneOffset.UTC).toEpochSecond(ZoneOffset.UTC) / 600L
        val futureInstant2 = LocalDateTime.ofInstant(clock.get().plus(1, ChronoUnit.HOURS), ZoneOffset.UTC).toEpochSecond(ZoneOffset.UTC) / 600L
        val payload = DiagnosisKeysDownloadResponse(
            "batchTag",
            listOf(
                Exposure("W2zb3BeMWt6Xr2u0ABG32Q==", futureInstant1.toInt(), 7,144, listOf("NI")),
                Exposure("W2zb3BeMWt6Xr2u0ABG32Q==", futureInstant2.toInt(), 4,144, listOf("NI"))
            )
        )
        keyUploader.acceptKeysFromFederatedServer(payload)

        assertThat(s3Storage.count, equalTo(0))
    }

    @Test
    fun rejectExpiredKeys() {
        val keyUploader = FederatedKeyUploader(s3Storage, bucketName, "federatedKeyPrefix", clock, validRegions)
        val pastInstant1 = LocalDateTime.ofInstant(clock.get().minus(20, ChronoUnit.DAYS), ZoneOffset.UTC).toEpochSecond(ZoneOffset.UTC) / 600L
        val pastInstant2 = LocalDateTime.ofInstant(clock.get().minus((24*14)+1, ChronoUnit.HOURS), ZoneOffset.UTC).toEpochSecond(ZoneOffset.UTC) / 600L
        val payload = DiagnosisKeysDownloadResponse(
            "batchTag",
            listOf(
                Exposure("W2zb3BeMWt6Xr2u0ABG32Q==", pastInstant1.toInt(), 7,144, listOf("NI")),
                Exposure("kzQt9Lf3xjtAlMtm7jkSqw==", pastInstant2.toInt(), 4,144, listOf("NI"))
            )
        )
        keyUploader.acceptKeysFromFederatedServer(payload)

        assertThat(s3Storage.count, equalTo(0))
    }

}

