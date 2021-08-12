package uk.nhs.nhsx.diagnosiskeyssubmission

import com.amazonaws.HttpMethod.GET
import com.amazonaws.HttpMethod.POST
import com.amazonaws.services.dynamodbv2.document.Item
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.natpryce.snodge.json.defaultJsonMutagens
import com.natpryce.snodge.json.forStrings
import com.natpryce.snodge.mutants
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import org.http4k.core.Status.Companion.METHOD_NOT_ALLOWED
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import strikt.api.expect
import strikt.api.expectThat
import strikt.assertions.hasEntry
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.Environment.Access.Companion.TEST
import uk.nhs.nhsx.core.auth.ResponseSigner
import uk.nhs.nhsx.core.aws.dynamodb.AwsDynamoClient
import uk.nhs.nhsx.core.aws.dynamodb.TableName
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.core.events.DiagnosisKeySubmission
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.core.events.UnprocessableJson
import uk.nhs.nhsx.domain.TestKit
import uk.nhs.nhsx.testhelper.ContextBuilder.Companion.aContext
import uk.nhs.nhsx.testhelper.ProxyRequestBuilder.request
import uk.nhs.nhsx.testhelper.assertions.AwsRuntimeAssertions.ProxyResponse.body
import uk.nhs.nhsx.testhelper.assertions.AwsRuntimeAssertions.ProxyResponse.headers
import uk.nhs.nhsx.testhelper.assertions.AwsRuntimeAssertions.ProxyResponse.status
import uk.nhs.nhsx.testhelper.assertions.S3ObjectAssertions.asString
import uk.nhs.nhsx.testhelper.assertions.S3ObjectAssertions.content
import uk.nhs.nhsx.testhelper.assertions.contains
import uk.nhs.nhsx.testhelper.assertions.isSameAs
import uk.nhs.nhsx.testhelper.data.TestData.STORED_KEYS_PAYLOAD_DAYS_SINCE_ONSET_SUBMISSION
import uk.nhs.nhsx.testhelper.data.TestData.STORED_KEYS_PAYLOAD_SUBMISSION
import uk.nhs.nhsx.testhelper.data.TestData.STORED_KEYS_PAYLOAD_WITH_RISK_LEVEL
import uk.nhs.nhsx.testhelper.mocks.FakeS3
import uk.nhs.nhsx.testhelper.mocks.getBucket
import uk.nhs.nhsx.testhelper.mocks.getObject
import uk.nhs.nhsx.testhelper.mocks.isEmpty
import uk.nhs.nhsx.testhelper.withBearerToken
import uk.nhs.nhsx.testhelper.withCustomOai
import uk.nhs.nhsx.testhelper.withJson
import uk.nhs.nhsx.testhelper.withMethod
import uk.nhs.nhsx.testhelper.withRequestId
import java.time.Instant
import java.util.*
import kotlin.random.Random

class DiagnosisKeySubmissionHandlerTest {

    private val submissionDiagnosisKeysPath = "/submission/diagnosis-keys"

    private val clock = { Instant.ofEpochSecond((2667023 * 600).toLong()) } // 2020-09-15 23:50:00 UTC

    private val bucketName = BucketName.of(UUID.randomUUID().toString())
    private val tableName = TableName.of(UUID.randomUUID().toString())
    private val environmentSettings = mapOf(
        "submission_tokens_table" to tableName.value,
        "SUBMISSION_STORE" to bucketName.value,
        "MAINTENANCE_MODE" to "FALSE",
        "custom_oai" to "OAI"
    )

    private val events = RecordingEvents()
    private val environment = Environment.fromName("test", TEST.apply(environmentSettings))
    private val s3Storage = FakeS3()
    private val awsDynamoClient = mockk<AwsDynamoClient>()
    private val objectKeyNameProvider = { ObjectKey.of("my-object-key") }
    private val signer = ResponseSigner { _, r -> r.headers["signed"] = "yup" }

    private val handler = DiagnosisKeySubmissionHandler(
        environment = environment,
        clock = clock,
        events = events,
        mobileAuthenticator = { true },
        healthAuthenticator = { true },
        signer = signer,
        s3Storage = s3Storage,
        awsDynamoClient = awsDynamoClient,
        objectKeyNameProvider = objectKeyNameProvider
    )

