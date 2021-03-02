package uk.nhs.nhsx.diagnosiskeyssubmission

import com.amazonaws.HttpMethod
import com.amazonaws.services.dynamodbv2.document.DeleteItemOutcome
import com.amazonaws.services.dynamodbv2.document.Item
import com.amazonaws.services.dynamodbv2.model.DeleteItemResult
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.natpryce.snodge.json.defaultJsonMutagens
import com.natpryce.snodge.json.forStrings
import com.natpryce.snodge.mutants
import io.mockk.every
import io.mockk.mockk
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.aws.dynamodb.AwsDynamoClient
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.core.aws.s3.ObjectKeyNameProvider
import uk.nhs.nhsx.core.events.DiagnosisKeySubmission
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.core.events.UnprocessableJson
import uk.nhs.nhsx.core.exceptions.HttpStatusCode
import uk.nhs.nhsx.testhelper.ContextBuilder
import uk.nhs.nhsx.testhelper.ProxyRequestBuilder
import uk.nhs.nhsx.testhelper.data.TestData
import uk.nhs.nhsx.testhelper.data.TestData.STORED_KEYS_PAYLOAD_DAYS_SINCE_ONSET_SUBMISSION
import uk.nhs.nhsx.testhelper.matchers.ProxyResponseAssertions.hasBody
import uk.nhs.nhsx.testhelper.matchers.ProxyResponseAssertions.hasHeader
import uk.nhs.nhsx.testhelper.matchers.ProxyResponseAssertions.hasStatus
import uk.nhs.nhsx.testhelper.mocks.FakeS3Storage
import java.time.Instant
import java.util.function.Consumer
import java.util.function.Supplier
import kotlin.random.Random

class DiagnosisKeySubmissionHandlerTest {
    private val uuid = "dd3aa1bf-4c91-43bb-afb6-12d0b5dcad43"
    private val submissionDiagnosisKeysPath = "/submission/diagnosis-keys"

    private val payloadJson = """
        {
          "diagnosisKeySubmissionToken": "$uuid",
          "temporaryExposureKeys": [
            {
              "key": "W2zb3BeMWt6Xr2u0ABG32Q==",
              "rollingStartNumber": 2666736,
              "rollingPeriod": 144
            },
            {
              "key": "kzQt9Lf3xjtAlMtm7jkSqw==",
              "rollingStartNumber": 2664864,
              "rollingPeriod": 144
            }
          ]
        }
        """.trimIndent()

    private val payloadJsonWithDaysSinceOnset = """
        {
          "diagnosisKeySubmissionToken": "$uuid",
          "temporaryExposureKeys": [
            {
              "key": "W2zb3BeMWt6Xr2u0ABG32Q==",
              "rollingStartNumber": 2666736,
              "rollingPeriod": 144,
              "daysSinceOnsetOfSymptoms": 1
            },
            {
              "key": "kzQt9Lf3xjtAlMtm7jkSqw==",
              "rollingStartNumber": 2664864,
              "rollingPeriod": 144,
              "daysSinceOnsetOfSymptoms": 4
            }
          ]
        }
        """.trimIndent()

    private val payloadJsonWithRiskLevel = """
        {
          "diagnosisKeySubmissionToken": "$uuid",
          "temporaryExposureKeys": [
            {
              "key": "W2zb3BeMWt6Xr2u0ABG32Q==",
              "rollingStartNumber": 2666736,
              "rollingPeriod": 144,
              "transmissionRiskLevel": 5
            },
            {
              "key": "kzQt9Lf3xjtAlMtm7jkSqw==",
              "rollingStartNumber": 2664864,
              "rollingPeriod": 144,
              "transmissionRiskLevel": 4
            }
          ]
        }
        """.trimIndent()

    private val environmentSettings = mapOf(
        "submission_tokens_table" to "stt",
        "SUBMISSION_STORE" to "store",
        "MAINTENANCE_MODE" to "FALSE",
        "custom_oai" to "OAI"
    )

