package uk.nhs.nhsx.core.aws.cloudfront

fun interface AwsCloudFront {
    fun invalidateCache(distributionId: String, path: String)
}
