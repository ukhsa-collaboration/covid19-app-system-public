package uk.nhs.nhsx.analyticsevents

import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose
import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehoseClientBuilder
import com.amazonaws.services.kinesisfirehose.model.PutRecordRequest
import com.amazonaws.services.kinesisfirehose.model.Record
import com.amazonaws.services.kinesisfirehose.model.ServiceUnavailableException
import uk.nhs.nhsx.analyticssubmission.AnalyticsMapFlattener
import uk.nhs.nhsx.analyticssubmission.PostDistrictLaReplacer
import uk.nhs.nhsx.core.ContentType.Companion.APPLICATION_JSON
import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.Json.toJson
import uk.nhs.nhsx.core.aws.s3.AwsS3
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.ByteArraySource.Companion.fromUtf8String
import uk.nhs.nhsx.core.aws.s3.Locator
import uk.nhs.nhsx.core.aws.s3.ObjectKeyNameProvider
import uk.nhs.nhsx.core.events.Events
import java.nio.ByteBuffer
import java.util.*
import kotlin.reflect.KClass

class AnalyticsEventsSubmissionService(
    private val awsS3: AwsS3,
    private val objectKeyNameProvider: ObjectKeyNameProvider,
    private val bucketName: BucketName,
    private val events: Events,
    private val firehoseClient: FirehoseClient,
) {
    fun accept(payload: Map<String, Any>) {
        val transformedPayload = transformPayload(payload)
        uploadToS3(toJson(transformedPayload))

        if (firehoseClient.enabled) {
            val flattenedPayloads = flattenPayload(transformedPayload)
            flattenedPayloads.forEach { firehoseClient.uploadWithRetry(toJson(it)) }
        }
    }

    private fun transformPayload(payload: Map<String, Any>): Map<String, Any> {
        val metadata = payload["metadata"] as? Map<*, *> ?: error("metadata must be a map")

        val currentPostalDistrict = metadata["postalDistrict"] as String
        val currentLocalAuthority = metadata["localAuthority"] as String?
        val mappedPostDistrict = PostDistrictLaReplacer(
            currentPostalDistrict,
            currentLocalAuthority,
            events
        )

        return payload.toMutableMap().apply {
            this["uuid"] = UUID.randomUUID()
            this["metadata"] = metadata.toMutableMap().apply {
                this["postalDistrict"] = mappedPostDistrict.postDistrict
                this["localAuthority"] = mappedPostDistrict.localAuthorityId
            }

        }
    }

    private fun uploadToS3(json: String) {
        awsS3.upload(
            Locator.of(bucketName, objectKeyNameProvider.generateObjectKeyName().append(".json")),
            APPLICATION_JSON,
            fromUtf8String(json)
        )
    }

    private fun flattenPayload(payload: Map<String, Any?>): List<Map<String, Any?>> {
        // events should only ever be a list with 1 element. This is a policy requirement (for privacy). However, we do
        // not enforce this, thus when we flatten the data structure we should be prepared to handle cases where events
        // contains multiple requirements
        val events = payload["events"]
        val payloadCopy =
            AnalyticsMapFlattener.flattenRecursively(
                payload.filter { (key, _value) -> key != "events" }
            )
        if (events !is List<*>) {
            throw IllegalArgumentException("events is not a list")
        }
        val flattened = events.map @Suppress("UNCHECKED_CAST") {
            AnalyticsMapFlattener.flattenRecursively(it as Map<String, Any?>) + payloadCopy
        }
        return flattened
    }
}


class FirehoseClient(
    val enabled: Boolean,
    private val streamName: String,
    private val client: AmazonKinesisFirehose,
    private val retrier: Retrier = Retrier(1, 0)
) {
    fun upload(json: String) {
        if (!enabled) {
            throw IllegalStateException("upload is disabled")
        }
        val record = Record().withData(ByteBuffer.wrap(json.toByteArray()))
        val putRecordRequest = PutRecordRequest()
            .withRecord(record)
            .withDeliveryStreamName(streamName)
        client.putRecord(putRecordRequest)
    }

    fun uploadWithRetry(json: String) = retrier.retry(cause = ServiceUnavailableException::class) { upload(json) }

    companion object {
        private val ENABLED = Environment.EnvironmentKey.bool("firehose_ingest_enabled")
        private val STREAM_NAME = Environment.EnvironmentKey.string("firehose_stream_name")
        private val RETRY_TIMES = Environment.EnvironmentKey.integer("firehose_retry_times")
        private val RETRY_DELAY = Environment.EnvironmentKey.long("firehose_retry_delay")

        fun from(env: Environment): FirehoseClient {
            val enabled = env.access.optional(ENABLED).orElse(false)
            val streamName = env.access.optional(STREAM_NAME).orElse("")
            val retryTimes = env.access.optional(RETRY_TIMES).orElse(1)
            val retryDelay = env.access.optional(RETRY_DELAY).orElse(0)
            if (enabled && streamName == "") {
                throw IllegalStateException("firehose ingestion is enabled, but no stream name was provided")
            }

            return FirehoseClient(
                enabled,
                streamName,
                AmazonKinesisFirehoseClientBuilder.defaultClient(),
                Retrier(retryTimes, retryDelay),
            )
        }
    }
}

class Retrier(
    private val times: Int = 1, // total number of times to try something -- 1 means no retrying
    private val delayMillis: Long = 0,
) {
    init {
        assert(times >= 1)
        assert(delayMillis >= 0)
    }

    fun <T> retry(cause: KClass<out Exception>, block: () -> T) = retry(times, delayMillis, cause, block)

    fun <T> retry(times: Int, delayMillis: Long, cause: KClass<out Exception>, block: () -> T): T {
        for (i in 1 until times) {
            try {
                return block()
            } catch (ex: Exception) {
                if (!cause.isInstance(ex)) {
                    // unknown exception: rethrow
                    throw ex
                }
                if (delayMillis > 0) {
                    Thread.sleep(delayMillis)
                }
            }
        }
        // final attempt: do not catch any errors
        return block()
    }
}
