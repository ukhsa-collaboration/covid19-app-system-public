package uk.nhs.nhsx.testhelper.mocks

import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.S3Object
import com.amazonaws.services.s3.model.S3ObjectInputStream
import com.amazonaws.services.s3.model.S3ObjectSummary
import strikt.api.Assertion
import strikt.assertions.first
import strikt.assertions.getValue
import uk.nhs.nhsx.core.Clock
import uk.nhs.nhsx.core.ContentType
import uk.nhs.nhsx.core.Json
import uk.nhs.nhsx.core.aws.s3.AwsS3
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.ByteArraySource
import uk.nhs.nhsx.core.aws.s3.Locator
import uk.nhs.nhsx.core.aws.s3.MetaHeader
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.testhelper.s3.S3ObjectSummary
import java.io.ByteArrayInputStream
import java.net.URL
import java.time.Instant
import java.util.*

open class FakeS3(val clock: Clock = { java.time.Clock.systemUTC().instant() }) : AwsS3 {
    override val enabled: Boolean = true

    private val summaries: MutableMap<BucketName, MutableList<S3ObjectSummary>> = mutableMapOf()
    val objects: MutableMap<BucketName, MutableList<S3Object>> = mutableMapOf()

    override fun getObjectSummaries(bucketName: BucketName) =
        summaries.getOrDefault(bucketName, mutableListOf())

    override fun getObject(locator: Locator) =
        objects.getOrDefault(locator.bucket, mutableListOf())
            .firstOrNull { it.key == locator.key.value }

    override fun deleteObject(locator: Locator) {
        summaries[locator.bucket] = summaries.getOrDefault(locator.bucket, mutableListOf())
            .apply { removeIf { it.key == locator.key.value } }
    }

    override fun copyObject(from: Locator, to: Locator) {
        getObject(from)?.also { addS3Object(to.bucket, it) }
        getObjectSummaries(from.bucket).forEach { addS3ObjectSummary(to.bucket, it) }
    }

    override fun upload(
        locator: Locator,
        contentType: ContentType,
        bytes: ByteArraySource,
        metaHeaders: List<MetaHeader>
    ) {
        val now = Date.from(clock())

        val s3Object = S3Object().apply {
            key = locator.key.value
            bucketName = locator.bucket.value
            setObjectContent(ByteArrayInputStream(bytes.bytes))
            objectMetadata = ObjectMetadata().apply {
                lastModified = now
                this.contentType = contentType.value
                metaHeaders.forEach { (k, v) -> this.addUserMetadata(k, v) }
            }
        }

        val summary = S3ObjectSummary().apply {
            key = locator.key.value
            bucketName = locator.bucket.value
            lastModified = now
        }

        addS3Object(locator.bucket, s3Object)
        addS3ObjectSummary(locator.bucket, summary)
    }

    override fun getSignedURL(locator: Locator, expiration: Date) = URL("https://example.com")

    fun add(s3Object: S3Object, lastModifiedDate: Instant? = null) {
        val bucketName = BucketName.of(s3Object.bucketName)
        addS3Object(
            bucketName = bucketName,
            s3Object = s3Object
        )
        addS3ObjectSummary(
            bucketName = bucketName,
            summary = S3ObjectSummary(
                key = s3Object.key,
                bucket = bucketName.value,
                lastModified = lastModifiedDate
            )
        )
    }

    private fun addS3Object(bucketName: BucketName, s3Object: S3Object) = apply {
        objects.merge(bucketName, mutableListOf(s3Object)) { a, b -> (a + b).toMutableList() }
    }

    private fun addS3ObjectSummary(bucketName: BucketName, summary: S3ObjectSummary) = apply {
        summaries.merge(bucketName, mutableListOf(summary)) { a, b -> (a + b).toMutableList() }
    }

    fun isEmpty(bucketName: BucketName) = objects.getOrDefault(bucketName, emptyList()).isEmpty()
    fun isEmpty() = objects.isEmpty()

    override fun toString() = "FakeS3(summaries=$summaries)"
}

inline fun <reified T : FakeS3> Assertion.Builder<T>.getBucket(bucketName: BucketName) =
    get(FakeS3::objects).getValue(bucketName)

fun Assertion.Builder<MutableList<S3Object>>.getObject(objectKey: String) =
    first { it.key == objectKey }

fun Assertion.Builder<MutableList<S3Object>>.getObject(objectKey: ObjectKey) =
    first { it.key == objectKey.value }

inline fun <reified T : FakeS3> Assertion.Builder<T>.isEmpty() {
    assertThat("is empty") { s3 -> s3.isEmpty() }
}

inline fun <reified T : FakeS3> Assertion.Builder<T>.isEmpty(name: String) = isEmpty(BucketName.of(name))

inline fun <reified T : FakeS3> Assertion.Builder<T>.isEmpty(bucketName: BucketName) =
    assertThat("bucket %s is empty", bucketName) { s3 -> s3.isEmpty(bucketName) }

inline fun <reified T : Any> Assertion.Builder<S3ObjectInputStream>.withReadJsonOrThrows(noinline block: Assertion.Builder<T>.() -> Unit) =
    with({ Json.readJsonOrThrow(this) }, block)
