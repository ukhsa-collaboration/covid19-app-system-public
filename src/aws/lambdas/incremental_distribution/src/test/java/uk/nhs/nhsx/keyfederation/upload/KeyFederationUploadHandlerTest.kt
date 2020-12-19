package uk.nhs.nhsx.keyfederation.upload

import com.amazonaws.services.s3.model.S3ObjectSummary
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.type.TypeReference
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.MatchResult
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import org.jose4j.jws.JsonWebSignature
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.Jackson
import uk.nhs.nhsx.core.SystemObjectMapper
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.secretsmanager.SecretName
import uk.nhs.nhsx.core.aws.ssm.ParameterName
import uk.nhs.nhsx.testhelper.mocks.FakeDiagnosisKeysS3
import uk.nhs.nhsx.keyfederation.InMemoryBatchTagService
import uk.nhs.nhsx.keyfederation.InteropClient
import java.security.KeyPairGenerator
import java.security.spec.ECGenParameterSpec
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.Date

class KeyFederationUploadHandlerTest {

    companion object {
        val keyPair = {
            val keyPairGenerator = KeyPairGenerator.getInstance("EC")
            keyPairGenerator.initialize(ECGenParameterSpec("secp256r1"))
            keyPairGenerator.generateKeyPair()
        }()
    }

    private val wireMockRule: WireMockServer = WireMockServer(0)

    @BeforeEach
    fun start() = wireMockRule.start()

    @AfterEach
    fun stop() = wireMockRule.stop()

    @Test
    fun `enable upload should call interop`() {
        wireMockRule.stubFor(WireMock.post("/diagnosiskeys/upload")
            .willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withBody("""
                        {
                            "batchTag": "75b326f7-ae6f-42f6-9354-00c0a6b797b3",
                            "insertedExposures":0
                        }
                    """.trimIndent())
            ))

        KeyFederationUploadHandler(
            KeyFederationUploadConfig(
                100,
                14,
                0,
                { true },
                false,
                -1,
                BucketName.of("foo"),
                wireMockRule.baseUrl(),
                SecretName.of("authToken"),
                ParameterName.of("parameter"),
                "DUMMY_TABLE",
                "GB-EAW",
                emptyList()
            ),
            InMemoryBatchTagService(),
            { InteropClient(wireMockRule.baseUrl(), "DUMMY_TOKEN", JWS(KmsCompatibleSigner(keyPair.private))) },
            FakeDiagnosisKeysS3(listOf(
                S3ObjectSummary().apply {
                    key = "foo"
                    lastModified = Date.from(OffsetDateTime.now(ZoneOffset.UTC).toInstant())
                }
            ))
        ).handleRequest(null, null)

        wireMockRule.verify(
            WireMock.postRequestedFor(WireMock.urlEqualTo("/diagnosiskeys/upload"))
                .withHeader("Authorization", WireMock.equalTo("Bearer" + " DUMMY_TOKEN")) // on purpose
                .withRequestBody(JwsUploadContentPattern(expectedNumberOfKeys = 1))
        )
    }

    @Test
    fun `disable upload should not call interop`() {
        wireMockRule.stubFor(WireMock.post("/diagnosiskeys/upload")
            .willReturn(WireMock.aResponse().withStatus(200)))

        KeyFederationUploadHandler(
            KeyFederationUploadConfig(
                100,
                14,
                0,
                { false },
                false,
                -1,
                BucketName.of("foo"),
                wireMockRule.baseUrl(),
                SecretName.of("authToken"),
                ParameterName.of("parameter"),
                "DUMMY_TABLE",
                "GB-EAW",
                emptyList()
            ),
            InMemoryBatchTagService(),
            { InteropClient(wireMockRule.baseUrl(), "DUMMY_TOKEN", JWS(KmsCompatibleSigner(keyPair.private))) },
            FakeDiagnosisKeysS3(emptyList())
        ).handleRequest(null, null)

        wireMockRule.verify(0,
            WireMock.postRequestedFor(WireMock.urlEqualTo("/diagnosiskeys/upload"))
                .withHeader("Authorization", WireMock.equalTo("Bearer DUMMY_TOKEN"))
        )
    }

    @Test
    fun `upload keys should include whitelisted federated keys`() {
        wireMockRule.stubFor(WireMock.post("/diagnosiskeys/upload")
            .willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withBody("""
                        {
                            "batchTag": "75b326f7-ae6f-42f6-9354-00c0a6b797b3",
                            "insertedExposures":0
                        }
                    """.trimIndent())
            ))

        KeyFederationUploadHandler(
            KeyFederationUploadConfig(
                100,
                14,
                0,
                { true },
                false,
                -1,
                BucketName.of("foo"),
                wireMockRule.baseUrl(),
                SecretName.of("authToken"),
                ParameterName.of("parameter"),
                "DUMMY_TABLE",
                "GB-EAW",
                listOf("nearform/GB-EAW", "nearform/NI", "nearform/JE"),
            ),
            InMemoryBatchTagService(),
            { InteropClient(wireMockRule.baseUrl(), "DUMMY_TOKEN", JWS(KmsCompatibleSigner(keyPair.private))) },
            FakeDiagnosisKeysS3(listOf(
                S3ObjectSummary().apply {
                    key = "foo"
                    lastModified = Date.from(OffsetDateTime.now(ZoneOffset.UTC).toInstant())
                },
                S3ObjectSummary().apply {
                    key = "bar"
                    lastModified = Date.from(OffsetDateTime.now(ZoneOffset.UTC).toInstant())
                },
                S3ObjectSummary().apply {
                    key = "foobar"
                    lastModified = Date.from(OffsetDateTime.now(ZoneOffset.UTC).toInstant())
                },
                S3ObjectSummary().apply {
                    key = "federatedKeyPrefix/foo"
                    lastModified = Date.from(OffsetDateTime.now(ZoneOffset.UTC).toInstant())
                },
                S3ObjectSummary().apply {
                    key = "nearform/GB-EAW/bar"
                    lastModified = Date.from(OffsetDateTime.now(ZoneOffset.UTC).toInstant())
                }
            ))
        ).handleRequest(null, null)


        wireMockRule.verify(
            WireMock.postRequestedFor(WireMock.urlEqualTo("/diagnosiskeys/upload"))
                .withHeader("Authorization", WireMock.equalTo("Bearer" + " DUMMY_TOKEN")) // on purpose
                .withRequestBody(JwsUploadContentPattern(expectedNumberOfKeys = 4))
        )
    }

    class JwsUploadContentPattern(@JsonProperty matchesPayloadPattern: String = "",
                                  private val expectedNumberOfKeys: Int) : StringValuePattern(matchesPayloadPattern) {

        override fun match(value: String?): MatchResult {
            return Jackson.deserializeMaybe(value, DiagnosisKeysUploadRequest::class.java)
                .map {
                    val jws = JsonWebSignature().apply {
                        key = keyPair.public
                        compactSerialization = it.payload
                    }

                    val exposures = SystemObjectMapper.MAPPER.readValue(jws.payload, object : TypeReference<List<ExposureUpload>>() {})

                    if (exposures.size == expectedNumberOfKeys) {
                        MatchResult.exactMatch()
                    } else {
                        MatchResult.noMatch()
                    }
                }.orElse(
                    MatchResult.noMatch()
                )
        }

        override fun getExpected(): String {
            return "Cannot display expected response due to custom matching, expecting $expectedNumberOfKeys keys in payload"
        }
    }

}


