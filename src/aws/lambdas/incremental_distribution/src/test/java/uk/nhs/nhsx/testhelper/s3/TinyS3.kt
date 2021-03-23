package uk.nhs.nhsx.testhelper.s3

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.AnonymousAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.fasterxml.jackson.annotation.JsonRootName
import com.fasterxml.jackson.databind.PropertyNamingStrategies.UpperCamelCaseStrategy
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.PUT
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.CONFLICT
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.NOT_IMPLEMENTED
import org.http4k.core.Status.Companion.NO_CONTENT
import org.http4k.core.Status.Companion.OK
import org.http4k.lens.Header
import org.http4k.lens.Path
import org.http4k.lens.Query
import org.http4k.lens.int
import org.http4k.lens.nonEmptyString
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import uk.nhs.nhsx.testhelper.s3.TinyS3Model.Bucket
import uk.nhs.nhsx.testhelper.s3.TinyS3Model.BucketContents
import uk.nhs.nhsx.testhelper.s3.TinyS3Model.BucketName
import uk.nhs.nhsx.testhelper.s3.TinyS3Model.ListAllMyBucketsResult
import uk.nhs.nhsx.testhelper.s3.TinyS3Model.ListBucketResultV2
import uk.nhs.nhsx.testhelper.s3.TinyS3Model.ObjectKey
import uk.nhs.nhsx.testhelper.s3.TinyS3Model.Owner
import uk.nhs.nhsx.testhelper.s3.TinyS3Model.S3Object
import java.io.ByteArrayInputStream
import java.lang.String.CASE_INSENSITIVE_ORDER
import java.math.BigInteger
import java.security.MessageDigest
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.Locale.ENGLISH
import java.util.concurrent.ConcurrentHashMap

object TinyS3Model {
    data class ObjectKey(val value: String)

    data class BucketName(val value: String)

    data class Bucket(
        val name: String,
        val creationDate: Instant
    )

    data class ListAllMyBucketsResult(
        val owner: Owner,
        val buckets: List<Bucket>
    )

    data class Owner(
        val id: Long = 0,
        val displayName: String
    )

    @JsonRootName("ListBucketResult")
    data class ListBucketResultV2(
        val name: String,
        val prefix: String? = null,
        val keyCount: Int = 0,
        val maxKeys: Int = 1000,
        @get:JacksonXmlProperty(localName = "IsTruncated") val isTruncated: Boolean = false,
        val commonPrefixes: CommonPrefixes? = null,
        val continuationToken: String? = null,
        val nextContinuationToken: String? = null,
        val startAfter: String? = null,
        @get:JacksonXmlElementWrapper(useWrapping = false)
        val contents: List<BucketContents> = listOf()
    )

    data class CommonPrefixes(@JacksonXmlElementWrapper(useWrapping = false) val prefix: List<String>)

    @JsonRootName("Contents")
    data class BucketContents(
        val key: String,
        val lastModified: Instant,
        @get:JacksonXmlProperty(localName = "ETag")
        val etag: String,
        val size: Long,
        val owner: Owner,
        val storageClass: String = "STANDARD"
    )

    data class S3Object(
        val name: String,
        val size: Long,
        val creationDate: Instant,
        val modificationDate: Instant,
        val lastModified: Instant,
        val md5: String,
        val contentType: String,
        val content: String,
        val userMetadata: Map<String, String>,
        val contentEncoding: String? = null,
        val isEncrypted: Boolean? = false,
        val kmsEncryption: String? = null,
        val kmsKeyId: String? = null,
        val tags: List<Tag>? = null
    )

    data class Tag(
        val key: String,
        val value: String
    )
}

interface TinyS3Storage {
    fun bucket(name: BucketName): Bucket?
    fun buckets(): List<Bucket>
    fun create(name: BucketName)
    fun delete(name: BucketName)
    fun putS3Object(name: BucketName, value: S3Object)
    fun getS3Objects(name: BucketName): Set<S3Object>
    fun getS3Object(name: BucketName, key: ObjectKey): S3Object?
    fun deleteS3Object(name: BucketName, key: ObjectKey)
}

class TinyS3InMemoryStorage(private val clock: Clock) : TinyS3Storage {
    private val buckets = ConcurrentHashMap<BucketName, Bucket>()
    private val objectsBuBucket = ConcurrentHashMap<BucketName, ConcurrentHashMap<String, S3Object>>()

