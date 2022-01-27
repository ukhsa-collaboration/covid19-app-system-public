package uk.nhs.nhsx.core.aws.s3

import com.amazonaws.services.s3.model.S3Object
import com.amazonaws.services.s3.model.S3ObjectSummary
import uk.nhs.nhsx.core.ContentType
import java.net.URL
import java.util.*

interface AwsS3 {
    fun upload(locator: Locator, contentType: ContentType, bytes: ByteArraySource, metaHeaders: List<MetaHeader>)
    fun upload(locator: Locator, contentType: ContentType, bytes: ByteArraySource) {
        upload(locator, contentType, bytes, listOf())
    }

    fun getObjectSummaries(bucketName: BucketName): List<S3ObjectSummary>
    fun getObject(locator: Locator): Optional<S3Object>
    fun deleteObject(locator: Locator)
    fun copyObject(from: Locator, to: Locator)
    fun getSignedURL(locator: Locator, expiration: Date): Optional<URL>
}
