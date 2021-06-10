package uk.nhs.nhsx.testhelper.mocks

import uk.nhs.nhsx.core.ContentType
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.ByteArraySource
import uk.nhs.nhsx.core.aws.s3.Locator
import uk.nhs.nhsx.core.aws.s3.MetaHeader
import uk.nhs.nhsx.core.aws.s3.S3Storage
import java.net.URL
import java.util.ArrayList
import java.util.Date
import java.util.Optional

class FakeS3StorageMultipleObjects : S3Storage {
    var count = 0
    var bucket: BucketName? = null
    var fakeS3Objects: MutableList<FakeS3Object> = ArrayList()

    override fun upload(
        locator: Locator,
        contentType: ContentType,
        bytes: ByteArraySource,
        metaHeaders: List<MetaHeader>
    ) {
        count++
        bucket = locator.bucket
        fakeS3Objects.add(FakeS3Object(locator.key, contentType, bytes, metaHeaders))
    }

    override fun getSignedURL(locator: Locator, expiration: Date) =
        Optional.of(URL("https://example.com"))
}
