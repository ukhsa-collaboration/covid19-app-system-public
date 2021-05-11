package uk.nhs.nhsx.analyticsedge

import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.ObjectKey

data class S3PutObjectEvent(val bucketName: BucketName, val key: ObjectKey)

enum class Dataset(val datasetName: String) {
    Adoption("app_adoption.csv"),
    Aggregate("app_aggregate.csv"),
    Enpic("app_enpic.csv"),
    Isolation("app_isolation.csv"),
    Poster("app_posters.csv")
}
