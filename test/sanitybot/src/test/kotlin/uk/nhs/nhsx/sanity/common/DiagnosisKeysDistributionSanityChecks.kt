package uk.nhs.nhsx.sanity.common

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.lessThanOrEqualTo
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request
import uk.nhs.nhsx.sanity.AwsSanityCheck
import uk.nhs.nhsx.sanity.config.DeployedApiResource.DiagnosisKeysDistribution
import uk.nhs.nhsx.sanity.config.Distribution

class DiagnosisKeysDistributionSanityChecks : AwsSanityCheck() {

    //    Check two-hourly diagnosis key distribution - GET objects from bucket <= 168✅

    @Test
    fun `2 hourly Diagnosis distribution returns a 200 and matches resource`() {
        val lambda = env.configFor(DiagnosisKeysDistribution, "diagnosis_keys_distribution_2hourly") as Distribution
        assertThat(countObjectsIn(lambda, "distribution/two-hourly"), lessThanOrEqualTo(168))
    }

    private fun countObjectsIn(lambda: Distribution, type: String): Int {
        val s3 = S3Client.builder().build()
        return s3.listObjectsV2(ListObjectsV2Request.builder().bucket(lambda.storeName).build()).contents()
            .filter { it.key().startsWith(type) }.size
    }

}
