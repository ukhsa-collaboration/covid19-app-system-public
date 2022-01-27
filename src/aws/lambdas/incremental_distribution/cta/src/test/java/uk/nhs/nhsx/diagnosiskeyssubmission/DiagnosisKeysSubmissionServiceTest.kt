@file:Suppress("TestFunctionName")

package uk.nhs.nhsx.diagnosiskeyssubmission

import com.amazonaws.services.dynamodbv2.document.Item
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import strikt.api.Assertion
import strikt.api.expect
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import uk.nhs.nhsx.core.aws.dynamodb.AwsDynamoClient
import uk.nhs.nhsx.core.aws.dynamodb.TableName
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.diagnosiskeyssubmission.model.ClientTemporaryExposureKey
import uk.nhs.nhsx.diagnosiskeyssubmission.model.ClientTemporaryExposureKeysPayload
import uk.nhs.nhsx.domain.TestKit
import uk.nhs.nhsx.testhelper.assertions.S3ObjectAssertions.asString
import uk.nhs.nhsx.testhelper.assertions.S3ObjectAssertions.content
import uk.nhs.nhsx.testhelper.data.TestData.STORED_KEYS_PAYLOAD_ONE_KEY
import uk.nhs.nhsx.testhelper.data.TestData.STORED_KEYS_PAYLOAD_SUBMISSION
import uk.nhs.nhsx.testhelper.data.TestData.STORED_KEYS_PAYLOAD_WITH_RISK_LEVEL
import uk.nhs.nhsx.testhelper.mocks.FakeS3
import uk.nhs.nhsx.testhelper.mocks.getBucket
import uk.nhs.nhsx.testhelper.mocks.getObject
import uk.nhs.nhsx.testhelper.mocks.isEmpty
import java.time.Duration
import java.time.Instant
import java.util.*

class DiagnosisKeysSubmissionServiceTest {

    private val clock = { Instant.ofEpochSecond((2667023 * 600).toLong()) } // 2020-09-15 23:50:00 UTC
    private val rollingStartNumberLastKey = 2666736 // 2020-09-14 00:00:00 UTC (last key in 14 day history)
    private val rollingStartNumberFirstKey = 2664864 // 2020-09-01 00:00:00 UTC (first key in 14 day history)

    private val events = RecordingEvents()
    private val tableName = TableName.of(UUID.randomUUID().toString())
    private val bucketName = BucketName.of(UUID.randomUUID().toString())
    private val awsS3 = FakeS3()
    private val awsDynamoClient = mockk<AwsDynamoClient>()
    private val objectKeyNameProvider = { ObjectKey.of("my-object-key") }

    private val service = DiagnosisKeysSubmissionService(
        awsS3 = awsS3,
        awsDynamoClient = awsDynamoClient,
        objectKeyNameProvider = objectKeyNameProvider,
        tableName = tableName,
        bucketName = bucketName,
        clock = clock,
        events = events
    )

    @Test
    fun `accepts temporary exposure keys no testkit defaults to lab result`() {
        awsDynamoClient.willDeleteAny().willReturnVirologyRecord()

        val payload = ClientTemporaryExposureKeysPayload(
            ClientTemporaryExposureKey(
                key = "W2zb3BeMWt6Xr2u0ABG32Q==",
                rollingStartNumber = rollingStartNumberLastKey,
                rollingPeriod = 144
            ),
            ClientTemporaryExposureKey(
                key = "kzQt9Lf3xjtAlMtm7jkSqw==",
                rollingStartNumber = rollingStartNumberFirstKey,
                rollingPeriod = 144
            )
        )

        service.acceptTemporaryExposureKeys(payload)

        expect {
            that(awsDynamoClient).hasBeenCalledWith(payload)
            that(awsS3)
                .getBucket(bucketName)
                .getObject("mobile/LAB_RESULT/my-object-key.json")
                .content
                .asString()
                .isEqualTo(STORED_KEYS_PAYLOAD_SUBMISSION)
        }
    }

    @ParameterizedTest
    @EnumSource(TestKit::class)
    fun `accepts temporary exposure keys uses stored testkit`(testKit: TestKit) {
        awsDynamoClient.willDeleteAny().willReturnVirologyRecord(testKit)

        val payload = ClientTemporaryExposureKeysPayload(
            ClientTemporaryExposureKey(
                key = "W2zb3BeMWt6Xr2u0ABG32Q==",
                rollingStartNumber = rollingStartNumberLastKey,
                rollingPeriod = 144
            ),
            ClientTemporaryExposureKey(
                key = "kzQt9Lf3xjtAlMtm7jkSqw==",
                rollingStartNumber = rollingStartNumberFirstKey,
                rollingPeriod = 144
            )
        )

        service.acceptTemporaryExposureKeys(payload)

        expect {
            that(awsDynamoClient).hasBeenCalledWith(payload)
            that(awsS3)
                .getBucket(bucketName)
                .getObject("mobile/${testKit}/my-object-key.json")
                .content
                .asString()
                .isEqualTo(STORED_KEYS_PAYLOAD_SUBMISSION)
        }
    }

