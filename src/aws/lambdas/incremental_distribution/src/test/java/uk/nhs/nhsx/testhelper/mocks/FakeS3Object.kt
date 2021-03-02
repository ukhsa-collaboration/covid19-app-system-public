package uk.nhs.nhsx.testhelper.mocks

import org.apache.http.entity.ContentType
import uk.nhs.nhsx.core.aws.s3.ByteArraySource
import uk.nhs.nhsx.core.aws.s3.MetaHeader
import uk.nhs.nhsx.core.aws.s3.ObjectKey

data class FakeS3Object(
    val name: ObjectKey,
    val contentType: ContentType,
    val bytes: ByteArraySource,
    val meta: List<MetaHeader>
)
