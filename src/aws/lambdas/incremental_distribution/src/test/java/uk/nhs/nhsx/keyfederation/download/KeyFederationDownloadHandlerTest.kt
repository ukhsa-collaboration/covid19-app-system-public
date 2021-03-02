package uk.nhs.nhsx.keyfederation.download

import com.amazonaws.services.lambda.runtime.events.ScheduledEvent
import com.amazonaws.services.s3.model.S3ObjectSummary
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.exactly
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.nhs.nhsx.core.Jackson.readJson
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.secretsmanager.SecretName
import uk.nhs.nhsx.core.aws.ssm.ParameterName
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.RecordingEvents
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
import uk.nhs.nhsx.testhelper.proxy
import uk.nhs.nhsx.testhelper.wiremock.WireMockExtension
import java.time.Instant
import java.time.LocalDate
import java.util.Date

@ExtendWith(WireMockExtension::class)
class KeyFederationDownloadHandlerTest(private val wireMock: WireMockServer) {

    private val events: Events = RecordingEvents()
    private lateinit var downloadEnabledConfig: KeyFederationDownloadConfig

    @BeforeEach
    fun start() {
        downloadEnabledConfig = KeyFederationDownloadConfig(
            100,
            14,
            { true },
            false, -1,
            BucketName.of("foo"),
            wireMock.baseUrl(),
            SecretName.of("authToken"),
            ParameterName.of("parameter"),
            "federatedKeyDownloadPrefix",
            "DUMMY_TABLE",
            listOf("GB-EAW"),
            "GB-EAW"
        )
    }

    @Test
    fun `enable download should call interop`() {
        wireMock.stubFor(
            get("/diagnosiskeys/download/2020-08-01")
                .withHeader("Authorization", equalTo("Bearer DUMMY_TOKEN"))
                .willReturn(
                    aResponse()
                        .withStatus(204)
                )
        )

        KeyFederationDownloadHandler(
            { "2020-08-15T00:00:00.000Z".asInstant() },
            events,
            downloadEnabledConfig,
            InMemoryBatchTagService(),
            interopClient = InteropClient(
                wireMock.baseUrl(),
                "DUMMY_TOKEN",
                JWS(KmsCompatibleSigner(ecPrime256r1.private)),
                events
            ),
            awsS3Client = FakeDiagnosisKeysS3(listOf(
                S3ObjectSummary().apply {
                    key = "foo"
                    lastModified = Date.from(Instant.now())
                }
            ))
        ).handleRequest(ScheduledEvent(), proxy())
    }

    @Test
    fun `disable download should not call interop`() {
        wireMock.stubFor(
            get("/diagnosiskeys/download/2020-08-01")
                .withHeader("Authorization", equalTo("Bearer DUMMY_TOKEN"))
                .willReturn(
                    aResponse()
                        .withStatus(204)
                )
        )

        KeyFederationDownloadHandler(
            { "2020-08-15T00:00:00.000Z".asInstant() },
            events,
            KeyFederationDownloadConfig(
                100,
                14,
                { false },
                false, -1,
                BucketName.of("foo"),
                wireMock.baseUrl(),
                SecretName.of("authToken"),
                ParameterName.of("parameter"),
                "federatedKeyDownloadPrefix",
                "DUMMY_TABLE",
                listOf("GB-EAW"),
                "GB-EAW",
            ),
            InMemoryBatchTagService(),
            interopClient = InteropClient(
                wireMock.baseUrl(),
                "DUMMY_TOKEN",
                JWS(KmsCompatibleSigner(ecPrime256r1.private)),
                events
            ),
            awsS3Client = FakeDiagnosisKeysS3(emptyList())
        ).handleRequest(ScheduledEvent(), proxy())

        wireMock.verify(
            exactly(0),
            getRequestedFor(urlEqualTo("/diagnosiskeys/download/2020-08-01"))
                .withHeader("Authorization", equalTo("Bearer DUMMY_TOKEN"))
        )
    }

