package uk.nhs.nhsx.sanity.stores.common

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import software.amazon.awssdk.services.s3.S3Client
import uk.nhs.nhsx.sanity.stores.S3SanityCheck
import uk.nhs.nhsx.sanity.stores.config.CTAStore
import uk.nhs.nhsx.sanity.stores.config.S3BucketConfig

class HealthSanityChecks : S3SanityCheck() {

    @MethodSource("ctaStores")
    @ParameterizedTest(name = "Access S3 bucket {arguments}")
    fun `check s3 bucket exists`(s3BucketConfig: S3BucketConfig) {
        assert(checkS3BucketExists(s3BucketConfig.name))
    }

    private fun checkS3BucketExists(bucketName: String): Boolean {
        val s3 = S3Client.builder().build()
        val listBucketResponse = s3.listBuckets()
        return listBucketResponse.buckets().any { it.name() == bucketName }
    }

    @Suppress("unused")
    companion object {
        @JvmStatic
        private fun ctaStores() = stores().filterIsInstance<CTAStore>()
    }
}
