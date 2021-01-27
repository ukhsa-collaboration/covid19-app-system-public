package uk.nhs.nhsx.diagnosiskeyssubmission

import com.amazonaws.HttpMethod
import com.amazonaws.services.dynamodbv2.document.Item
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.natpryce.snodge.json.defaultJsonMutagens
import com.natpryce.snodge.json.forStrings
import com.natpryce.snodge.mutants
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.aws.dynamodb.AwsDynamoClient
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.core.aws.s3.ObjectKeyNameProvider
import uk.nhs.nhsx.core.exceptions.HttpStatusCode
import uk.nhs.nhsx.testhelper.ContextBuilder
import uk.nhs.nhsx.testhelper.ProxyRequestBuilder
import uk.nhs.nhsx.testhelper.data.TestData
import uk.nhs.nhsx.testhelper.data.TestData.STORED_KEYS_PAYLOAD_DAYS_SINCE_ONSET_SUBMISSION
import uk.nhs.nhsx.testhelper.matchers.ProxyResponseAssertions.hasBody
import uk.nhs.nhsx.testhelper.matchers.ProxyResponseAssertions.hasHeader
import uk.nhs.nhsx.testhelper.matchers.ProxyResponseAssertions.hasStatus
import uk.nhs.nhsx.testhelper.mocks.FakeS3Storage
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.function.Consumer
import java.util.function.Supplier
import kotlin.random.Random

class DiagnosisKeysSubmissionHandlerTest {
    private val uuid = "dd3aa1bf-4c91-43bb-afb6-12d0b5dcad43"
    private val payloadJson = "{" +
        "  \"diagnosisKeySubmissionToken\": \"" + uuid + "\"," +
        "  \"temporaryExposureKeys\": [" +
        "    {" +
        "      \"key\": \"W2zb3BeMWt6Xr2u0ABG32Q==\"," +
        "      \"rollingStartNumber\": 2666736," +
        "      \"rollingPeriod\": 144" +
        "    }," +
        "    {" +
        "      \"key\": \"kzQt9Lf3xjtAlMtm7jkSqw==\"," +
        "      \"rollingStartNumber\": 2664864," +
        "      \"rollingPeriod\": 144" +
        "    }" +
        "  ]" +
        "}"
    private val payloadJsonWithDaysSinceOnset = "{" +
        "  \"diagnosisKeySubmissionToken\": \"" + uuid + "\"," +
        "  \"temporaryExposureKeys\": [" +
        "    {" +
        "      \"key\": \"W2zb3BeMWt6Xr2u0ABG32Q==\"," +
        "      \"rollingStartNumber\": 2666736," +
        "      \"rollingPeriod\": 144," +
        "      \"daysSinceOnsetOfSymptoms\": 1" +
        "    }," +
        "    {" +
        "      \"key\": \"kzQt9Lf3xjtAlMtm7jkSqw==\"," +
        "      \"rollingStartNumber\": 2664864," +
        "      \"rollingPeriod\": 144," +
        "      \"daysSinceOnsetOfSymptoms\": 4" +
        "    }" +
        "  ]" +
        "}"
    private val payloadJsonWithRiskLevel = "{" +
        "  \"diagnosisKeySubmissionToken\": \"" + uuid + "\"," +
        "  \"temporaryExposureKeys\": [" +
        "    {" +
        "        \"key\": \"W2zb3BeMWt6Xr2u0ABG32Q==\"," +
        "        \"rollingStartNumber\": 2666736, " +
        "        \"rollingPeriod\": 144," +
        "        \"transmissionRiskLevel\": 5" +
        "    }," +
        "    {" +
        "        \"key\": \"kzQt9Lf3xjtAlMtm7jkSqw==\"," +
        "        \"rollingStartNumber\": 2664864, " +
        "        \"rollingPeriod\": 144," +
        "        \"transmissionRiskLevel\": 4" +
        "    }" +
        "   ]" +
        "}"
    private val environmentSettings = mapOf(
        "submission_tokens_table" to "stt",
        "SUBMISSION_STORE" to "store",
        "MAINTENANCE_MODE" to "FALSE"
    )
    private val environment = Environment.fromName("test", Environment.Access.TEST.apply(environmentSettings))
    private val s3Storage = FakeS3Storage()
    private val awsDynamoClient = Mockito.mock(AwsDynamoClient::class.java)
    private val objectKeyNameProvider = Mockito.mock(ObjectKeyNameProvider::class.java)
    private val clock = Supplier { Instant.ofEpochSecond((2667023 * 600).toLong()) } // 2020-09-15 23:50:00 UTC
    private val handler = DiagnosisKeySubmissionHandler(
        environment,
        { true },
        { _: APIGatewayProxyRequestEvent?, resp: APIGatewayProxyResponseEvent -> resp.headers["signed"] = "yup" },
        s3Storage,
        awsDynamoClient,
        objectKeyNameProvider,
        clock
    )