    @Test
    fun `download keys with no content does nothing`() {
        wireMock.stubFor(
            get(urlPathEqualTo("/diagnosiskeys/download/2020-08-01"))
                .withQueryParam("batchTag", equalTo("75b326f7-ae6f-42f6-9354-00c0a6b797b3"))
                .withHeader("Authorization", equalTo("Bearer DUMMY_TOKEN"))
                .willReturn(
                    aResponse()
                        .withStatus(204)
                )
        )

        val batchTagService = InMemoryBatchTagService(
            BatchTag.of("75b326f7-ae6f-42f6-9354-00c0a6b797b3"),
            LocalDate.of(2020, 8, 1)
        )

        val fakeS3Storage = FakeS3StorageMultipleObjects()

        KeyFederationDownloadHandler(
            { "2020-08-01T00:00:00.000Z".asInstant() },
            events,
            downloadEnabledConfig,
            batchTagService,
            interopClient = InteropClient(
                wireMock.baseUrl(),
                "DUMMY_TOKEN",
                JWS(KmsCompatibleSigner(ecPrime256r1.private)),
                events
            ),
            awsS3Client = fakeS3Storage
        ).handleRequest(ScheduledEvent(), proxy())

        assertThat(fakeS3Storage.count).isEqualTo(0)
        assertThat(batchTagService.batchTag!!.value).isEqualTo("75b326f7-ae6f-42f6-9354-00c0a6b797b3")
    }

    @Test
    fun `download keys with single page and save to s3`() {
        wireMock.stubFor(
            get("/diagnosiskeys/download/2020-08-01")
                .withHeader("Authorization", equalTo("Bearer DUMMY_TOKEN"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json; charset=utf-8")
                        .withBody(
                            """
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
                )
        )

        wireMock.stubFor(
            get(urlPathEqualTo("/diagnosiskeys/download/2020-08-01"))
                .withQueryParam("batchTag", equalTo("75b326f7-ae6f-42f6-9354-00c0a6b797b3"))
                .withHeader("Authorization", equalTo("Bearer DUMMY_TOKEN"))
                .willReturn(
                    aResponse()
                        .withStatus(204)
                )
        )

        val batchTagService = InMemoryBatchTagService()
        val fakeS3Storage = FakeS3StorageMultipleObjects()

        KeyFederationDownloadHandler(
            { "2020-08-15T00:00:00.000Z".asInstant() },
            events,
            downloadEnabledConfig,
            batchTagService,
            interopClient = InteropClient(
                wireMock.baseUrl(),
                "DUMMY_TOKEN",
                JWS(KmsCompatibleSigner(ecPrime256r1.private)),
                events
            ),
            awsS3Client = fakeS3Storage
        ).handleRequest(ScheduledEvent(), ContextBuilder.aContext())

        assertThat(fakeS3Storage.count).isEqualTo(1)

        val keys = fakeS3Storage.fakeS3Objects
            .flatMap {
                readJson(
                    it.bytes.openStream(),
                    StoredTemporaryExposureKeyPayload::class.java
                ).temporaryExposureKeys
            }
            .map { it.key }

        assertThat(keys).hasSize(2)
        assertThat(keys).containsAll(listOf("ogNW4Ra+Zdds1ShN56yv3w==", "EwoHez3CQgdslvdxaf+ztw=="))
        assertThat(batchTagService.batchTag!!.value).isEqualTo("75b326f7-ae6f-42f6-9354-00c0a6b797b3")
    }

    @Test
    fun `download keys with single page is empty does nothing`() {
        wireMock.stubFor(
            get("/diagnosiskeys/download/2020-08-01")
                .withHeader("Authorization", equalTo("Bearer DUMMY_TOKEN"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json; charset=utf-8")
                        .withBody(
                            """
                        {
                            "batchTag": "75b326f7-ae6f-42f6-9354-00c0a6b797b3",
                            "exposures": []
                        }
                        """.trimIndent()
                        )
                )
        )

        wireMock.stubFor(
            get(urlPathEqualTo("/diagnosiskeys/download/2020-08-01"))
                .withQueryParam("batchTag", equalTo("75b326f7-ae6f-42f6-9354-00c0a6b797b3"))
                .withHeader("Authorization", equalTo("Bearer DUMMY_TOKEN"))
                .willReturn(
                    aResponse()
                        .withStatus(204)
                )
        )

        val batchTagService = InMemoryBatchTagService()
        val fakeS3Storage = FakeS3StorageMultipleObjects()

        KeyFederationDownloadHandler(
            { "2020-08-15T00:00:00.000Z".asInstant() },
            events,
            downloadEnabledConfig,
            batchTagService,
            interopClient = InteropClient(
                wireMock.baseUrl(),
                "DUMMY_TOKEN",
                JWS(KmsCompatibleSigner(ecPrime256r1.private)),
                events
            ),
            awsS3Client = fakeS3Storage
        ).handleRequest(ScheduledEvent(), ContextBuilder.aContext())

        assertThat(fakeS3Storage.count).isEqualTo(0)
        assertThat(batchTagService.batchTag!!.value).isEqualTo("75b326f7-ae6f-42f6-9354-00c0a6b797b3")
    }

    private fun String.asInstant() = Instant.parse(this)
}


