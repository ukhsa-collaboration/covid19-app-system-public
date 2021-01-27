package uk.nhs.nhsx.diagnosiskeyssubmission

import com.amazonaws.services.dynamodbv2.document.Item
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.aws.dynamodb.AwsDynamoClient
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.diagnosiskeyssubmission.model.ClientTemporaryExposureKey
import uk.nhs.nhsx.diagnosiskeyssubmission.model.ClientTemporaryExposureKeysPayload
import uk.nhs.nhsx.testhelper.data.TestData
import uk.nhs.nhsx.testhelper.data.TestData.STORED_KEYS_PAYLOAD_SUBMISSION
import uk.nhs.nhsx.testhelper.mocks.FakeS3Storage
import uk.nhs.nhsx.virology.TestKit
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.function.Supplier
import java.util.stream.Collectors
import java.util.stream.IntStream

class DiagnosisKeysSubmissionServiceTest {
    private val bucketName = BucketName.of("some-bucket-name")
    private val uuid = "dd3aa1bf-4c91-43bb-afb6-12d0b5dcad43"
    private val s3Storage = FakeS3Storage()
    private val awsDynamoClient = mockk<AwsDynamoClient>()
    private val objectKey = ObjectKey.of("some-object-key")
    private val objectKeyNameProvider = { objectKey }
    private val tableName = "some-table-name"
    private val hashKey = "diagnosisKeySubmissionToken"
    var clock = Supplier { Instant.ofEpochSecond((2667023 * 600).toLong()) } // 2020-09-15 23:50:00 UTC
    private var rollingStartNumberLastKey: Long = 2666736 // 2020-09-14 00:00:00 UTC (last key in 14 day history)
    private var rollingStartNumberFirstKey: Long = 2664864 // 2020-09-01 00:00:00 UTC (first key in 14 day history)
    private val service = DiagnosisKeysSubmissionService(
        s3Storage, awsDynamoClient, objectKeyNameProvider, tableName, bucketName, clock
    )
    private val dynamoItem = Item.fromJSON("{\"$hashKey\": \"$uuid\"}")

    @Test
    fun `accepts temporary exposure keys no testkit defaults to pcr`() {
        every { awsDynamoClient.getItem(tableName, hashKey, uuid) } returns dynamoItem
        every { awsDynamoClient.deleteItem(any(), any(), any()) } returns mockk()

        val payload = ClientTemporaryExposureKeysPayload(
            UUID.fromString(uuid),
            listOf(
                ClientTemporaryExposureKey("W2zb3BeMWt6Xr2u0ABG32Q==", rollingStartNumberLastKey.toInt(), 144),
                ClientTemporaryExposureKey("kzQt9Lf3xjtAlMtm7jkSqw==", rollingStartNumberFirstKey.toInt(), 144)
            )
        )
        service.acceptTemporaryExposureKeys(payload)
        verifyHappyPath(
            STORED_KEYS_PAYLOAD_SUBMISSION,
            ObjectKey.of("mobile/LAB_RESULT/${objectKey.value}.json"),
            uuid
        )
    }

    @Test
    fun `accepts temporary exposure keys uses stored testkit`() {
        TestKit.values().forEach {
            val uuid = UUID.randomUUID().toString()
            val dynamoItem = Item.fromJSON("""{"$hashKey": "$uuid", "testKit": "${it.name}"}""")
            every { awsDynamoClient.getItem(tableName, hashKey, uuid) } returns dynamoItem
            every { awsDynamoClient.deleteItem(any(), any(), any()) } returns mockk()

            val payload = ClientTemporaryExposureKeysPayload(
                UUID.fromString(uuid),
                listOf(
                    ClientTemporaryExposureKey("W2zb3BeMWt6Xr2u0ABG32Q==", rollingStartNumberLastKey.toInt(), 144),
                    ClientTemporaryExposureKey("kzQt9Lf3xjtAlMtm7jkSqw==", rollingStartNumberFirstKey.toInt(), 144)
                )
            )
            service.acceptTemporaryExposureKeys(payload)
            verifyHappyPath(
                STORED_KEYS_PAYLOAD_SUBMISSION,
                ObjectKey.of("mobile/${it.name}/${objectKey.value}.json"),
                uuid
            )
        }
    }

