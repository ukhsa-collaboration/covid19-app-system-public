package uk.nhs.nhsx.diagnosiskeydist.keydistribution

import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import java.io.File

fun interface KeyDistributor {

    fun distribute(
        name: BucketName,
        key: ObjectKey,
        binFile: File,
        sigFile: File
    )
}
