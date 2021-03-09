package uk.nhs.nhsx.keyfederation.upload

import com.amazonaws.services.lambda.runtime.events.ScheduledEvent
import com.amazonaws.services.s3.model.S3ObjectSummary
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.type.TypeReference
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.exactly
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.matching.MatchResult
import com.github.tomakehurst.wiremock.matching.MatchResult.exactMatch
import com.github.tomakehurst.wiremock.matching.MatchResult.noMatch
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import org.jose4j.jws.JsonWebSignature
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.nhs.nhsx.core.Jackson
import uk.nhs.nhsx.core.Jackson.readOrNull
import uk.nhs.nhsx.core.SystemClock
import uk.nhs.nhsx.core.SystemObjectMapper
import uk.nhs.nhsx.core.TestEnvironments
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.secretsmanager.SecretName
import uk.nhs.nhsx.core.aws.ssm.ParameterName
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.keyfederation.InMemoryBatchTagService
import uk.nhs.nhsx.keyfederation.InteropClient
import uk.nhs.nhsx.keyfederation.TestKeyPairs.ecPrime256r1
import uk.nhs.nhsx.testhelper.mocks.FakeDiagnosisKeysS3
import uk.nhs.nhsx.testhelper.proxy
import uk.nhs.nhsx.testhelper.wiremock.WireMockExtension
import java.time.Instant
import java.util.Date

@ExtendWith(WireMockExtension::class)
class KeyFederationUploadHandlerTest(private val wireMock: WireMockServer) {

    private val events = RecordingEvents()
    private val now = Instant.parse("2020-02-05T10:00:00.000Z")

    @Test
    fun `enable upload should call interop`() {
        wireMock.stubFor(
            post("/diagnosiskeys/upload")
                .withHeader("Authorization", equalTo("Bearer DUMMY_TOKEN"))
                .withRequestBody(JwsUploadContentPattern(expectedKeys = listOf("foo")))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withBody(
                            """
                            {
                                "batchTag": "75b326f7-ae6f-42f6-9354-00c0a6b797b3",
                                "insertedExposures":0
                            }
                            """.trimIndent()
                        )
                )
        )

