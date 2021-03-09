package uk.nhs.nhsx.testhelper.mocks

import com.amazonaws.services.s3.model.S3Object
import com.amazonaws.services.s3.model.S3ObjectSummary
import uk.nhs.nhsx.core.aws.s3.AwsS3
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.Locator
import java.net.URL
import java.util.*

open class FakeS3 : FakeS3Storage(), AwsS3 {

    val existing: MutableList<S3ObjectSummary> = mutableListOf()
    val deleted: MutableList<Locator> = mutableListOf()

    override fun getObjectSummaries(bucketName: BucketName): MutableList<S3ObjectSummary> {
        return existing
    }

    override fun getObject(locator: Locator): Optional<S3Object> {
        throw UnsupportedOperationException("james didn't write")
    }

    override fun deleteObject(locator: Locator) {
        deleted.add(locator)
    }
}
