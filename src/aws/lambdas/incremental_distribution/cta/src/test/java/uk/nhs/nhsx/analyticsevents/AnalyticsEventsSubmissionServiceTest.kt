package uk.nhs.nhsx.analyticsevents

import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose
import com.amazonaws.services.kinesisfirehose.model.PutRecordRequest
import com.amazonaws.services.kinesisfirehose.model.PutRecordResult
import com.amazonaws.services.kinesisfirehose.model.ServiceUnavailableException
import com.amazonaws.services.s3.model.AmazonS3Exception
import io.mockk.*
import org.http4k.asString
import org.junit.jupiter.api.Test
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.*
import uk.nhs.nhsx.core.ContentType
import uk.nhs.nhsx.core.Json
import uk.nhs.nhsx.core.aws.s3.*
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
    private val retrier = Retrier(2, 0)

    private fun createService(bucketName: BucketName, firehoseEnabled: Boolean = false, awsS3: AwsS3? = null) =
        AnalyticsEventsSubmissionService(
            awsS3 = awsS3 ?: fakeS3,
            objectKeyNameProvider = { ObjectKey.of("key") },
            bucketName = bucketName,
            events = events,
            firehoseClient = FirehoseClient(firehoseEnabled, "streamName", kinesisFirehose),
            retrier = retrier,
        )

    private fun payload(postalDistrict: String = "a1") = mapOf(
        "metadata" to mapOf(
            "operatingSystemVersion" to "1",
            "latestApplicationVersion" to "2",
            "deviceModel" to "phone",
            "postalDistrict" to postalDistrict,
        ), "events" to listOf(
            mapOf(
                "type" to "t", "version" to "v", "payload" to mapOf(
                    "riskScore" to 0.5,
                    "riskCalculationVersion" to 1,
                    "infectiousness" to "high",
                    "isConsideredRisky" to true,
                    "scanInstances" to listOf(
                        mapOf(
                            "secondsSinceLastScan" to 1, "minimumAttenuation" to 2, "typicalAttenuation" to 3
                        ), mapOf(
                            "secondsSinceLastScan" to 4, "minimumAttenuation" to 5, "typicalAttenuation" to 6
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
                        isA<Map<String, String>>().hasEntry("operatingSystemVersion", "1")
                            .hasEntry("latestApplicationVersion", "2").hasEntry("deviceModel", "phone")
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
                        isA<Map<String, String>>().hasEntry("operatingSystemVersion", "1")
                            .hasEntry("latestApplicationVersion", "2").hasEntry("deviceModel", "phone")
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
        expectThat(actualJsonObj).isEqualTo(
            mutableMapOf(
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
                        "secondsSinceLastScan" to 1, "minimumAttenuation" to 2, "typicalAttenuation" to 3
                    ), mapOf(
                        "secondsSinceLastScan" to 4, "minimumAttenuation" to 5, "typicalAttenuation" to 6
                    )
                ),
                "date" to "YYYY-MM-DDT00:00:00Z"
            )
        )
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
            val actualJsonObj =
                Json.readJsonOrThrow<MutableMap<String, Any>>(actualPutRecordRequest.record.data.asString())
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

    @Test
    fun `retry on temporary firehose failure`() {
        val captures = mutableListOf<PutRecordRequest>()
        val job = FlakyJob(error = ServiceUnavailableException("foo"), nErrors = 1) { mockk<PutRecordResult>() }
        every { kinesisFirehose.putRecord(capture(captures)) } answers { job.invoke() }

        val bucketName = BucketName.of("bucket")
        val service = createService(bucketName, firehoseEnabled = true)
        service.accept(payload())

        verify(exactly = 2) { kinesisFirehose.putRecord(any()) }

        expectThat(captures).hasSize(2)
        expectThat(captures.toSet()).describedAs("retried with different argument...").hasSize(1)
    }

    @Test
    fun `retry on temporary s3 failure`() {
        // we create a spy wrapper around the fakeS3 client so as to inject a level of flakiness to the upload call
        val captures = mutableListOf<List<Any?>>()
        val spyS3 = spyk(fakeS3)
        var args: List<Any?>? = null
        val flakyJob = FlakyJob(error = AmazonS3Exception("foo")) {
            val (locator, contentType, bytes, metaHeaders) = args!!
            fakeS3.upload(
                locator as Locator,
                contentType as ContentType,
                bytes as ByteArraySource,
                @Suppress("UNCHECKED_CAST") (metaHeaders as List<MetaHeader>),
            )
        }
        every { spyS3.upload(any(), any(), any(), any()) } answers { call ->
            if (args == null) {
                args = call.invocation.args
            }
            captures.add(call.invocation.args)
            flakyJob.invoke()
        }

        val bucketName = BucketName.of("bucket")
        val service = createService(bucketName, awsS3 = spyS3)
        service.accept(payload())

        verify(exactly = 2) { spyS3.upload(any(), any(), any(), any()) }

        expectThat(captures).hasSize(2)
        expectThat(captures.toSet()).describedAs("retried with different argument...").hasSize(1)
    }
}


class RetryTest {

    @Test
    fun `retry returns first success`() {
        val retrier = Retrier(times = 2, delayMillis = 0)
        val expected = "result"
        val job = FlakyJob(nErrors = 0) { expected }
        val actual = retrier.retry(job::invoke) { Retrier.Outcome.FAIL }
        expectThat(expected).isEqualTo(actual)
    }

    @Test
    fun `retry on failure`() {
        val retrier = Retrier(times = 2, delayMillis = 0)
        val expected = "result"
        val job = FlakyJob(nErrors = 1) { expected }
        val actual = retrier.retry(job::invoke) { Retrier.Outcome.RETRY }
        expectThat(expected).isEqualTo(actual)
    }

    @Test
    fun `retry throws other exceptions`() {
        val retrier = Retrier(times = 2, delayMillis = 0)
        val job = FlakyJob(nErrors = 3, error = ServiceUnavailableException("foo")) { "result" }
        expectCatching {
            retrier.retry(job::invoke) { ex ->
                if (ex is IllegalStateException) Retrier.Outcome.RETRY else Retrier.Outcome.FAIL
            }
        }.isFailure().isA<ServiceUnavailableException>()
        expectThat(job.called).describedAs("should stop immediately on different exception").isEqualTo(1)
    }

    @Test
    fun `retry throws broad exceptions`() {
        val retrier = Retrier(times = 2, delayMillis = 0)
        val job = FlakyJob(nErrors = 3, error = Exception("foo")) { "result" }
        expectCatching {
            retrier.retry(job::invoke) { ex ->
                if (ex is IllegalStateException) Retrier.Outcome.RETRY else Retrier.Outcome.FAIL
            }
        }.isFailure().isA<Exception>()
        expectThat(job.called).describedAs("should stop immediately on different exception").isEqualTo(1)
    }

    @Test
    fun `retry stops after n retries`() {
        val retrier = Retrier(times = 2, delayMillis = 0)
        val job = FlakyJob(nErrors = 3, error = ServiceUnavailableException("foo")) { "result" }
        expectCatching {
            retrier.retry(job::invoke) { ex ->
                if (ex is ServiceUnavailableException) Retrier.Outcome.RETRY else Retrier.Outcome.FAIL
            }
        }.isFailure().isA<ServiceUnavailableException>()
        expectThat(job.called).isEqualTo(2)

    }

    @Test
    fun `amazonClientHandler retries on known errors`() {
        val exceptionResults = listOf(
            ServiceUnavailableException("foo") to Retrier.Outcome.RETRY,
            AmazonS3Exception("foo") to Retrier.Outcome.RETRY,
            Exception() to Retrier.Outcome.FAIL,
            IllegalStateException() to Retrier.Outcome.FAIL,
        )

        for ((exception, result) in exceptionResults) {
            expectThat(Retrier().amazonClientHandler(exception)).describedAs("$exception should result in $result")
                .isEqualTo(result)
        }
    }
}

class FlakyJob<out T>(
    val error: Exception = Exception("error"),
    val nErrors: Int = 1,
    val block: () -> T,
) {
    var called: Int = 0
    operator fun invoke(): T {
        called += 1
        if (called > nErrors) {
            return block()
        } else {
            throw error
        }
    }
}
