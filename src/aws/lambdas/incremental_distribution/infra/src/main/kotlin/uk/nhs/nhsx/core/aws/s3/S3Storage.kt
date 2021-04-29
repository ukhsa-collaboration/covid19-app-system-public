package uk.nhs.nhsx.core.aws.s3

import uk.nhs.nhsx.core.ContentType
import java.net.URL
import java.util.*

interface S3Storage {
    fun upload(
        locator: Locator,
        contentType: ContentType,
        bytes: ByteArraySource,
        metaHeaders: List<MetaHeader>
    )

    fun upload(
        locator: Locator,
        contentType: ContentType,
        bytes: ByteArraySource
    ) {
        upload(locator, contentType, bytes, listOf())
    }

    fun getSignedURL(
        locator: Locator,
        expiration: Date
    ): Optional<URL>
}
