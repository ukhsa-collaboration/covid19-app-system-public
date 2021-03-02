package uk.nhs.nhsx.diagnosiskeydist.keydistribution

import org.apache.http.entity.ContentType
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.ByteArraySource.Companion.fromFile
import uk.nhs.nhsx.core.aws.s3.Locator
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.core.aws.s3.S3Storage
import uk.nhs.nhsx.core.signature.DatedSigner
import uk.nhs.nhsx.core.signature.DistributionSignature
import uk.nhs.nhsx.core.signature.SigningHeaders.fromDatedSignature
import java.io.File

class UploadToS3KeyDistributor(
    private val s3Storage: S3Storage,
    private val signer: DatedSigner
) : KeyDistributor {

    override fun distribute(name: BucketName, key: ObjectKey, binFile: File, sigFile: File) {
        File.createTempFile("export", ".zip").let {
            try {
                KeyFileUtility.zipFiles(it, binFile, sigFile)

                val byteSource = fromFile(it)
                val signatureResult = signer.sign(DistributionSignature(byteSource))

                s3Storage.upload(
                    Locator.of(name, key),
                    ContentType.create("application/zip"),
                    byteSource,
                    fromDatedSignature(signatureResult)
                )
            } finally {
                it.delete()
            }
        }
    }
}