    @Test
    fun acceptsPayloadAndReturns200() {
        val objectKey = ObjectKey.of("some-object-key")
        val hashKey = "diagnosisKeySubmissionToken"
        Mockito.`when`(objectKeyNameProvider.generateObjectKeyName()).thenReturn(objectKey)
        Mockito.`when`(awsDynamoClient.getItem("stt", hashKey, uuid))
            .thenReturn(Item.fromJSON("{\"$hashKey\": \"$uuid\"}"))
        val responseEvent = responseFor(payloadJson)
        assertThat(responseEvent, hasStatus(HttpStatusCode.OK_200))
        assertThat(responseEvent, hasBody(equalTo(null)))
        assertThat(responseEvent, hasHeader("signed", equalTo("yup")))
        assertThat(s3Storage.count, equalTo(1))
        assertThat(s3Storage.name, equalTo(ObjectKey.of("mobile/LAB_RESULT/${objectKey.value}.json")))
        assertThat(s3Storage.bucket.value, equalTo("store"))
        assertThat(String(s3Storage.bytes.read(), StandardCharsets.UTF_8), equalTo(TestData.STORED_KEYS_PAYLOAD_SUBMISSION))
    }

    @Test
    fun acceptsPayloadWithDaysSinceOnset() {
        val objectKey = ObjectKey.of("some-object-key")
        val hashKey = "diagnosisKeySubmissionToken"
        Mockito.`when`(objectKeyNameProvider.generateObjectKeyName()).thenReturn(objectKey)
        Mockito.`when`(awsDynamoClient.getItem("stt", hashKey, uuid))
            .thenReturn(Item.fromJSON("{\"$hashKey\": \"$uuid\"}"))
        val responseEvent = responseFor(payloadJsonWithDaysSinceOnset)
        assertThat(responseEvent, hasStatus(HttpStatusCode.OK_200))
        assertThat(responseEvent, hasBody(equalTo(null)))
        assertThat(responseEvent, hasHeader("signed", equalTo("yup")))
        assertThat(s3Storage.count, equalTo(1))
        assertThat(s3Storage.name, equalTo(ObjectKey.of("mobile/LAB_RESULT/${objectKey.value}.json")))
        assertThat(s3Storage.bucket.value, equalTo("store"))
        assertThat(String(s3Storage.bytes.read(), StandardCharsets.UTF_8), equalTo(STORED_KEYS_PAYLOAD_DAYS_SINCE_ONSET_SUBMISSION))
    }

    @Test
    fun acceptsPayloadWithRiskLevelAndReturns200() {
        val objectKey = ObjectKey.of("some-object-key")
        val hashKey = "diagnosisKeySubmissionToken"
        Mockito.`when`(objectKeyNameProvider.generateObjectKeyName()).thenReturn(objectKey)
        Mockito.`when`(awsDynamoClient.getItem("stt", hashKey, uuid))
            .thenReturn(Item.fromJSON("{\"$hashKey\": \"$uuid\"}"))
        val responseEvent = responseFor(payloadJsonWithRiskLevel)
        assertThat(responseEvent, hasStatus(HttpStatusCode.OK_200))
        assertThat(responseEvent, hasBody(equalTo(null)))
        assertThat(responseEvent, hasHeader("signed", equalTo("yup")))
        assertThat(s3Storage.count, equalTo(1))
        assertThat(s3Storage.name, equalTo(ObjectKey.of("mobile/LAB_RESULT/${objectKey.value}.json")))
        assertThat(s3Storage.bucket.value, equalTo("store"))
        assertThat(String(s3Storage.bytes.read(), StandardCharsets.UTF_8), equalTo(TestData.STORED_KEYS_PAYLOAD_WITH_RISK_LEVEL))
    }

