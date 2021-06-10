package uk.nhs.nhsx.analyticsedge

import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.ObjectKey

data class S3PutObjectEvent(val bucketName: BucketName, val key: ObjectKey)

enum class Dataset(private val datasetName: String) {
    Adoption("app-adoption.csv"),
    Aggregate("app-aggregate.csv"),
    Enpic("app-enpic.csv"),
    Isolation("app-isolation.csv"),
    Poster("app-posters.csv");

    fun filename(prefix: String) = "$prefix-$datasetName"
}
