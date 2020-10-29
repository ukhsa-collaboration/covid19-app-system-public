package uk.nhs.nhsx.keyfederation

import com.amazonaws.services.s3.model.S3ObjectSummary
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.type.TypeReference
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit.WireMockRule
import com.github.tomakehurst.wiremock.matching.MatchResult
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import org.jose4j.jws.JsonWebSignature
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.core.Jackson
import uk.nhs.nhsx.core.SystemObjectMapper
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.secretsmanager.SecretName
import uk.nhs.nhsx.core.aws.xray.Tracing
import uk.nhs.nhsx.diagnosiskeydist.s3.FakeDiagnosisKeysS3
import uk.nhs.nhsx.keyfederation.upload.DiagnosisKeysUploadRequest
import uk.nhs.nhsx.keyfederation.upload.JWS
import java.security.KeyPairGenerator
import java.security.spec.ECGenParameterSpec
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

class HandlerTest {

    init {
        Tracing.disableXRayComplaintsForMainClasses()
    }

    companion object {
        val keyPair = {
            val keyPairGenerator = KeyPairGenerator.getInstance("EC")
            keyPairGenerator.initialize(ECGenParameterSpec("secp256r1"))
            keyPairGenerator.generateKeyPair()
        }()
    }


    @Rule
    @JvmField
    val wireMockRule = WireMockRule(WireMockConfiguration.wireMockConfig().dynamicPort())

    @Test
    fun `enable download and upload should call interop`() {
        wireMockRule.stubFor(WireMock.get("/diagnosiskeys/download/2020-08-01").willReturn(WireMock.aResponse()
            .withStatus(204)
        ))

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

        Handler(
            { LocalDate.of(2020, 8, 15).atStartOfDay().toInstant(ZoneOffset.UTC) },
            KeyFederationConfig(
                true,
                true,
                BucketName.of("foo"),
                wireMockRule.baseUrl(),
                SecretName.of("authToken"),
                SecretName.of("privateKey"),
                "federatedKeyPrefix",
                "DUMMY_TABLE",
                listOf("GB-EAW"),
                 "GB-EAW"
            ),
            InMemoryBatchTagService(),
            { InteropClient(wireMockRule.baseUrl(), "DUMMY_TOKEN", JWS(keyPair.private)) },
            FakeDiagnosisKeysS3(listOf(
                S3ObjectSummary().apply {
                    key = "foo"
                    lastModified = Date.from(OffsetDateTime.now(ZoneOffset.UTC).toInstant())
                }
            ))
        ).handleRequest(null, null)

        wireMockRule.verify(
            WireMock.getRequestedFor(WireMock.urlEqualTo("/diagnosiskeys/download/2020-08-01"))
                .withHeader("Authorization", WireMock.equalTo("Bearer" + " DUMMY_TOKEN")) // on purpose
        )

        wireMockRule.verify(
            WireMock.postRequestedFor(WireMock.urlEqualTo("/diagnosiskeys/upload"))
                .withHeader("Authorization", WireMock.equalTo("Bearer" + " DUMMY_TOKEN")) // on purpose
                .withRequestBody(JwsContentPattern(expectedNumberOfKeys = 1))
        )
    }

    @Test
    fun `disable download and upload should download not call interop`() {
        wireMockRule.stubFor(WireMock.get("/diagnosiskeys/download/2020-08-01").willReturn(WireMock.aResponse()
            .withStatus(204)
        ))

        wireMockRule.stubFor(WireMock.post("/diagnosiskeys/upload")
            .willReturn(WireMock.aResponse().withStatus(200)))

        Handler(
            { LocalDate.of(2020, 8, 15).atStartOfDay().toInstant(ZoneOffset.UTC) },
            KeyFederationConfig(
                false,
                false,
                BucketName.of("foo"),
                wireMockRule.baseUrl(),
                SecretName.of("authToken"),
                SecretName.of("privateKey"),
                "federatedKeyPrefix",
                "DUMMY_TABLE",
                listOf("GB-EAW"),
                "GB-EAW"
            ),
            InMemoryBatchTagService(),
            { InteropClient(wireMockRule.baseUrl(), "DUMMY_TOKEN", JWS(keyPair.private)) },
            FakeDiagnosisKeysS3(emptyList())
        ).handleRequest(null, null)

        wireMockRule.verify(0,
            WireMock.getRequestedFor(WireMock.urlEqualTo("/diagnosiskeys/download/2020-08-01"))
                .withHeader("Authorization", WireMock.equalTo("Bearer DUMMY_TOKEN"))
        )

        wireMockRule.verify(0,
            WireMock.postRequestedFor(WireMock.urlEqualTo("/diagnosiskeys/upload"))
                .withHeader("Authorization", WireMock.equalTo("Bearer DUMMY_TOKEN"))
        )
    }

    @Test
    fun `upload keys should filter out federated keys`() {
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

        Handler(
            { LocalDate.of(2020, 8, 15).atStartOfDay().toInstant(ZoneOffset.UTC) },
            KeyFederationConfig(
                false,
                true,
                BucketName.of("foo"),
                wireMockRule.baseUrl(),
                SecretName.of("authToken"),
                SecretName.of("privateKey"),
                "federatedKeyPrefix",
                "DUMMY_TABLE",
                listOf("GB-EAW"),
                "GB-EAW"
            ),
            InMemoryBatchTagService(),
            { InteropClient(wireMockRule.baseUrl(), "DUMMY_TOKEN", JWS(keyPair.private)) },
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
                    key = "federatedKeyPrefix-foo"
                    lastModified = Date.from(OffsetDateTime.now(ZoneOffset.UTC).toInstant())
                }
            ))
        ).handleRequest(null, null)


        wireMockRule.verify(
            WireMock.postRequestedFor(WireMock.urlEqualTo("/diagnosiskeys/upload"))
                .withHeader("Authorization", WireMock.equalTo("Bearer" + " DUMMY_TOKEN")) // on purpose
                .withRequestBody(JwsContentPattern(expectedNumberOfKeys = 3))
        )
    }

    class JwsContentPattern(@JsonProperty matchesPayloadPattern: String = "", private val expectedNumberOfKeys: Int) : StringValuePattern(matchesPayloadPattern) {
        override fun match(value: String?): MatchResult {

            return Jackson.deserializeMaybe(value, DiagnosisKeysUploadRequest::class.java).map {
                val jws = JsonWebSignature().apply {
                    key = keyPair.public
                    compactSerialization = it.payload
                }

                val exposures = SystemObjectMapper.MAPPER.readValue(jws.payload, object : TypeReference<List<Exposure>>() {})

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