    @Test
    fun `accepts temporary exposure keys with risk level`() {
        awsDynamoClient.willDeleteAny().willReturnVirologyRecord()

        val payload = ClientTemporaryExposureKeysPayload(
            ClientTemporaryExposureKey(
                key = "W2zb3BeMWt6Xr2u0ABG32Q==",
                rollingStartNumber = rollingStartNumberLastKey,
                rollingPeriod = 144
            ).apply { transmissionRiskLevel = 5 },
            ClientTemporaryExposureKey(
                key = "kzQt9Lf3xjtAlMtm7jkSqw==",
                rollingStartNumber = rollingStartNumberFirstKey,
                rollingPeriod = 144
            ).apply { transmissionRiskLevel = 4 }
        )

        service.acceptTemporaryExposureKeys(payload)

        expect {
            that(awsDynamoClient).hasBeenCalledWith(payload)
            that(awsS3)
                .getBucket(bucketName)
                .getObject("mobile/LAB_RESULT/my-object-key.json")
                .content
                .asString()
                .isEqualTo(STORED_KEYS_PAYLOAD_WITH_RISK_LEVEL)
        }
    }

    @Test
    fun `if more than fourteen exposure keys then reject`() {
        val payload = ClientTemporaryExposureKeysPayload(
            UUID.randomUUID(),
            (0..15).map {
                ClientTemporaryExposureKey(
                    key = "W2zb3BeMWt6Xr2u0ABG32Q==",
                    rollingStartNumber = 12345,
                    rollingPeriod = 144
                )
            }
        )

        service.acceptTemporaryExposureKeys(payload)

        expectThat(awsS3).isEmpty()
    }

    @Test
    fun `accept if at least one valid key`() {
        awsDynamoClient.willDeleteAny().willReturnVirologyRecord()

        val payload = ClientTemporaryExposureKeysPayload(
            ClientTemporaryExposureKey(
                key = "W2zb3BeMWt6Xr2u0ABG32Q==",
                rollingStartNumber = rollingStartNumberLastKey,
                rollingPeriod = 144
            ), ClientTemporaryExposureKey(
                key = null,
                rollingStartNumber = 12345,
                rollingPeriod = 148
            )
        )

        service.acceptTemporaryExposureKeys(payload)

        expect {
            that(awsDynamoClient).hasBeenCalledWith(payload)
            that(awsS3)
                .getBucket(bucketName)
                .getObject("mobile/LAB_RESULT/my-object-key.json")
                .content
                .asString()
                .isEqualTo(STORED_KEYS_PAYLOAD_ONE_KEY)
        }
    }

    @Test
    fun `reject if no valid keys`() {
        val payload = ClientTemporaryExposureKeysPayload(
            ClientTemporaryExposureKey(
                key = null,
                rollingStartNumber = 12345,
                rollingPeriod = 148
            )
        )

        service.acceptTemporaryExposureKeys(payload)

        expectThat(awsS3).isEmpty()
    }

    @Test
    fun `if token does not match then keys are not stored`() {
        val payload = ClientTemporaryExposureKeysPayload(
            ClientTemporaryExposureKey(
                key = "W2zb3BeMWt6Xr2u0ABG32Q==",
                rollingStartNumber = rollingStartNumberLastKey,
                rollingPeriod = 144
            ),
            ClientTemporaryExposureKey(
                key = "kzQt9Lf3xjtAlMtm7jkSqw==",
                rollingStartNumber = rollingStartNumberFirstKey,
                rollingPeriod = 144
            )
        )

        awsDynamoClient.willDeleteAny().willReturnNull(payload.diagnosisKeySubmissionToken)

        service.acceptTemporaryExposureKeys(payload)

        expectThat(awsS3).isEmpty()
    }

    @Test
    fun `key must be non null`() {
        val payload = ClientTemporaryExposureKeysPayload(
            ClientTemporaryExposureKey(
                key = null,
                rollingStartNumber = 12345,
                rollingPeriod = 144
            )
        )

        service.acceptTemporaryExposureKeys(payload)

        expectThat(awsS3).isEmpty()
    }

    @Test
    fun `key must be base 64 encoded`() {
        val payload = ClientTemporaryExposureKeysPayload(
            ClientTemporaryExposureKey(
                key = "some-key",
                rollingStartNumber = 12499,
                rollingPeriod = 144
            )
        )

        service.acceptTemporaryExposureKeys(payload)

        expectThat(awsS3).isEmpty()
    }