    @Test
    fun `accepts payload and returns 200`() {
        awsDynamoClient.willDeleteAny().willReturnVirologyRecord()

        val responseEvent = callHandlerWith(
            """
                {
                  "diagnosisKeySubmissionToken": "dd3aa1bf-4c91-43bb-afb6-12d0b5dcad43",
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
        )

        verifyScenario(responseEvent, STORED_KEYS_PAYLOAD_SUBMISSION)
    }

    @Test
    fun `accepts payload with days since onset`() {
        awsDynamoClient.willDeleteAny().willReturnVirologyRecord()

        val responseEvent = callHandlerWith(
            """
                {
                  "diagnosisKeySubmissionToken": "dd3aa1bf-4c91-43bb-afb6-12d0b5dcad43",
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
        )

        verifyScenario(responseEvent, STORED_KEYS_PAYLOAD_DAYS_SINCE_ONSET_SUBMISSION)
    }

    @Test
    fun `accepts payload with riskLevel and returns 200`() {
        awsDynamoClient.willDeleteAny().willReturnVirologyRecord()

        val responseEvent = callHandlerWith(
            """
                {
                  "diagnosisKeySubmissionToken": "dd3aa1bf-4c91-43bb-afb6-12d0b5dcad43",
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
        )

        verifyScenario(responseEvent, STORED_KEYS_PAYLOAD_WITH_RISK_LEVEL)
    }

    @Test
    fun `not found when path is wrong`() {
        val requestEvent = request()
            .withMethod(POST)
            .withPath("dodgy")
            .withCustomOai("OAI")
            .withRequestId()
            .withBearerToken("anything")
            .withJson(
                """
                    {
                      "diagnosisKeySubmissionToken": "dd3aa1bf-4c91-43bb-afb6-12d0b5dcad43",
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
            )

        val responseEvent = handler.handleRequest(requestEvent, aContext())

        expectThat(responseEvent) {
            status.isSameAs(NOT_FOUND)
            body.isNull()
        }

        expectThat(s3Storage).isEmpty()
    }

    @Test
    fun `method not allowed when method is wrong`() {
        val requestEvent = request()
            .withMethod(GET)
            .withPath(submissionDiagnosisKeysPath)
            .withCustomOai("OAI")
            .withRequestId()
            .withBearerToken("anything")
            .withJson(
                """
                    {
                      "diagnosisKeySubmissionToken": "dd3aa1bf-4c91-43bb-afb6-12d0b5dcad43",
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
            )

        val responseEvent = handler.handleRequest(requestEvent, aContext())

        expectThat(responseEvent) {
            status.isSameAs(METHOD_NOT_ALLOWED)
            body.isNull()
        }

        expectThat(s3Storage).isEmpty()
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "{", "{}"])
    fun `ok when empty body preventing abuse`(payload: String) {
        val responseEvent = callHandlerWith(payload)

        expectThat(responseEvent) {
            status.isSameAs(OK)
            body.isNull()
        }

        expectThat(s3Storage).isEmpty()
        expectThat(events).contains(DiagnosisKeySubmission::class, UnprocessableJson::class)
    }

    @Test
    fun `handles random values`() {
        val originalJson = """
            {
              "diagnosisKeySubmissionToken": "dd3aa1bf-4c91-43bb-afb6-12d0b5dcad43",
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

        awsDynamoClient.willReturnNull()

        Random.mutants(
            defaultJsonMutagens().forStrings(),
            100,
            originalJson
        ).forEach { mutant ->
            when {
                mutant != originalJson -> expectThat(callHandlerWith(mutant)) {
                    status.isSameAs(OK)
                    body.isNull()
                }
            }
        }
    }

    @Test
    fun `handles random values with risk level`() {
        val originalJson = """
            {
              "diagnosisKeySubmissionToken": "dd3aa1bf-4c91-43bb-afb6-12d0b5dcad43",
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

        awsDynamoClient.willReturnNull()

        Random.mutants(
            defaultJsonMutagens().forStrings(),
            100,
            originalJson
        ).forEach { mutant ->
            when {
                mutant != originalJson -> expectThat(callHandlerWith(mutant)) {
                    status.isSameAs(OK)
                    body.isNull()
                }
            }
        }
    }

    private fun callHandlerWith(requestPayload: String): APIGatewayProxyResponseEvent {
        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath(submissionDiagnosisKeysPath)
            .withBearerToken("anything")
            .withBody(requestPayload)

        return handler.handleRequest(requestEvent, aContext())
    }

    private fun AwsDynamoClient.willReturnVirologyRecord(testKit: TestKit? = null) = apply {
        val hashKeyValue = slot<String>()

        every {
            getItem(tableName, "diagnosisKeySubmissionToken", capture(hashKeyValue))
        } answers {
            val id = UUID.fromString(hashKeyValue.captured)
            when (testKit) {
                null -> Item.fromJSON("""{"diagnosisKeySubmissionToken": "$id"}""")
                else -> Item.fromJSON("""{"diagnosisKeySubmissionToken": "$id", "testKit": "$testKit"}""")
            }
        }
    }

    private fun AwsDynamoClient.willReturnNull() = apply {
        every { getItem(any(), any(), any()) } returns null
    }

    private fun AwsDynamoClient.willDeleteAny() = apply {
        every { deleteItem(any(), any(), any()) } just runs
    }

    private fun verifyScenario(
        responseEvent: APIGatewayProxyResponseEvent,
        expected: String
    ) {
        expect {
            that(responseEvent) {
                status.isSameAs(OK)
                body.isNull()
                headers.hasEntry("signed", "yup")
            }

            that(s3Storage)
                .getBucket(bucketName)
                .getObject("mobile/LAB_RESULT/my-object-key.json")
                .content
                .asString()
                .isEqualTo(expected)

            that(events).contains(
                DiagnosisKeySubmission::class,
                DownloadedTemporaryExposureKeys::class
            )
        }
    }
}
