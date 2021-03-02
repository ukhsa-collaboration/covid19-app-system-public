package uk.nhs.nhsx.testhelper.mocks

import org.apache.http.entity.ContentType
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.ByteArraySource
import uk.nhs.nhsx.core.aws.s3.Locator
import uk.nhs.nhsx.core.aws.s3.MetaHeader
import uk.nhs.nhsx.core.aws.s3.S3Storage
import java.util.*

class FakeS3StorageMultipleObjects : S3Storage {
    var count = 0
    var bucket: BucketName? = null
    var fakeS3Objects: MutableList<FakeS3Object> = ArrayList()

    override fun upload(
        locator: Locator,
        contentType: ContentType,
        bytes: ByteArraySource,
        meta: List<MetaHeader>
    ) {
        count++
        bucket = locator.bucket
        fakeS3Objects.add(FakeS3Object(locator.key, contentType, bytes, meta))
    }
}
