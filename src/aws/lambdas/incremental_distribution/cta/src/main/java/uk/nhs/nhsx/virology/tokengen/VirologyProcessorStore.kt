package uk.nhs.nhsx.virology.tokengen

import uk.nhs.nhsx.core.ContentType.Companion.APPLICATION_ZIP
import uk.nhs.nhsx.core.ContentType.Companion.TEXT_CSV
import uk.nhs.nhsx.core.aws.s3.AwsS3
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.ByteArraySource.Companion.fromFile
import uk.nhs.nhsx.core.aws.s3.ByteArraySource.Companion.fromUtf8String
import uk.nhs.nhsx.core.aws.s3.Locator
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import java.net.URL
import java.util.*

class VirologyProcessorStore(
    private val awsS3: AwsS3,
    private val bucketName: BucketName
) {
    fun storeCsv(ctaTokensCsv: CtaTokensCsv) {
        awsS3.upload(
            Locator.of(bucketName, ObjectKey.of(ctaTokensCsv.filename)),
            TEXT_CSV,
            fromUtf8String(ctaTokensCsv.content)
        )
    }

    fun storeZip(ctaTokensZip: CtaTokensZip) {
        awsS3.upload(
            Locator.of(bucketName, ObjectKey.of(ctaTokensZip.filename)),
            APPLICATION_ZIP,
            fromFile(ctaTokensZip.content)
        )
    }

    fun generateSignedURL(filename: String, expirationDate: Date) = awsS3
        .getSignedURL(
            Locator.of(bucketName, ObjectKey.of(filename)),
            expirationDate
        )
}
