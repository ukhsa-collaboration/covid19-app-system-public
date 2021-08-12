package uk.nhs.nhsx.diagnosiskeydist

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import uk.nhs.nhsx.core.TestEnvironments
import java.time.Duration
import java.util.*

class BatchProcessingConfigTest {

    @Test
    fun `defaults to 15 minute offset if not overridden in environment`() {
        val config = BatchProcessingConfig.fromEnvironment(
            TestEnvironments.TEST.apply(
                mapOf(
                    "ABORT_OUTSIDE_TIME_WINDOW" to "false",
                    "DISTRIBUTION_BUCKET_NAME" to "distribution-bucket-name",
                    "DISTRIBUTION_ID" to UUID.randomUUID().toString(),
                    "DISTRIBUTION_PATTERN_DAILY" to "/distribution/daily/*",
                    "DISTRIBUTION_PATTERN_2HOURLY" to "/distribution/two-hourly/*",
                    "SSM_AG_SIGNING_KEY_ID_PARAMETER_NAME" to "/app/kms/SigningKeyArn",
                    "SSM_METADATA_SIGNING_KEY_ID_PARAMETER_NAME" to "/app/kms/ContentSigningKeyArn",
                )
            )
        )

        expectThat(config.zipSubmissionPeriodOffset).isEqualTo(Duration.ofMinutes(-15))
    }

    @Test
    fun `offset can be overridden in environment`() {
        val config = BatchProcessingConfig.fromEnvironment(
            TestEnvironments.TEST.apply(
                mapOf(
                    "ABORT_OUTSIDE_TIME_WINDOW" to "false",
                    "DISTRIBUTION_BUCKET_NAME" to "distribution-bucket-name",
                    "DISTRIBUTION_ID" to UUID.randomUUID().toString(),
                    "DISTRIBUTION_PATTERN_DAILY" to "/distribution/daily/*",
                    "DISTRIBUTION_PATTERN_2HOURLY" to "/distribution/two-hourly/*",
                    "SSM_AG_SIGNING_KEY_ID_PARAMETER_NAME" to "/app/kms/SigningKeyArn",
                    "SSM_METADATA_SIGNING_KEY_ID_PARAMETER_NAME" to "/app/kms/ContentSigningKeyArn",
                    "ZIP_SUBMISSION_PERIOD_OFFSET" to "PT-20M",
                )
            )
        )

        expectThat(config.zipSubmissionPeriodOffset).isEqualTo(Duration.ofMinutes(-20))
    }
}
