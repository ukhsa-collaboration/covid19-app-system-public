package uk.nhs.nhsx.sanity.stores.config

import com.fasterxml.jackson.databind.JsonNode
import kotlin.reflect.KFunction1

data class Store(val store: DeployedStore, val name: String)

enum class DeployedStore(
    val converter: KFunction1<Store, (JsonNode) -> S3BucketConfig>,
    val storeReference: String
) {
    AnalyticsSubmissionStoreConsolidatedParquet(CTAStore.Companion::from, "analytics_submission_store_consolidated_parquet_bucket_id"),
    AnalyticsSubmissionStoreParquet(CTAStore.Companion::from, "analytics_submission_store_parquet_bucket"),
    CircuitBreakerStats(CTAStore.Companion::from, "circuit_breaker_stats_bucket_id"),
    DiagnosisKeySubmissionStats(CTAStore.Companion::from, "diagnosis_key_submission_stats_bucket_id"),
    KeyFederationDownloadStats(CTAStore.Companion::from, "key_federation_download_stats_bucket_id"),
    KeyFederationUploadStats(CTAStore.Companion::from, "key_federation_upload_stats_bucket_id"),
    RiskyPostCodes(CTAStore.Companion::from, "risky_post_codes_bucket_id"),
    SIPAnalytics(SIPStore.Companion::from, "sip_analytics_bucket_location");

    fun configFrom(config: JsonNode, name: String) = converter(Store(this, name))(config)
}
