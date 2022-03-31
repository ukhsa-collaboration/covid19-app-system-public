package uk.nhs.nhsx.analyticssubmission

import uk.nhs.nhsx.analyticssubmission.policy.PolicyConfig
import uk.nhs.nhsx.core.Environment

data class AnalyticsConfig(
    val firehoseStreamName: String,
    val firehoseIngestEnabled: Boolean,
    val policyConfig: PolicyConfig
) {
    companion object {
        private val FIREHOSE_STREAM = Environment.EnvironmentKey.string("firehose_stream_name")
        private val FIREHOSE_INGEST_ENABLED = Environment.EnvironmentKey.bool("firehose_ingest_enabled")

        fun from(e: Environment) = AnalyticsConfig(
            firehoseStreamName = e.access.required(FIREHOSE_STREAM),
            firehoseIngestEnabled = e.access.required(FIREHOSE_INGEST_ENABLED),
            policyConfig = PolicyConfig.from(e)
        )
    }
}
