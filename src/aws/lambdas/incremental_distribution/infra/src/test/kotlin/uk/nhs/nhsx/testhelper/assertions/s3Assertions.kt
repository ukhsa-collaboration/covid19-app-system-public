package uk.nhs.nhsx.testhelper.assertions

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.ObjectListing
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.S3Object
import com.amazonaws.services.s3.model.S3ObjectInputStream
import com.amazonaws.services.s3.model.S3ObjectSummary
import strikt.api.Assertion
import strikt.api.DescribeableBuilder
import strikt.assertions.isEqualTo
import strikt.assertions.map
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import java.io.InputStream

object S3Assertions {
    fun Assertion.Builder<AmazonS3>.listObjects(bucketName: String) =
        get("listObjects") { listObjects(bucketName) }

    val Assertion.Builder<ObjectListing>.objectSummaries
        get() = get(ObjectListing::getObjectSummaries)

    val Assertion.Builder<ObjectListing>.keys
        get() = get("keys") { objectSummaries.map(S3ObjectSummary::getKey) }

    fun Assertion.Builder<AmazonS3>.getObject(bucketName: String, key: String) =
        get("getObject") { getObject(bucketName, key) }
}

object S3ObjectSummaryAssertions {
    val Assertion.Builder<S3ObjectSummary>.key
        get() = get(S3ObjectSummary::getKey)
}

object S3ObjectAssertions {
    val Assertion.Builder<S3Object>.key
        get() = get(S3Object::getKey)

    val Assertion.Builder<S3Object>.content
        get() = get(S3Object::getObjectContent)

    fun Assertion.Builder<S3ObjectInputStream>.asString() =
        get(S3ObjectInputStream::readAllBytes).get(::String)

    val Assertion.Builder<S3Object>.metadata
        get() = get(S3Object::getObjectMetadata)

    val Assertion.Builder<S3Object>.bucketName
        get() = get(S3Object::getBucketName)

    val Assertion.Builder<ObjectMetadata>.contentType
        get() = get(ObjectMetadata::getContentType)

    val Assertion.Builder<ObjectMetadata>.lastModified
        get() = get(ObjectMetadata::getLastModified)

    val Assertion.Builder<ObjectMetadata>.userMetadata
        get() = get(ObjectMetadata::getUserMetadata)
}