    @Test
    fun acceptsTemporaryExposureKeysWithRiskLevel() {
        val key1 = ClientTemporaryExposureKey("W2zb3BeMWt6Xr2u0ABG32Q==", rollingStartNumberLastKey.toInt(), 144)
        val key2 = ClientTemporaryExposureKey("kzQt9Lf3xjtAlMtm7jkSqw==", rollingStartNumberFirstKey.toInt(), 144)
        key1.setTransmissionRiskLevel(5)
        key2.setTransmissionRiskLevel(4)

        every { awsDynamoClient.getItem(tableName, hashKey, uuid) } returns dynamoItem
        every { awsDynamoClient.deleteItem(any(), any(), any()) } returns mockk()

        val payload = ClientTemporaryExposureKeysPayload(
            UUID.fromString(uuid),
            listOf(key1, key2)
        )
        service.acceptTemporaryExposureKeys(payload)
        verifyHappyPath(
            TestData.STORED_KEYS_PAYLOAD_WITH_RISK_LEVEL,
            ObjectKey.of("mobile/LAB_RESULT/${objectKey.value}.json"),
            uuid
        )
    }

    @Test
    fun ifMoreThanFourteenExposureKeysThenReject() {
        val key1 = ClientTemporaryExposureKey("W2zb3BeMWt6Xr2u0ABG32Q==", 12345, 144)
        val listOfKeys = IntStream.range(0, 15).mapToObj { key1 }.collect(Collectors.toList())
        val payload = ClientTemporaryExposureKeysPayload(
            UUID.fromString(uuid),
            listOfKeys
        )
        service.acceptTemporaryExposureKeys(payload)
        verifyNoInteractions()
    }

    @Test
    fun acceptIfAtleastOneValidKey() {
        every { awsDynamoClient.getItem(tableName, hashKey, uuid) } returns dynamoItem
        every { awsDynamoClient.deleteItem(any(), any(), any()) } returns mockk()

        val key1 = ClientTemporaryExposureKey("W2zb3BeMWt6Xr2u0ABG32Q==", rollingStartNumberLastKey.toInt(), 144)
        val key2 = ClientTemporaryExposureKey(null, 12345, 148)
        val payload = ClientTemporaryExposureKeysPayload(
            UUID.fromString(uuid),
            listOf(key1, key2)
        )

        service.acceptTemporaryExposureKeys(payload)
        verifyHappyPath(
            TestData.STORED_KEYS_PAYLOAD_ONE_KEY,
            ObjectKey.of("mobile/LAB_RESULT/${objectKey.value}.json"),
            uuid
        )
    }

    @Test
    fun rejectIfNoValidKeys() {
        val key2 = ClientTemporaryExposureKey(null, 12345, 148)
        val payload = ClientTemporaryExposureKeysPayload(
            UUID.fromString(uuid),
            listOf(key2)
        )
        service.acceptTemporaryExposureKeys(payload)
        verifyNoInteractions()
    }

    @Test
    fun ifTokenDoesNotMatchThenKeysAreNotStored() {
        every { awsDynamoClient.getItem(tableName, hashKey, uuid) } returns null
        every { awsDynamoClient.deleteItem(any(), any(), any()) } returns mockk()

        val payload = ClientTemporaryExposureKeysPayload(
            UUID.fromString(uuid),
            listOf(
                ClientTemporaryExposureKey("W2zb3BeMWt6Xr2u0ABG32Q==", rollingStartNumberLastKey.toInt(), 144),
                ClientTemporaryExposureKey("kzQt9Lf3xjtAlMtm7jkSqw==", rollingStartNumberFirstKey.toInt(), 144)
            )
        )
        service.acceptTemporaryExposureKeys(payload)
        assertThat(s3Storage.count, equalTo(0))
    }

    @Test
    fun keyMustBeNonNull() {
        val payload = ClientTemporaryExposureKeysPayload(
            UUID.fromString(uuid),
            listOf(
                ClientTemporaryExposureKey(null, 12345, 144)
            )
        )
        service.acceptTemporaryExposureKeys(payload)
        verifyNoInteractions()
    }

    @Test
    fun keyMustBeBase64Encoded() {
        val payload = ClientTemporaryExposureKeysPayload(
            UUID.fromString(uuid),
            listOf(
                ClientTemporaryExposureKey("some-key", 12499, 144)
            )
        )
        service.acceptTemporaryExposureKeys(payload)
        verifyNoInteractions()
    }

