package uk.nhs.nhsx.testhelper.mocks

import com.google.common.io.ByteSource
import org.apache.http.entity.ContentType
import uk.nhs.nhsx.core.aws.s3.MetaHeader
import uk.nhs.nhsx.core.aws.s3.ObjectKey

data class FakeS3Object(val name: ObjectKey,
                        val contentType: ContentType,
                        val bytes: ByteSource,
                        val meta: List<MetaHeader>)