package uk.nhs.nhsx.sanity.lambdas.config

import com.fasterxml.jackson.databind.JsonNode
import kotlin.reflect.KFunction1

data class Endpoint(val lambda: DeployedLambda, val name: String)

/**
 * Names of all of the lambdas in the system
 */
enum class DeployedLambda(
    val converter: KFunction1<Endpoint, (JsonNode) -> EndpointConfig>,
    val baseEndpoint: String,
    val endpointBearerTokenJsonName: String? = null,
    endpointSuffixes: List<String> = listOf(),
    val healthEndpoint: String = "${baseEndpoint}_health_endpoint"
) {
    AnalyticsEventsSubmission(Submission.Companion::from, "analytics_events_submission", "mobile"),
    AnalyticsSubmission(Submission.Companion::from, "analytics_submission", "mobile"),
    AvailabilityAndroidDistribution(Distribution.Companion::from, "availability_android_distribution"),
    AvailabilityIosDistribution(Distribution.Companion::from, "availability_ios_distribution"),

    DiagnosisKeysDistribution(
        Distribution.Companion::from,
        "diagnosis_keys_distribution",
        endpointSuffixes = listOf("_2hourly", "_daily")
    ),
    DiagnosisKeysSubmission(
        Submission.Companion::from,
        "diagnosis_keys_submission",
        "mobile"
    ),

    ExposureNotificationDistribution(Distribution.Companion::from, "exposure_configuration_distribution"),
    ExposureNotificationCircuitBreaker(CircuitBreaker.Companion::from, "exposure_notification_circuit_breaker", "mobile"),
    IsolationPayment(Submission.Companion::from, "isolation_payment", "mobile", listOf("_create", "_update")),
    PostDistrictsDistribution(Distribution.Companion::from, "post_districts_distribution"),
    RiskyPostcodeDistrictsUpload(Upload.Companion::from, "risky_post_districts_upload", "highRiskPostCodeUpload"),

    RiskyVenuesCircuitBreaker(CircuitBreaker.Companion::from, "risky_venues_circuit_breaker", "mobile"),
    RiskyVenuesDistribution(Distribution.Companion::from, "risky_venues_distribution"),
    RiskyVenuesMessagesDownload(Download.Companion::from, "risky_venues_messages_download"), // is this the same thing as a distribution?
    RiskyVenuesUpload(Upload.Companion::from, "risky_venues_upload", "highRiskVenuesCodeUpload"),
    SelfIsolationDistribution(Distribution.Companion::from, "self_isolation_distribution"),
    SymptomaticQuestionnaireDistribution(Distribution.Companion::from, "symptomatic_questionnaire_distribution"),

    TestResultsUpload(
        Upload.Companion::from,
        "test_results",
        "testResultUpload",
        listOf("_eng_tokengen_upload", "_fiorano_upload", "_npex_upload", "_wls_tokengen_upload")
    ),
    VirologyKit(Submission.Companion::from, "virology_kit", "mobile");

    val endpointJsonNames = if (endpointSuffixes.isEmpty()) listOf(baseEndpoint) else endpointSuffixes.map { baseEndpoint + it }
    val storeJsonName: String = "${baseEndpoint}_store"
    val healthEndpointJsonNames = listOf(healthEndpoint)

    fun configFrom(config: JsonNode, name: String) = converter(Endpoint(this, name))(config)
}
