@file:Suppress("TestFunctionName")

package uk.nhs.nhsx.testhelper.s3

import com.amazonaws.services.s3.model.S3ObjectSummary
import java.time.Instant
import java.util.*

fun S3ObjectSummary(
    key: String,
    bucket: String? = null,
    lastModified: Instant? = null
) = S3ObjectSummary().apply {
    this.key = key
    if (bucket != null) this.bucketName = bucket
    if (lastModified != null) this.lastModified = Date.from(lastModified)
}
