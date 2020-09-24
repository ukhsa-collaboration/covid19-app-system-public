package uk.nhs.nhsx.keyfederation

import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Test
import org.mockito.Mockito
import uk.nhs.nhsx.TestData
import uk.nhs.nhsx.analyticssubmission.FakeS3Storage
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.core.aws.s3.ObjectKeyNameProvider
import uk.nhs.nhsx.diagnosiskeyssubmission.model.ClientTemporaryExposureKey
import uk.nhs.nhsx.diagnosiskeyssubmission.model.ClientTemporaryExposureKeysPayload
import java.nio.charset.StandardCharsets
import java.util.*


class KeyUploaderTest {

    private val bucketName = BucketName.of("some-bucket-name")
    private val s3Storage = FakeS3Storage()
    private val objectKeyNameProvider = Mockito.mock(ObjectKeyNameProvider::class.java)
    private val objectKey = ObjectKey.of("federatedKeyPrefix_batchTag")
    private val uuid = "dd3aa1bf-4c91-43bb-afb6-12d0b5dcad43"
    @Test
    fun acceptTemporaryExposureKeysFromFederatedServer() {
        val keyUploader = KeyUploader(s3Storage, bucketName, "federatedKeyPrefix")
        val payload = ClientTemporaryExposureKeysPayload(
            UUID.fromString(uuid),
            listOf(
                ClientTemporaryExposureKey("W2zb3BeMWt6Xr2u0ABG32Q==", 12345, 144),
                ClientTemporaryExposureKey("kzQt9Lf3xjtAlMtm7jkSqw==", 12499, 144)
            )
        )
        keyUploader.acceptKeysFromFederatedServer(payload, "batchTag")


        MatcherAssert.assertThat(s3Storage.count, CoreMatchers.equalTo(1))
        MatcherAssert.assertThat(s3Storage.name, CoreMatchers.equalTo<ObjectKey>(objectKey.append(".json")))
        MatcherAssert.assertThat(s3Storage.bucket, CoreMatchers.equalTo<BucketName>(bucketName))
        MatcherAssert.assertThat(String(s3Storage.bytes.read(), StandardCharsets.UTF_8), CoreMatchers.equalTo(TestData.STORED_KEYS_PAYLOAD))
        Mockito.verifyNoMoreInteractions(objectKeyNameProvider)
    }

    @Test
    fun rejectKeyLongerThan32Bytes(){
        val keyUploader = KeyUploader(s3Storage, bucketName, "federatedKeyPrefix")
        val payload = ClientTemporaryExposureKeysPayload(
            UUID.fromString(uuid),
            listOf(
                ClientTemporaryExposureKey("W2zb3BeMWt6Xr2u0ABG32Q==", 12345, 144),
                ClientTemporaryExposureKey("YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXpBQkNERUZHCg==", 12499, 144)
            )
        )
        keyUploader.acceptKeysFromFederatedServer(payload, "batchTag")

        MatcherAssert.assertThat(s3Storage.count, CoreMatchers.equalTo(0))
    }

}