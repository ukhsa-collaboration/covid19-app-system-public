package uk.nhs.nhsx.testhelper.mocks

import com.amazonaws.services.s3.model.S3Object
import org.apache.http.entity.ContentType
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.ByteArraySource
import uk.nhs.nhsx.core.aws.s3.Locator
import uk.nhs.nhsx.core.aws.s3.MetaHeader
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.core.aws.s3.S3Storage
import java.net.URL
import java.util.*

open class FakeS3Storage : S3Storage {
    var count: Int = 0
    lateinit var bucket: BucketName
    lateinit var name: ObjectKey
    lateinit var contentType: ContentType
    lateinit var bytes: ByteArraySource
    val meta: MutableList<MetaHeader> = mutableListOf()
    val exists: Optional<S3Object> = Optional.empty()

    override fun upload(
        locator: Locator,
        contentType: ContentType,
        bytes: ByteArraySource,
        meta: List<MetaHeader>
    ) {
        overwriting(locator, contentType, bytes, meta.toList())
    }
    override fun getSignedURL(locator: Locator?, expiration: Date?): Optional<URL> {
        return Optional.of(URL("https://example.com"))
    }

    private fun overwriting(
        locator: Locator,
        contentType: ContentType,
        bytes: ByteArraySource,
        meta: List<MetaHeader>
    ) {
        count++
        bucket = locator.bucket
        name = locator.key
        this.contentType = contentType
        this.bytes = bytes
        this.meta += meta
    }
}
