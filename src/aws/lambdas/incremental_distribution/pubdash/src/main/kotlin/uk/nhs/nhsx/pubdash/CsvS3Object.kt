package uk.nhs.nhsx.pubdash

import uk.nhs.nhsx.core.aws.s3.ObjectKey

interface CsvS3Object {
    fun objectKey(): ObjectKey
    fun csv(): String
}
