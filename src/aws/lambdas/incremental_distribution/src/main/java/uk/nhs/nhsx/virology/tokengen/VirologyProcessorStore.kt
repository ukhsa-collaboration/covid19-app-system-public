package uk.nhs.nhsx.virology.tokengen

import org.apache.http.Consts
import org.apache.http.entity.ContentType
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.ByteArraySource.Companion.fromFile
import uk.nhs.nhsx.core.aws.s3.ByteArraySource.Companion.fromUtf8String
import uk.nhs.nhsx.core.aws.s3.Locator
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.core.aws.s3.S3Storage
import java.net.URL
import java.util.Date

class VirologyProcessorStore(
    private val s3Client: S3Storage,
    private val bucketName: BucketName
) {
    fun storeCsv(ctaTokensCsv: CtaTokensCsv) {
        s3Client.upload(
            Locator.of(bucketName, ObjectKey.of(ctaTokensCsv.filename)),
            ContentType.create("text/csv", Consts.UTF_8),
            fromUtf8String(ctaTokensCsv.content)
        )
    }

    fun storeZip(ctaTokensZip: CtaTokensZip) {
        s3Client.upload(
            Locator.of(bucketName, ObjectKey.of(ctaTokensZip.filename)),
            ContentType.create("application/zip"),
            fromFile(ctaTokensZip.content)
        )
    }

    fun generateSignedURL(filename: String, expirationDate: Date): URL? = s3Client.getSignedURL(
        Locator.of(bucketName, ObjectKey.of(filename)),
        expirationDate
    ).orElse(null)
}