    @Test
    fun notFoundWhenPathIsWrong() {
        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath("dodgy")
            .withBearerToken("anything")
            .withJson(payloadJson)
            .build()
        val responseEvent = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(responseEvent, hasStatus(HttpStatusCode.NOT_FOUND_404))
        assertThat(responseEvent, hasBody(equalTo(null)))
        verifyNoMockInteractions()
    }

    @Test
    fun methodNotAllowedWhenMethodIsWrong() {
        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.GET)
            .withPath(SUBMISSION_DIAGNOSIS_KEYS_PATH)
            .withBearerToken("anything")
            .withJson(payloadJson)
            .build()
        val responseEvent = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(responseEvent, hasStatus(HttpStatusCode.METHOD_NOT_ALLOWED_405))
        assertThat(responseEvent, hasBody(equalTo(null)))
        verifyNoMockInteractions()
    }

    @Test
    fun okWhenEmptyBodyPreventingAbuse() {
        val responseEvent = responseFor("")
        assertThat(responseEvent, hasStatus(HttpStatusCode.OK_200))
        assertThat(responseEvent, hasBody(equalTo(null)))
        verifyNoMockInteractions()
    }

    @Test
    fun okWhenInvalidJsonPreventingAbuse() {
        val responseEvent = responseFor("{")
        assertThat(responseEvent, hasStatus(HttpStatusCode.OK_200))
        assertThat(responseEvent, hasBody(equalTo(null)))
        verifyNoMockInteractions()
    }

    @Test
    fun okWhenEmptyJsonPreventingAbuse() {
        val responseEvent = responseFor("{}")
        assertThat(responseEvent, hasStatus(HttpStatusCode.OK_200))
        assertThat(responseEvent, hasBody(equalTo(null)))
        verifyNoMockInteractions()
    }

    @Test
    fun handlesRandomValues() {
        val originalJson = payloadJson
        Random.mutants(defaultJsonMutagens().forStrings(), 100, originalJson)
            .forEach(Consumer { json: String ->
                if (json != originalJson) {
                    val response = responseFor(json)
                    assertThat(response, hasStatus(HttpStatusCode.OK_200))
                    assertThat(response, hasBody(equalTo(null)))
                }
            })
    }

    @Test
    fun handlesRandomValuesWithRiskLevel() {
        val originalJson = payloadJsonWithRiskLevel
        Random.mutants(defaultJsonMutagens().forStrings(), 100, originalJson)
            .forEach(Consumer { json: String ->
                if (json != originalJson) {
                    val response = responseFor(json)
                    assertThat(response, hasStatus(HttpStatusCode.OK_200))
                    assertThat(response, hasBody(equalTo(null)))
                }
            })
    }

    private fun responseFor(requestPayload: String): APIGatewayProxyResponseEvent {
        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath(SUBMISSION_DIAGNOSIS_KEYS_PATH)
            .withBearerToken("anything")
            .withBody(requestPayload)
            .build()
        return handler.handleRequest(requestEvent, ContextBuilder.aContext())
    }

    private fun verifyNoMockInteractions() {
        assertThat(s3Storage.count, equalTo(0))
        Mockito.verifyNoInteractions(awsDynamoClient)
        Mockito.verifyNoInteractions(objectKeyNameProvider)
    }

    companion object {
        private const val SUBMISSION_DIAGNOSIS_KEYS_PATH = "/submission/diagnosis-keys"
    }
}