    private val environment = Environment.fromName("test", Environment.Access.TEST.apply(environmentSettings))
    private val s3Storage = FakeS3Storage()
    private val awsDynamoClient = mockk<AwsDynamoClient>().also {
        every { it.deleteItem(any(), any(), any()) } returns DeleteItemOutcome(DeleteItemResult())
    }
    private val objectKeyNameProvider = mockk<ObjectKeyNameProvider>()
    private val clock = Supplier { Instant.ofEpochSecond((2667023 * 600).toLong()) } // 2020-09-15 23:50:00 UTC
    private val events = RecordingEvents()
    private val handler = DiagnosisKeySubmissionHandler(
        environment,
        { true },
        { true },
        { _: APIGatewayProxyRequestEvent?, resp: APIGatewayProxyResponseEvent -> resp.headers["signed"] = "yup" },
        s3Storage,
        awsDynamoClient,
        objectKeyNameProvider,
        clock,
        events
    )

    @Test
    fun `accepts payload and returns 200`() {
        val objectKey = ObjectKey.of("some-object-key")
        val hashKey = "diagnosisKeySubmissionToken"
        every { objectKeyNameProvider.generateObjectKeyName() } returns objectKey
        every { awsDynamoClient.getItem("stt", hashKey, uuid) } returns Item.fromJSON("{\"$hashKey\": \"$uuid\"}")
        val responseEvent = responseFor(payloadJson)
        assertThat(responseEvent, hasStatus(HttpStatusCode.OK_200))
        assertThat(responseEvent, hasBody(equalTo(null)))
        assertThat(responseEvent, hasHeader("signed", equalTo("yup")))
        assertThat(s3Storage.count, equalTo(1))
        assertThat(s3Storage.name, equalTo(ObjectKey.of("mobile/LAB_RESULT/${objectKey.value}.json")))
        assertThat(s3Storage.bucket.value, equalTo("store"))
        assertThat(s3Storage.bytes.toUtf8String(), equalTo(TestData.STORED_KEYS_PAYLOAD_SUBMISSION))
        events.containsExactly(DiagnosisKeySubmission::class, DownloadedTemporaryExposureKeys::class)
    }

    @Test
    fun `accepts payload with days since onset`() {
        val objectKey = ObjectKey.of("some-object-key")
        val hashKey = "diagnosisKeySubmissionToken"
        every { objectKeyNameProvider.generateObjectKeyName() } returns objectKey
        every { awsDynamoClient.getItem("stt", hashKey, uuid) } returns Item.fromJSON("{\"$hashKey\": \"$uuid\"}")

        val responseEvent = responseFor(payloadJsonWithDaysSinceOnset)
        assertThat(responseEvent, hasStatus(HttpStatusCode.OK_200))
        assertThat(responseEvent, hasBody(equalTo(null)))
        assertThat(responseEvent, hasHeader("signed", equalTo("yup")))
        assertThat(s3Storage.count, equalTo(1))
        assertThat(s3Storage.name, equalTo(ObjectKey.of("mobile/LAB_RESULT/${objectKey.value}.json")))
        assertThat(s3Storage.bucket.value, equalTo("store"))
        assertThat(s3Storage.bytes.toUtf8String(), equalTo(STORED_KEYS_PAYLOAD_DAYS_SINCE_ONSET_SUBMISSION))
        events.containsExactly(DiagnosisKeySubmission::class, DownloadedTemporaryExposureKeys::class)
    }

    @Test
    fun `accepts payload with riskLevel and returns 200`() {
        val objectKey = ObjectKey.of("some-object-key")
        val hashKey = "diagnosisKeySubmissionToken"
        every { objectKeyNameProvider.generateObjectKeyName() } returns objectKey
        every { awsDynamoClient.getItem("stt", hashKey, uuid) } returns Item.fromJSON("{\"$hashKey\": \"$uuid\"}")

        val responseEvent = responseFor(payloadJsonWithRiskLevel)
        assertThat(responseEvent, hasStatus(HttpStatusCode.OK_200))
        assertThat(responseEvent, hasBody(equalTo(null)))
        assertThat(responseEvent, hasHeader("signed", equalTo("yup")))
        assertThat(s3Storage.count, equalTo(1))
        assertThat(s3Storage.name, equalTo(ObjectKey.of("mobile/LAB_RESULT/${objectKey.value}.json")))
        assertThat(s3Storage.bucket.value, equalTo("store"))
        assertThat(s3Storage.bytes.toUtf8String(), equalTo(TestData.STORED_KEYS_PAYLOAD_WITH_RISK_LEVEL))
        events.containsExactly(DiagnosisKeySubmission::class, DownloadedTemporaryExposureKeys::class)
    }

