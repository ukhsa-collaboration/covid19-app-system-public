package uk.nhs.nhsx.analyticsevents

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsKeys
import strikt.assertions.hasEntry
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.withFirst
import strikt.assertions.withValue
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

    @Test
    fun `accept payload with no mapped postal district is stored in s3`() {
        val bucketName = BucketName.of("bucket")

        AnalyticsEventsSubmissionService(
            s3Storage = fakeS3,
            objectKeyNameProvider = { ObjectKey.of("key") },
            bucketName = bucketName,
            events = events
        ).accept(
            mapOf(
                "metadata" to mapOf(
                    "operatingSystemVersion" to "1",
                    "latestApplicationVersion" to "2",
                    "deviceModel" to "phone",
                    "postalDistrict" to "a1"
                ),
                "events" to listOf(
                    mapOf(
                        "type" to "t",
                        "version" to "v",
                        "payload" to emptyMap<String, Any>()
                    )
                )
            )
        )

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

        AnalyticsEventsSubmissionService(
            s3Storage = fakeS3,
            objectKeyNameProvider = { ObjectKey.of("key") },
            bucketName = bucketName,
            events = events
        ).accept(
            mapOf(
                "metadata" to mapOf(
                    "operatingSystemVersion" to "1",
                    "latestApplicationVersion" to "2",
                    "deviceModel" to "phone",
                    "postalDistrict" to "AB13"
                ),
                "events" to listOf(
                    mapOf(
                        "type" to "t",
                        "version" to "v",
                        "payload" to emptyMap<String, Any>()
                    )
                )
            )
        )

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
}
