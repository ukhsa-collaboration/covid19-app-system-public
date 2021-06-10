package uk.nhs.nhsx.testhelper.mocks

import com.amazonaws.services.s3.model.S3Object
import com.amazonaws.services.s3.model.S3ObjectSummary
import uk.nhs.nhsx.core.aws.s3.AwsS3
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.Locator
import java.util.*

open class FakeS3 : FakeS3Storage(), AwsS3 {

    val existing: MutableList<S3ObjectSummary> = mutableListOf()
    val deleted: MutableList<Locator> = mutableListOf()
    val copied: MutableList<Pair<Locator, Locator>> = mutableListOf()

    override fun getObjectSummaries(bucketName: BucketName) = existing

    override fun deleteObject(locator: Locator) {
        deleted.add(locator)
    }

    override fun copyObject(from: Locator, to: Locator) {
        copied.add(from to to)
    }

    override fun getObject(locator: Locator): Optional<S3Object> = TODO("james didn't write")
}