    @Test
    fun `not found when path is wrong`() {
        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath("dodgy")
            .withCustomOai("OAI")
            .withRequestId()
            .withBearerToken("anything")
            .withJson(payloadJson)
            .build()
        val responseEvent = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(responseEvent, hasStatus(HttpStatusCode.NOT_FOUND_404))
        assertThat(responseEvent, hasBody(equalTo(null)))
        verifyNoMockInteractions()
    }

    @Test
    fun `method not allowed when method is wrong`() {
        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.GET)
            .withPath(submissionDiagnosisKeysPath)
            .withCustomOai("OAI")
            .withRequestId()
            .withBearerToken("anything")
            .withJson(payloadJson)
            .build()
        val responseEvent = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(responseEvent, hasStatus(HttpStatusCode.METHOD_NOT_ALLOWED_405))
        assertThat(responseEvent, hasBody(equalTo(null)))
        verifyNoMockInteractions()
    }

    @Test
    fun `ok when empty body preventing abuse`() {
        val responseEvent = responseFor("")
        assertThat(responseEvent, hasStatus(HttpStatusCode.OK_200))
        assertThat(responseEvent, hasBody(equalTo(null)))
        verifyNoMockInteractions()
        events.containsExactly(DiagnosisKeySubmission::class, UnprocessableJson::class)
    }

    @Test
    fun `ok when invalid Json preventing abuse`() {
        val responseEvent = responseFor("{")
        assertThat(responseEvent, hasStatus(HttpStatusCode.OK_200))
        assertThat(responseEvent, hasBody(equalTo(null)))
        verifyNoMockInteractions()
        events.containsExactly(DiagnosisKeySubmission::class, UnprocessableJson::class)
    }

    @Test
    fun `ok when empty Json preventing abuse`() {
        val responseEvent = responseFor("{}")
        assertThat(responseEvent, hasStatus(HttpStatusCode.OK_200))
        assertThat(responseEvent, hasBody(equalTo(null)))
        verifyNoMockInteractions()
        events.containsExactly(DiagnosisKeySubmission::class, UnprocessableJson::class)
    }

    @Test
    fun `handles random values`() {
        val originalJson = payloadJson
        Random.mutants(defaultJsonMutagens().forStrings(), 100, originalJson)
            .forEach(Consumer { json: String ->
                if (json != originalJson) {
                    every { awsDynamoClient.getItem(any(), any(), any()) } returns null
                    val response = responseFor(json)
                    assertThat(response, hasStatus(HttpStatusCode.OK_200))
                    assertThat(response, hasBody(equalTo(null)))
                }
            })
    }

    @Test
    fun `handles random values with risk level`() {
        val originalJson = payloadJsonWithRiskLevel
        Random.mutants(defaultJsonMutagens().forStrings(), 100, originalJson)
            .forEach(Consumer { json: String ->
                if (json != originalJson) {
                    every { awsDynamoClient.getItem(any(), any(), any()) } returns null
                    val response = responseFor(json)
                    assertThat(response, hasStatus(HttpStatusCode.OK_200))
                    assertThat(response, hasBody(equalTo(null)))
                }
            })
    }

    private fun responseFor(requestPayload: String): APIGatewayProxyResponseEvent {
        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath(submissionDiagnosisKeysPath)
            .withBearerToken("anything")
            .withBody(requestPayload)
            .build()
        return handler.handleRequest(requestEvent, ContextBuilder.aContext())
    }

    private fun verifyNoMockInteractions() = assertThat(s3Storage.count, equalTo(0))

}
