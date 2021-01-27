package uk.nhs.nhsx.keyfederation.download

import com.amazonaws.services.s3.model.S3ObjectSummary
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.Jackson
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.secretsmanager.SecretName
import uk.nhs.nhsx.core.aws.ssm.ParameterName
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKeyPayload
import uk.nhs.nhsx.keyfederation.BatchTag
import uk.nhs.nhsx.keyfederation.InMemoryBatchTagService
import uk.nhs.nhsx.keyfederation.InteropClient
import uk.nhs.nhsx.keyfederation.TestKeyPairs.ecPrime256r1
import uk.nhs.nhsx.keyfederation.upload.JWS
import uk.nhs.nhsx.keyfederation.upload.KmsCompatibleSigner
import uk.nhs.nhsx.testhelper.ContextBuilder
import uk.nhs.nhsx.testhelper.mocks.FakeDiagnosisKeysS3
import uk.nhs.nhsx.testhelper.mocks.FakeS3StorageMultipleObjects
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

class KeyFederationDownloadHandlerTest {

    private val wireMockRule: WireMockServer = WireMockServer(0)

    private lateinit var downloadEnabledConfig: KeyFederationDownloadConfig

    @BeforeEach
    fun start() {
        wireMockRule.start()
        downloadEnabledConfig = KeyFederationDownloadConfig(
            100,
            14,
            { true },
            false, -1,
            BucketName.of("foo"),
            wireMockRule.baseUrl(),
            SecretName.of("authToken"),
            ParameterName.of("parameter"),
            "federatedKeyDownloadPrefix",
            "DUMMY_TABLE",
            listOf("GB-EAW"),
            "GB-EAW"
        )
    }

    @AfterEach
    fun stop() = wireMockRule.stop()

    @Test
    fun `enable download should call interop`() {
        wireMockRule.stubFor(get("/diagnosiskeys/download/2020-08-01").willReturn(aResponse()
            .withStatus(204)
        ))

        KeyFederationDownloadHandler(
            { LocalDate.of(2020, 8, 15).atStartOfDay().toInstant(ZoneOffset.UTC) },
            downloadEnabledConfig,
            InMemoryBatchTagService(),
            { InteropClient(wireMockRule.baseUrl(), "DUMMY_TOKEN", JWS(KmsCompatibleSigner(ecPrime256r1.private))) },
            FakeDiagnosisKeysS3(listOf(
                S3ObjectSummary().apply {
                    key = "foo"
                    lastModified = Date.from(OffsetDateTime.now(ZoneOffset.UTC).toInstant())
                }
            ))
        ).handleRequest(null, null)

        wireMockRule.verify(
            getRequestedFor(urlEqualTo("/diagnosiskeys/download/2020-08-01"))
                .withHeader("Authorization", equalTo("Bearer" + " DUMMY_TOKEN")) // on purpose
        )
    }

    @Test
    fun `disable download should not call interop`() {
        wireMockRule.stubFor(get("/diagnosiskeys/download/2020-08-01").willReturn(aResponse()
            .withStatus(204)
        ))

        KeyFederationDownloadHandler(
            { LocalDate.of(2020, 8, 15).atStartOfDay().toInstant(ZoneOffset.UTC) },
            KeyFederationDownloadConfig(
                100,
                14,
                { false },
                false, -1,
                BucketName.of("foo"),
                wireMockRule.baseUrl(),
                SecretName.of("authToken"),
                ParameterName.of("parameter"),
                "federatedKeyDownloadPrefix",
                "DUMMY_TABLE",
                listOf("GB-EAW"),
                "GB-EAW",
            ),
            InMemoryBatchTagService(),
            { InteropClient(wireMockRule.baseUrl(), "DUMMY_TOKEN", JWS(KmsCompatibleSigner(ecPrime256r1.private))) },
            FakeDiagnosisKeysS3(emptyList())
        ).handleRequest(null, null)

        wireMockRule.verify(0,
            getRequestedFor(urlEqualTo("/diagnosiskeys/download/2020-08-01"))
                .withHeader("Authorization", equalTo("Bearer DUMMY_TOKEN"))
        )
    }

    @Test
    fun `download keys with no content does nothing`() {
        wireMockRule.stubFor(
            get("/diagnosiskeys/download/2020-08-01?batchTag=75b326f7-ae6f-42f6-9354-00c0a6b797b3")
                .willReturn(aResponse().withStatus(204))
        )

        val batchTagService = InMemoryBatchTagService(BatchTag.of("75b326f7-ae6f-42f6-9354-00c0a6b797b3"), LocalDate.of(2020, 8, 1))
        val fakeS3Storage = FakeS3StorageMultipleObjects()

        KeyFederationDownloadHandler(
            { LocalDate.of(2020, 8, 1).atStartOfDay().toInstant(ZoneOffset.UTC) },
            downloadEnabledConfig,
            batchTagService,
            { InteropClient(wireMockRule.baseUrl(), "DUMMY_TOKEN", JWS(KmsCompatibleSigner(ecPrime256r1.private))) },
            fakeS3Storage
        ).handleRequest(null, null)

        assertThat(fakeS3Storage.count).isEqualTo(0)
        assertThat(batchTagService.batchTag!!.value).isEqualTo("75b326f7-ae6f-42f6-9354-00c0a6b797b3")

        wireMockRule.verify(1,
            getRequestedFor(urlEqualTo("/diagnosiskeys/download/2020-08-01?batchTag=75b326f7-ae6f-42f6-9354-00c0a6b797b3"))
                .withHeader("Authorization", equalTo("Bearer DUMMY_TOKEN"))
        )
    }

