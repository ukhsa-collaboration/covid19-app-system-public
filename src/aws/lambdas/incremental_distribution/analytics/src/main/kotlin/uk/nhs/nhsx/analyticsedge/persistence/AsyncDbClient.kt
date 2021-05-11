package uk.nhs.nhsx.analyticsedge.persistence

import uk.nhs.nhsx.core.aws.s3.BucketName

interface AsyncDbClient {
    fun submitQueryWithOutputLocation(sqlQuery: String, outputBucket: BucketName, prefix: String)
}