    @Test
    fun keyMustBeLessThan32Bytes() {
        val payload = ClientTemporaryExposureKeysPayload(
            UUID.fromString(uuid),
            listOf(
                ClientTemporaryExposureKey("YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXpBQkNERUZHCg==", 12499, 144)
            )
        )
        service.acceptTemporaryExposureKeys(payload)
        verifyNoInteractions()
    }

    @Test
    fun rollingStartNumberMustBeNonNegative() {
        val payload = ClientTemporaryExposureKeysPayload(
            UUID.fromString(uuid),
            listOf(
                ClientTemporaryExposureKey("kzQt9Lf3xjtAlMtm7jkSqw==", -1, 144)
            )
        )
        service.acceptTemporaryExposureKeys(payload)
        verifyNoInteractions()
    }

    @Test
    fun rollingStartNumberMustNotBeInFuture() {
        val tenMinutesIntervalSeconds = 600L
        val futureRollingStartNumber = clock.get().plus(10, ChronoUnit.DAYS).epochSecond / tenMinutesIntervalSeconds
        val payload = ClientTemporaryExposureKeysPayload(
            UUID.fromString(uuid),
            listOf(
                ClientTemporaryExposureKey("kzQt9Lf3xjtAlMtm7jkSqw==", futureRollingStartNumber.toInt(), 144)
            )
        )
        service.acceptTemporaryExposureKeys(payload)
        verifyNoInteractions()
    }

    @Test
    fun rollingPeriodMustBeBetweenZeroTo144() {
        val payload = ClientTemporaryExposureKeysPayload(
            UUID.fromString(uuid),
            listOf(
                ClientTemporaryExposureKey("W2zb3BeMWt6Xr2u0ABG32Q==", rollingStartNumberLastKey.toInt(), 145)
            )
        )
        service.acceptTemporaryExposureKeys(payload)
        verifyNoInteractions()
    }

    @Test
    fun rollingPeriodMustBeNonNegative() {
        val payload = ClientTemporaryExposureKeysPayload(
            UUID.fromString(uuid),
            listOf(
                ClientTemporaryExposureKey("W2zb3BeMWt6Xr2u0ABG32Q==", rollingStartNumberLastKey.toInt(), -2)
            )
        )
        service.acceptTemporaryExposureKeys(payload)
        verifyNoInteractions()
    }

    @Test
    fun transmissionRiskLevelMustBeBetweenZeroTo7() {
        val key = ClientTemporaryExposureKey("W2zb3BeMWt6Xr2u0ABG32Q==", rollingStartNumberLastKey.toInt(), 142)
        key.setTransmissionRiskLevel(9)
        val payload = ClientTemporaryExposureKeysPayload(
            UUID.fromString(uuid),
            listOf(key)
        )
        service.acceptTemporaryExposureKeys(payload)
        verifyNoInteractions()
    }

    @Test
    fun transmissionRiskLevelMustBeNonNegative() {
        val key = ClientTemporaryExposureKey("W2zb3BeMWt6Xr2u0ABG32Q==", rollingStartNumberLastKey.toInt(), 142)
        key.setTransmissionRiskLevel(-2)
        val payload = ClientTemporaryExposureKeysPayload(
            UUID.fromString(uuid),
            listOf(key)
        )
        service.acceptTemporaryExposureKeys(payload)
        verifyNoInteractions()
    }

    private fun verifyNoInteractions() {
        assertThat(s3Storage.count, equalTo(0))
    }

    private fun verifyHappyPath(expectedStoredPayload: String, expectedObjectKey: ObjectKey, uuid: String) {
        verify {
            awsDynamoClient.getItem(tableName, hashKey, uuid)
            awsDynamoClient.deleteItem(tableName, hashKey, uuid)
        }
        verifyObjectStored(expectedObjectKey, expectedStoredPayload)
    }

    private fun verifyObjectStored(expectedObjectKey: ObjectKey, expectedStoredPayload: String) {
        assertThat(s3Storage.name, equalTo(expectedObjectKey))
        assertThat(s3Storage.bucket, equalTo(bucketName))
        assertThat(String(s3Storage.bytes.read(), StandardCharsets.UTF_8), equalTo(expectedStoredPayload))
    }
}