    @Test
    fun `download keys with single page and save to s3`() {

        wireMockRule.stubFor(get("/diagnosiskeys/download/2020-08-01").willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json; charset=utf-8")
            .withBody("""
            {
                "batchTag": "75b326f7-ae6f-42f6-9354-00c0a6b797b3",
                "exposures": [
                    {
                        "keyData": "ogNW4Ra+Zdds1ShN56yv3w==",
                        "rollingStartNumber": 2660544,
                        "transmissionRiskLevel": 0,
                        "rollingPeriod": 144,
                        "origin": "GB-EAW",
                        "regions": [
                            "GB"
                        ]
                    },
                    {
                        "keyData": "EwoHez3CQgdslvdxaf+ztw==",
                        "rollingStartNumber": 2660544,
                        "transmissionRiskLevel": 0,
                        "rollingPeriod": 144,
                        "origin": "GB-EAW",
                        "regions": [
                            "GB"
                        ]
                    }
                ]
            }
            """.trimIndent()
            )
        ))

        wireMockRule.stubFor(
            get("/diagnosiskeys/download/2020-08-01?batchTag=75b326f7-ae6f-42f6-9354-00c0a6b797b3")
                .willReturn(aResponse().withStatus(204))
        )

        val batchTagService = InMemoryBatchTagService()
        val fakeS3Storage = FakeS3StorageMultipleObjects()

        KeyFederationDownloadHandler(
            { LocalDate.of(2020, 8, 15).atStartOfDay().toInstant(ZoneOffset.UTC) },
            downloadEnabledConfig,
            batchTagService,
            { InteropClient(wireMockRule.baseUrl(), "DUMMY_TOKEN", JWS(KmsCompatibleSigner(ecPrime256r1.private))) },
            fakeS3Storage
        ).handleRequest(null, ContextBuilder.aContext())

        assertThat(fakeS3Storage.count).isEqualTo(1)

        val keys = fakeS3Storage.fakeS3Objects.flatMap {
            Jackson.readJson(it.bytes.openStream(), StoredTemporaryExposureKeyPayload::class.java).temporaryExposureKeys
        }.map { it.key }

        assertThat(keys).hasSize(2)
        assertThat(keys).containsAll(listOf("ogNW4Ra+Zdds1ShN56yv3w==", "EwoHez3CQgdslvdxaf+ztw=="))

        assertThat(batchTagService.batchTag!!.value).isEqualTo("75b326f7-ae6f-42f6-9354-00c0a6b797b3")

        wireMockRule.verify(1,
            getRequestedFor(urlEqualTo("/diagnosiskeys/download/2020-08-01"))
                .withHeader("Authorization", equalTo("Bearer DUMMY_TOKEN"))
        )

        wireMockRule.verify(1,
            getRequestedFor(urlEqualTo("/diagnosiskeys/download/2020-08-01?batchTag=75b326f7-ae6f-42f6-9354-00c0a6b797b3"))
                .withHeader("Authorization", equalTo("Bearer DUMMY_TOKEN"))
        )
    }

    @Test
    fun `download keys with single page is empty does nothing`() {

        wireMockRule.stubFor(get("/diagnosiskeys/download/2020-08-01").willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json; charset=utf-8")
            .withBody("""
            {
                "batchTag": "75b326f7-ae6f-42f6-9354-00c0a6b797b3",
                "exposures": []
            }
            """.trimIndent()
            )
        ))

        wireMockRule.stubFor(
            get("/diagnosiskeys/download/2020-08-01?batchTag=75b326f7-ae6f-42f6-9354-00c0a6b797b3")
                .willReturn(aResponse().withStatus(204))
        )

        val batchTagService = InMemoryBatchTagService()
        val fakeS3Storage = FakeS3StorageMultipleObjects()

        KeyFederationDownloadHandler(
            { LocalDate.of(2020, 8, 15).atStartOfDay().toInstant(ZoneOffset.UTC) },
            downloadEnabledConfig,
            batchTagService,
            { InteropClient(wireMockRule.baseUrl(), "DUMMY_TOKEN", JWS(KmsCompatibleSigner(ecPrime256r1.private))) },
            fakeS3Storage
        ).handleRequest(null, ContextBuilder.aContext() )


        assertThat(fakeS3Storage.count).isEqualTo(0)
        assertThat(batchTagService.batchTag!!.value).isEqualTo("75b326f7-ae6f-42f6-9354-00c0a6b797b3")

        wireMockRule.verify(1,
            getRequestedFor(urlEqualTo("/diagnosiskeys/download/2020-08-01"))
                .withHeader("Authorization", equalTo("Bearer DUMMY_TOKEN"))
        )

        wireMockRule.verify(1,
            getRequestedFor(urlEqualTo("/diagnosiskeys/download/2020-08-01?batchTag=75b326f7-ae6f-42f6-9354-00c0a6b797b3"))
                .withHeader("Authorization", equalTo("Bearer DUMMY_TOKEN"))
        )
    }
}


