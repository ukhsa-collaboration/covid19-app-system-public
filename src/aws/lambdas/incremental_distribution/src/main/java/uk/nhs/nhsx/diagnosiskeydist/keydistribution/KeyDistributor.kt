package uk.nhs.nhsx.diagnosiskeydist.keydistribution

import kotlin.Throws
import java.io.IOException
import java.security.NoSuchAlgorithmException
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import java.io.File

fun interface KeyDistributor {
    @Throws(IOException::class, NoSuchAlgorithmException::class)
    fun distribute(name: BucketName, key: ObjectKey, binFile: File, sigFile: File)
}