        KeyFederationUploadHandler(
            TestEnvironments.environmentWith(),
            SystemClock.CLOCK,
            RecordingEvents(),
            BucketName.of("SUBMISSION_BUCKET"),
            KeyFederationUploadConfig(
                100,
                14,
                0,
                { true },
                false,
                -1,
                wireMock.baseUrl(),
                SecretName.of("authToken"),
                ParameterName.of("parameter"),
                "DUMMY_TABLE",
                "GB-EAW",
                emptyList()
            ),
            batchTagService = InMemoryBatchTagService(),
            interopClient = InteropClient(
                wireMock.baseUrl(),
                "DUMMY_TOKEN",
                JWS(KmsCompatibleSigner(ecPrime256r1.private)),
                events
            ),
            awsS3Client = FakeDiagnosisKeysS3(listOf(
                S3ObjectSummary().apply {
                    key = "foo"
                    lastModified = Date.from(now)
                }
            ))
        ).handleRequest(ScheduledEvent(), proxy())
    }

    @Test
    fun `disable upload should not call interop`() {
        wireMock.stubFor(
            post("/diagnosiskeys/upload")
                .withHeader("Authorization", equalTo("Bearer DUMMY_TOKEN"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                )
        )

        KeyFederationUploadHandler(
            TestEnvironments.environmentWith(),
            SystemClock.CLOCK,
            RecordingEvents(),
            BucketName.of("SUBMISSION_BUCKET"),
            KeyFederationUploadConfig(
                100,
                14,
                0,
                { false },
                false,
                -1,
                wireMock.baseUrl(),
                SecretName.of("authToken"),
                ParameterName.of("parameter"),
                "DUMMY_TABLE",
                "GB-EAW",
                emptyList()
            ),
            batchTagService = InMemoryBatchTagService(),
            interopClient = InteropClient(
                wireMock.baseUrl(),
                "DUMMY_TOKEN",
                JWS(KmsCompatibleSigner(ecPrime256r1.private)),
                events
            ),
            awsS3Client = FakeDiagnosisKeysS3(emptyList()),
        ).handleRequest(ScheduledEvent(), proxy())

        wireMock.verify(
            exactly(0),
            postRequestedFor(urlEqualTo("/diagnosiskeys/upload"))
                .withHeader("Authorization", equalTo("Bearer DUMMY_TOKEN"))
        )
    }

    @Test
    fun `upload keys should only include mobile lab results, root mobile, and whitelisted federated keys`() {
        wireMock.stubFor(
            post("/diagnosiskeys/upload")
                .withHeader("Authorization", equalTo("Bearer DUMMY_TOKEN"))
                .withRequestBody(
                    JwsUploadContentPattern(
                        expectedKeys = listOf(
                            "foo", "bar", "foobar", "nearform/GB-EAW/bar", "mobile/LAB_RESULT/foobar"
                        )
                    )
                )
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withBody(
                            """
                            {
                                "batchTag": "75b326f7-ae6f-42f6-9354-00c0a6b797b3",
                                "insertedExposures":0
                            }
                            """.trimIndent()
                        )
                )
        )

        KeyFederationUploadHandler(
            TestEnvironments.environmentWith(),
            SystemClock.CLOCK,
            RecordingEvents(),
            BucketName.of("SUBMISSION_BUCKET"),
            KeyFederationUploadConfig(
                100,
                14,
                0,
                { true },
                false,
                -1,
                wireMock.baseUrl(),
                SecretName.of("authToken"),
                ParameterName.of("parameter"),
                "DUMMY_TABLE",
                "GB-EAW",
                listOf("nearform/GB-EAW", "nearform/NI", "nearform/JE"),
            ),
            batchTagService = InMemoryBatchTagService(),
            interopClient = InteropClient(
                wireMock.baseUrl(),
                "DUMMY_TOKEN",
                JWS(KmsCompatibleSigner(ecPrime256r1.private)),
                events
            ),
            awsS3Client = FakeDiagnosisKeysS3(listOf(
                S3ObjectSummary().apply {
                    key = "foo"
                    lastModified = Date.from(now)
                },
                S3ObjectSummary().apply {
                    key = "bar"
                    lastModified = Date.from(now)
                },
                S3ObjectSummary().apply {
                    key = "foobar"
                    lastModified = Date.from(now)
                },
                S3ObjectSummary().apply {
                    key = "federatedKeyPrefix/foo"
                    lastModified = Date.from(now)
                },
                S3ObjectSummary().apply {
                    key = "nearform/GB-EAW/bar"
                    lastModified = Date.from(now)
                },
                S3ObjectSummary().apply {
                    key = "mobile/LAB_RESULT/foobar"
                    lastModified = Date.from(now)
                },
                S3ObjectSummary().apply {
                    key = "mobile/RAPID_RESULT/foobar"
                    lastModified = Date.from(now)
                }
            ))
        ).handleRequest(ScheduledEvent(), proxy())
    }

    class JwsUploadContentPattern(
        @JsonProperty matchesPayloadPattern: String = "",
        private val expectedKeys: List<String>
    ) : StringValuePattern(matchesPayloadPattern) {

        override fun match(value: String): MatchResult = readOrNull<DiagnosisKeysUploadRequest>(value)
            ?.let {
                val jws = JsonWebSignature().apply {
                    key = ecPrime256r1.public
                    compactSerialization = it.payload
                }

                val exposures = SystemObjectMapper.MAPPER.readValue(
                    jws.payload,
                    object : TypeReference<List<ExposureUpload>>() {})

                if (exposures.map { it.keyData }.toSet() == expectedKeys.toSet()) exactMatch() else noMatch()
            } ?: noMatch()

        override fun getExpected(): String =
            "Cannot display expected response due to custom matching, expecting $expectedKeys keys in payload"
    }
}