    @Test
    fun `key must be less than 32 bytes`() {
        val payload = ClientTemporaryExposureKeysPayload(
            ClientTemporaryExposureKey(
                key = "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXpBQkNERUZHCg==",
                rollingStartNumber = 12499,
                rollingPeriod = 144
            )
        )

        service.acceptTemporaryExposureKeys(payload)

        expectThat(awsS3).isEmpty()
    }

    @Test
    fun `rolling start number must be non negative`() {
        val payload = ClientTemporaryExposureKeysPayload(
            ClientTemporaryExposureKey(
                key = "kzQt9Lf3xjtAlMtm7jkSqw==",
                rollingStartNumber = -1,
                rollingPeriod = 144
            )
        )

        service.acceptTemporaryExposureKeys(payload)

        expectThat(awsS3).isEmpty()
    }

    @Test
    fun `rolling start number must not be in future`() {
        val futureRollingStartNumber = clock()
            .plus(Duration.ofDays(10))
            .epochSecond
            .div(Duration.ofMinutes(10).toSeconds())

        val payload = ClientTemporaryExposureKeysPayload(
            ClientTemporaryExposureKey(
                key = "kzQt9Lf3xjtAlMtm7jkSqw==",
                rollingStartNumber = futureRollingStartNumber.toInt(),
                rollingPeriod = 144
            )
        )

        service.acceptTemporaryExposureKeys(payload)

        expectThat(awsS3).isEmpty()
    }

    @Test
    fun `rolling period must be between zero to 144`() {
        val payload = ClientTemporaryExposureKeysPayload(
            ClientTemporaryExposureKey(
                key = "W2zb3BeMWt6Xr2u0ABG32Q==",
                rollingStartNumber = rollingStartNumberLastKey,
                rollingPeriod = 145
            )
        )

        service.acceptTemporaryExposureKeys(payload)

        expectThat(awsS3).isEmpty()
    }

    @Test
    fun `rolling period must be non negative`() {
        val payload = ClientTemporaryExposureKeysPayload(
            ClientTemporaryExposureKey(
                key = "W2zb3BeMWt6Xr2u0ABG32Q==",
                rollingStartNumber = rollingStartNumberLastKey,
                rollingPeriod = -2
            )
        )

        service.acceptTemporaryExposureKeys(payload)

        expectThat(awsS3).isEmpty()
    }

    @Test
    fun `transmission risk level must be between zero to7`() {
        val payload = ClientTemporaryExposureKeysPayload(
            ClientTemporaryExposureKey(
                key = "W2zb3BeMWt6Xr2u0ABG32Q==",
                rollingStartNumber = rollingStartNumberLastKey,
                rollingPeriod = 142
            ).apply {
                transmissionRiskLevel = 9
            }
        )

        service.acceptTemporaryExposureKeys(payload)

        expectThat(awsS3).isEmpty()
    }

    @Test
    fun `transmission risk level must be non negative`() {
        val payload = ClientTemporaryExposureKeysPayload(
            ClientTemporaryExposureKey(
                key = "W2zb3BeMWt6Xr2u0ABG32Q==",
                rollingStartNumber = rollingStartNumberLastKey,
                rollingPeriod = 142
            ).apply {
                transmissionRiskLevel = -2
            }
        )

        service.acceptTemporaryExposureKeys(payload)

        expectThat(awsS3).isEmpty()
    }

    private fun AwsDynamoClient.willReturnVirologyRecord(testKit: TestKit? = null) = apply {
        val hashKeyValue = slot<String>()

        every {
            getItem(tableName, "diagnosisKeySubmissionToken", capture(hashKeyValue))
        } answers {
            val id = UUID.fromString(hashKeyValue.captured)
            when (testKit) {
                null -> Item.fromJSON("""{"diagnosisKeySubmissionToken": "$id"}""")
                else -> Item.fromJSON("""{"diagnosisKeySubmissionToken": "$id", "testKit": "$testKit"}""")
            }
        }
    }

    private fun AwsDynamoClient.willReturnNull(id: UUID) = apply {
        every { getItem(tableName, "diagnosisKeySubmissionToken", id.toString()) } returns null
    }

    private fun AwsDynamoClient.willDeleteAny() = apply {
        every { deleteItem(any(), any(), any()) } just runs
    }

    private fun ClientTemporaryExposureKeysPayload(vararg keys: ClientTemporaryExposureKey) =
        ClientTemporaryExposureKeysPayload(UUID.randomUUID(), keys.toList())

    private fun Assertion.Builder<AwsDynamoClient>.hasBeenCalledWith(payload: ClientTemporaryExposureKeysPayload) =
        apply {
            verify {
                awsDynamoClient.getItem(
                    tableName,
                    "diagnosisKeySubmissionToken",
                    payload.diagnosisKeySubmissionToken.toString()
                )
                awsDynamoClient.deleteItem(
                    tableName,
                    "diagnosisKeySubmissionToken",
                    payload.diagnosisKeySubmissionToken.toString()
                )
            }
        }
}
