package uk.nhs.nhsx.analyticsevents

import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose
import com.amazonaws.services.kinesisfirehose.model.PutRecordRequest
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.http4k.asString
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsKeys
import strikt.assertions.hasEntry
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue
import strikt.assertions.withFirst
import strikt.assertions.withValue
import uk.nhs.nhsx.core.Json
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.testhelper.assertions.S3ObjectAssertions.content
import uk.nhs.nhsx.testhelper.assertions.S3ObjectAssertions.key
import uk.nhs.nhsx.testhelper.mocks.FakeS3
import uk.nhs.nhsx.testhelper.mocks.getBucket
import uk.nhs.nhsx.testhelper.mocks.withReadJsonOrThrows

class AnalyticsEventsSubmissionServiceTest {
    private val fakeS3 = FakeS3()
    private val events = RecordingEvents()
    private val kinesisFirehose = mockk<AmazonKinesisFirehose>()

    private fun createService(bucketName: BucketName, firehoseEnabled: Boolean = false) = AnalyticsEventsSubmissionService(
        awsS3 = fakeS3,
        objectKeyNameProvider = { ObjectKey.of("key") },
        bucketName = bucketName,
        events = events,
        firehoseClient = FirehoseClient(firehoseEnabled, "streamName", kinesisFirehose),
    )

    private fun payload(postalDistrict: String = "a1") = mapOf(
        "metadata" to mapOf(
            "operatingSystemVersion" to "1",
            "latestApplicationVersion" to "2",
            "deviceModel" to "phone",
            "postalDistrict" to postalDistrict,
        ),
        "events" to listOf(
            mapOf(
                "type" to "t",
                "version" to "v",
                "payload" to mapOf(
                    "riskScore" to 0.5,
                    "riskCalculationVersion" to 1,
                    "infectiousness" to "high",
                    "isConsideredRisky" to true,
                    "scanInstances" to listOf(
                        mapOf(
                            "secondsSinceLastScan" to 1,
                            "minimumAttenuation" to 2,
                            "typicalAttenuation" to 3
                        ),
                        mapOf(
                            "secondsSinceLastScan" to 4,
                            "minimumAttenuation" to 5,
                            "typicalAttenuation" to 6
                        )
                    ),
                    "date" to "YYYY-MM-DDT00:00:00Z"
                )
            )
        )
    )

    @Test
    fun `accept payload with no mapped postal district is stored in s3`() {
        val bucketName = BucketName.of("bucket")
        val service = createService(bucketName)
        service.accept(payload())

        expectThat(fakeS3) {
            getBucket(bucketName).hasSize(1).withFirst {
                key.isEqualTo("key.json")
                content.withReadJsonOrThrows<Map<String, Any>> {
                    containsKeys("uuid", "metadata", "events")
                    withValue("metadata") {
                        isA<Map<String, String>>()
                            .hasEntry("operatingSystemVersion", "1")
                            .hasEntry("latestApplicationVersion", "2")
                            .hasEntry("deviceModel", "phone")
                            .hasEntry("postalDistrict", "UNKNOWN")
                    }
                }
            }
        }
    }

    @Test
    fun `accept payload with merged postal district is stored in s3`() {
        val bucketName = BucketName.of("bucket")
        val service = createService(bucketName)
        service.accept(payload(postalDistrict = "AB13"))

        expectThat(fakeS3) {
            getBucket(bucketName).hasSize(1).withFirst {
                key.isEqualTo("key.json")
                content.withReadJsonOrThrows<Map<String, Any>> {
                    containsKeys("uuid", "metadata", "events")
                    withValue("metadata") {
                        isA<Map<String, String>>()
                            .hasEntry("operatingSystemVersion", "1")
                            .hasEntry("latestApplicationVersion", "2")
                            .hasEntry("deviceModel", "phone")
                            .hasEntry("postalDistrict", "AB13_AB14")
                    }
                }
            }
        }
    }

    @Test
    fun `uploads to firehose when feature is enabled`() {
        val slot = slot<PutRecordRequest>()
        every { kinesisFirehose.putRecord(capture(slot)) } returns mockk()

        val bucketName = BucketName.of("bucket")
        val service = createService(bucketName, firehoseEnabled = true)
        service.accept(payload())

        verify(exactly = 1) { kinesisFirehose.putRecord(any()) }

        expectThat(slot.isCaptured).isTrue()
        val actualJsonObj = Json.readJsonOrThrow<MutableMap<String, Any>>(slot.captured.record.data.asString())
        actualJsonObj.remove("uuid")  // uuid is randomly generated by service, so do not want to test it
        expectThat(actualJsonObj).isEqualTo(mutableMapOf(
            "operatingSystemVersion" to "1",
            "latestApplicationVersion" to "2",
            "deviceModel" to "phone",
            "postalDistrict" to "UNKNOWN",
            "localAuthority" to "UNKNOWN",
            "type" to "t",
            "version" to "v",
            "riskScore" to 0.5,
            "riskCalculationVersion" to 1,
            "infectiousness" to "high",
            "isConsideredRisky" to true,
            "scanInstances" to listOf(
                mapOf(
                    "secondsSinceLastScan" to 1,
                    "minimumAttenuation" to 2,
                    "typicalAttenuation" to 3
                ),
                mapOf(
                    "secondsSinceLastScan" to 4,
                    "minimumAttenuation" to 5,
                    "typicalAttenuation" to 6
                )
            ),
            "date" to "YYYY-MM-DDT00:00:00Z"
        ))
    }

    @Test
    fun `firehose can handle multiple events in one payload`() {
        val captures = mutableListOf<PutRecordRequest>()
        every { kinesisFirehose.putRecord(capture(captures)) } returns mockk()

        val bucketName = BucketName.of("bucket")
        val service = createService(bucketName, firehoseEnabled = true)
        val payload = mapOf(
            "metadata" to mapOf(
                "operatingSystemVersion" to "1",
                "latestApplicationVersion" to "2",
                "deviceModel" to "phone",
                "postalDistrict" to "a1",
            ),
            "events" to listOf(
                mapOf(
                    "type" to "first",
                    "version" to "v",
                    "payload" to emptyMap<String, Any>(),
                ),
                mapOf(
                    "type" to "second",
                    "version" to "v",
                    "payload" to emptyMap<String, Any>(),
                ),
            ),
        )
        service.accept(payload)

        verify(exactly = 2) { kinesisFirehose.putRecord(any()) }
        expectThat(captures).hasSize(2)

        for ((actualPutRecordRequest, expectedType) in captures.zip(listOf("first", "second"))) {
            val actualJsonObj = Json.readJsonOrThrow<MutableMap<String, Any>>(actualPutRecordRequest.record.data.asString())
            // uuid is randomly generated by service, so do not want to test it
            actualJsonObj.remove("uuid")
            expectThat(actualJsonObj).isEqualTo(
                mutableMapOf(
                    "operatingSystemVersion" to "1",
                    "latestApplicationVersion" to "2",
                    "deviceModel" to "phone",
                    "postalDistrict" to "UNKNOWN",
                    "localAuthority" to "UNKNOWN",
                    "type" to expectedType,
                    "version" to "v",
                )
            )
        }
    }
}
