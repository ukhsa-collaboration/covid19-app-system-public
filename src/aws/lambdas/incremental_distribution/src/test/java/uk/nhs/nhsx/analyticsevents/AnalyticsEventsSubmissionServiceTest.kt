package uk.nhs.nhsx.analyticsevents

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.Jackson
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.testhelper.mocks.FakeS3

@Suppress("UNCHECKED_CAST")
class AnalyticsEventsSubmissionServiceTest {
    private val fakeS3 = FakeS3()
    private val events = RecordingEvents()

    @Test
    fun `accept payload with no mapped postal district is stored in s3`() {
        val service = AnalyticsEventsSubmissionService(
            fakeS3, { ObjectKey.of("key") }, BucketName.of("bucket"),
            events
        )
        service.accept(
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

        assertThat(fakeS3.count).isEqualTo(1)
        assertThat(fakeS3.name.value).isEqualTo("key.json")

        val storedObject: Map<String, Any> =
            Jackson.readJson(fakeS3.bytes.openStream(), Map::class.java) as Map<String, Any>
        assertThat(storedObject).containsKey("uuid")
        assertThat(storedObject).containsKey("metadata")
        assertThat(storedObject).containsKey("events")

        val storedMetadata = storedObject["metadata"] as Map<String, String>
        assertThat(storedMetadata["operatingSystemVersion"]).isEqualTo("1")
        assertThat(storedMetadata["latestApplicationVersion"]).isEqualTo("2")
        assertThat(storedMetadata["deviceModel"]).isEqualTo("phone")
        assertThat(storedMetadata["postalDistrict"]).isEqualTo("UNKNOWN")
    }

    @Test
    fun `accept payload with merged postal district is stored in s3`() {
        val fakeS3 = FakeS3()
        val service = AnalyticsEventsSubmissionService(fakeS3, { ObjectKey.of("key") }, BucketName.of("bucket"), events)
        service.accept(
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

        assertThat(fakeS3.count).isEqualTo(1)
        assertThat(fakeS3.name.value).isEqualTo("key.json")

        val storedObject: Map<String, Any> =
            Jackson.readJson(fakeS3.bytes.openStream(), Map::class.java) as Map<String, Any>
        assertThat(storedObject).containsKey("uuid")
        assertThat(storedObject).containsKey("metadata")
        assertThat(storedObject).containsKey("events")

        val storedMetadata = storedObject["metadata"] as Map<String, String>
        assertThat(storedMetadata["operatingSystemVersion"]).isEqualTo("1")
        assertThat(storedMetadata["latestApplicationVersion"]).isEqualTo("2")
        assertThat(storedMetadata["deviceModel"]).isEqualTo("phone")
        assertThat(storedMetadata["postalDistrict"]).isEqualTo("AB13_AB14")
    }

}
