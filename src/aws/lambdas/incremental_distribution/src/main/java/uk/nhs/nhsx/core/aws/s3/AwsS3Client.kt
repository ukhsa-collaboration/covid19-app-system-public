package uk.nhs.nhsx.core.aws.s3

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.AmazonS3Exception
import com.amazonaws.services.s3.model.ListObjectsV2Request
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectRequest
import com.amazonaws.services.s3.model.S3Object
import com.amazonaws.services.s3.model.S3ObjectSummary
import org.apache.http.entity.ContentType
import uk.nhs.nhsx.core.events.Events
import java.io.IOException
import java.net.URL
import java.util.*

class AwsS3Client(private val events: Events) : AwsS3 {

    private val client: AmazonS3 = AmazonS3ClientBuilder.defaultClient()

    override fun upload(
        locator: Locator,
        contentType: ContentType,
        bytes: ByteArraySource,
        meta: List<MetaHeader>
    ) {
        events.emit(javaClass, S3Upload(locator.key, locator.bucket))

        val metadata = ObjectMetadata().apply {
            this.contentType = contentType.mimeType
            this.contentLength = bytes.size.toLong()
            meta.forEach { addUserMetadata(it.asS3MetaName(), it.value) }
        }

        try {
            bytes.openStream().use {
                client.putObject(PutObjectRequest(locator.bucket.value, locator.key.value, it, metadata))
            }
        } catch (e: IOException) {
            throw RuntimeException("Unable to open data source", e)
        }
    }

    override fun getObjectSummaries(bucketName: BucketName): List<S3ObjectSummary> {
        val request = ListObjectsV2Request().apply {
            this.bucketName = bucketName.value
        }

        var result = client.listObjectsV2(request)

        val objectSummaries = result.objectSummaries?.toMutableList() ?: mutableListOf()

        while (result.isTruncated) {
            request.continuationToken = result.nextContinuationToken
            result = client.listObjectsV2(request)
            objectSummaries.addAll(result.objectSummaries)
        }

        return objectSummaries
    }

    override fun getObject(locator: Locator): Optional<S3Object> {
        return try {
            Optional.ofNullable(client.getObject(locator.bucket.value, locator.key.value))
        } catch (e: AmazonS3Exception) {
            if (!e.is404()) {
                events.emit(javaClass, S3Error(locator, e.statusCode, e.errorCode))
            }
            Optional.empty()
        }
    }



    private fun AmazonS3Exception.is404() = statusCode == 404 && errorCode == "NoSuchKey"

    override fun deleteObject(locator: Locator) = client.deleteObject(locator.bucket.value, locator.key.value)

    override fun getSignedURL(locator: Locator, expiration: Date): Optional<URL> {
        return try {
            Optional.ofNullable(client.generatePresignedUrl(locator.bucket.value,locator.key.value,expiration))
        } catch (e: AmazonS3Exception) {
            if (!e.is404()) {
                events.emit(javaClass, S3Error(locator, e.statusCode, e.errorCode))
            }
            Optional.empty()
        }
    }
}