    override fun create(name: BucketName) {
        buckets[name] = Bucket(name.value, clock.instant())
    }

    override fun delete(name: BucketName) {
        buckets.remove(name)
        objectsBuBucket.remove(name)
    }

    override fun bucket(name: BucketName): Bucket? = buckets[name]
    override fun buckets(): List<Bucket> = buckets.values.toList()
    override fun putS3Object(name: BucketName, value: S3Object) {
        objectsBuBucket.merge(name, ConcurrentHashMap(mapOf(value.name to value))) { k, v -> k.also { it.putAll(v) } }
    }

    override fun getS3Objects(name: BucketName): Set<S3Object> = objectsBuBucket[name]?.values?.toSet() ?: setOf()
    override fun getS3Object(name: BucketName, key: ObjectKey): S3Object? =
        (objectsBuBucket[name] ?: mutableMapOf())[key.value]

    override fun deleteS3Object(name: BucketName, key: ObjectKey) {
        (objectsBuBucket[name] ?: mutableMapOf()).remove(key.value)
    }
}

class TinyS3(
    val port: Int,
    private val clock: Clock = Clock.systemUTC(),
    private val storage: TinyS3Storage = TinyS3InMemoryStorage(clock)
) {
    private val mapper = XmlMapper()
        .setPropertyNamingStrategy(UpperCamelCaseStrategy())
        .registerModule(Jdk8Module())
        .registerModule(JavaTimeModule())
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)

    private val contentType = Header.required("Content-Type")
    private val bucketName = Path.nonEmptyString().map(::BucketName).of("name")
    private val objectKey = Path.nonEmptyString().map(::ObjectKey).of("objectKey")
    private val prefix = Query.optional("prefix")
    private val maxKeys = Query.int().defaulted("max-keys", 1000)
    private val continuationToken = Query.optional("continuation-token")

    companion object {
        private val TINY_S3_OWNER = Owner(999, "tiny-s3-server")
        private val RFC7232_FORMATTER = DateTimeFormatter
            .ofPattern("EEE, dd MMM yyyy HH:mm:ss z", ENGLISH)
            .withZone(ZoneId.of("GMT"))
    }

    private val app = routes(
        // Lists all existing buckets.
        "/" bind GET to {
            Response(OK)
                .header("Content-Type", "application/xml")
                .body(ListAllMyBucketsResult(TINY_S3_OWNER, storage.buckets()).toXml())
        },

        // Creates a bucket.
        "/{name}" bind PUT to { r ->
            storage.create(bucketName(r))
            Response(OK)
        },

        // Deletes a specified bucket.
        "/{name}" bind DELETE to { r ->
            val bucketName = bucketName(r)
            when {
                storage.bucket(bucketName) == null -> Response(NOT_FOUND)
                storage.getS3Objects(bucketName).isEmpty() -> Response(CONFLICT)
                else -> {
                    storage.delete(bucketName)
                    Response(NO_CONTENT)
                }
            }
        },

        // Retrieve list of objects of a bucket.
        // https://docs.aws.amazon.com/AmazonS3/latest/API/v2-RESTBucketGET.html
        "/{name}" bind GET to { r ->
            val prefix = prefix(r)
            val maxKeys = maxKeys(r)
            val bucketName = bucketName(r)
            val continuationToken = continuationToken(r)

            when {
                continuationToken != null -> Response(NOT_IMPLEMENTED)
                storage.bucket(bucketName) == null -> Response(NOT_FOUND)
                maxKeys < 0 -> Response(BAD_REQUEST, "maxKeys should be non-negative")
                else -> {
                    val prefixFilter: (S3Object) -> Boolean = prefix?.let { { it.name.startsWith(prefix) } } ?: { true }

                    val objects = storage.getS3Objects(bucketName)
                        .filter(prefixFilter)
                        .map { BucketContents(it.name, it.modificationDate, it.md5, it.size, TINY_S3_OWNER) }
                        .sortedBy { it.key }
                        .take(maxKeys)

                    val isTruncated = maxKeys < objects.size

                    val result = ListBucketResultV2(
                        name = bucketName.value,
                        keyCount = objects.size,
                        maxKeys = maxKeys,
                        isTruncated = isTruncated,
                        continuationToken = if (isTruncated) UUID.randomUUID().toString() else null,
                        contents = objects
                    )

                    Response(OK)
                        .header("Content-Type", "application/xml")
                        .body(result.toXml())
                }
            }
        },

        "/{name}" bind routes(
            // Adds an object to a bucket.
            // http://docs.aws.amazon.com/AmazonS3/latest/API/RESTObjectPUT.html
            "{objectKey:.*}" bind PUT to { r ->
                val bucketName = bucketName(r)
                val objectKey = objectKey(r)
                val payload = r.body.payload.array()
                val userMetaData = userMetaData(r)

                storage.bucket(bucketName)?.let {
                    val now = clock.instant()
                    val s3Object = S3Object(
                        name = objectKey.value,
                        contentType = contentType(r),
                        content = payload.encodeBase64(),
                        size = payload.size.toLong(),
                        creationDate = now,
                        modificationDate = userMetaData["last-modified"]?.let { Instant.parse(it) } ?: now,
                        lastModified = userMetaData["last-modified"]?.let { Instant.parse(it) } ?: now,
                        userMetadata = userMetaData,
                        md5 = payload.md5()
                    )

                    storage.putS3Object(bucketName, s3Object)

                    Response(OK)
                        .header("ETag", """"${s3Object.md5}"""")
                        .header("Last-Modified", RFC7232_FORMATTER.format(s3Object.lastModified))
                        .apply { s3Object.userMetadata.forEach { (k, v) -> header("x-amz-meta-$k", v) } }
                } ?: Response(NOT_FOUND)
            },

            // Returns the File identified by bucketName and fileName.
            // http://docs.aws.amazon.com/AmazonS3/latest/API/RESTObjectGET.html
            "{objectKey:.*}" bind GET to { r ->
                storage.getS3Object(bucketName(r), objectKey(r))?.let {
                    Response(OK)
                        .header("ETag", """"${it.md5}"""")
                        .header("Content-Type", it.contentType)
                        .header("Accept-Ranges", "bytes")
                        .header("Access-Control-Allow-Origin", "*")
                        .header("Last-Modified", RFC7232_FORMATTER.format(it.lastModified))
                        .apply { it.userMetadata.forEach { (k, v) -> header("x-amz-meta-$k", v) } }
                        .body(ByteArrayInputStream(it.content.decodeBase64()))
                } ?: Response(NOT_FOUND)
            },

            // The DELETE operation removes an object.
            // http://docs.aws.amazon.com/AmazonS3/latest/API/RESTObjectDELETE.html
            "{objectKey:.*}" bind DELETE to { r ->
                storage.deleteS3Object(bucketName(r), objectKey(r))
                Response(NO_CONTENT)
            })
    )

    private val server = app.asServer(SunHttp(port))

    fun start() = apply {
        server.start()
    }

    fun stop() = apply {
        server.stop()
    }

    fun client(): AmazonS3 = AmazonS3ClientBuilder
        .standard()
        .withPathStyleAccessEnabled(true)
        .withEndpointConfiguration(
            AwsClientBuilder.EndpointConfiguration(
                "http://localhost:${server.port()}",
                "us-west-2"
            )
        )
        .withCredentials(AWSStaticCredentialsProvider(AnonymousAWSCredentials()))
        .build()

    private fun Any.toXml() = mapper.writeValueAsString(this)
    private fun ByteArray.encodeBase64() = Base64.getUrlEncoder().encodeToString(this)
    private fun String.decodeBase64() = Base64.getUrlDecoder().decode(this.toByteArray())
    private fun ByteArray.md5(): String = BigInteger(1, MessageDigest.getInstance("MD5").digest(this))
        .toString(16)
        .padStart(32, '0')

    private val userMetaData: (Request) -> Map<String, String> = { r ->
        TreeMap<String, String>(CASE_INSENSITIVE_ORDER).apply {
            putAll(r.headers
                .toMap()
                .filterNotNullValues()
                .filterKeys { it.startsWith("x-amz-meta-", true) }
                .mapKeys { (k, _) -> k.substring("x-amz-meta-".length) }
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <K, V> Map<K, V?>.filterNotNullValues() = filterValues { it != null } as Map<K, V>
}
