@file:Suppress("TestFunctionName")

package uk.nhs.nhsx.keyfederation.download

import com.amazonaws.services.lambda.runtime.events.ScheduledEvent
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import strikt.api.expect
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.elementAt
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.map
import uk.nhs.nhsx.core.aws.dynamodb.TableName
import uk.nhs.nhsx.core.aws.s3.AwsS3
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.secretsmanager.SecretName
import uk.nhs.nhsx.core.aws.ssm.ParameterName
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKey
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKeyPayload
import uk.nhs.nhsx.domain.BatchTag
import uk.nhs.nhsx.keyfederation.BatchTagService
import uk.nhs.nhsx.keyfederation.InMemoryBatchTagService
import uk.nhs.nhsx.keyfederation.InteropClient
import uk.nhs.nhsx.keyfederation.TestKeyPairs.ecPrime256r1
import uk.nhs.nhsx.keyfederation.upload.JWS
import uk.nhs.nhsx.keyfederation.upload.KmsCompatibleSigner
import uk.nhs.nhsx.testhelper.ContextBuilder.Companion.aContext
import uk.nhs.nhsx.testhelper.assertions.S3ObjectAssertions.content
import uk.nhs.nhsx.testhelper.data.asInstant
import uk.nhs.nhsx.testhelper.mocks.FakeS3
import uk.nhs.nhsx.testhelper.mocks.exposureS3Object
import uk.nhs.nhsx.testhelper.mocks.getBucket
import uk.nhs.nhsx.testhelper.mocks.isEmpty
import uk.nhs.nhsx.testhelper.mocks.withReadJsonOrThrows
import uk.nhs.nhsx.testhelper.wiremock.WireMockExtension
import java.time.Instant
import java.time.LocalDate
import java.util.*

@ExtendWith(WireMockExtension::class)
class KeyFederationDownloadHandlerTest(private val wireMock: WireMockServer) {

    private val events = RecordingEvents()
    private val bucketName = BucketName.of(UUID.randomUUID().toString())

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

        val s3 = FakeS3()
        s3.add(exposureS3Object("foo", bucketName), Instant.now())

        val config = keyFederationDownloadConfig(
            wireMockServer = wireMock,
            bucketName = bucketName
        )

        keyFederationDownloadHandler(
            wireMockServer = wireMock,
            keyFederationDownloadConfig = config,
            awsS3 = s3
        ).handleRequest(ScheduledEvent(), aContext())

        wireMock.verify(1, getRequestedFor(urlEqualTo("/diagnosiskeys/download/2020-08-01")))
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

        val config = keyFederationDownloadConfig(wireMock, bucketName = bucketName) { false }

        keyFederationDownloadHandler(
            wireMockServer = wireMock,
            keyFederationDownloadConfig = config
        ).handleRequest(ScheduledEvent(), aContext())

        wireMock.verify(
            0, getRequestedFor(urlEqualTo("/diagnosiskeys/download/2020-08-01"))
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

        val fakeS3 = FakeS3()

        keyFederationDownloadHandler(
            wireMockServer = wireMock,
            keyFederationDownloadConfig = keyFederationDownloadConfig(wireMock),
            awsS3 = fakeS3,
            batchTagService = batchTagService
        ).handleRequest(ScheduledEvent(), aContext())

        expect {
            that(fakeS3).isEmpty(bucketName)

            that(batchTagService)
                .get(InMemoryBatchTagService::batchTag)
                .isNotNull()
                .isEqualTo(BatchTag.of("75b326f7-ae6f-42f6-9354-00c0a6b797b3"))
        }

        wireMock.verify(1, getRequestedFor(urlPathEqualTo("/diagnosiskeys/download/2020-08-01")))
    }

    @Test
    fun `download keys with single page and save to s3`() {
        wireMock.stubFor(
            get("/diagnosiskeys/download/2021-01-28")
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
                                        "keyData": "9m008UTn46C32jsWEw1Dnw==",
                                        "rollingStartNumber": 2686464,
                                        "transmissionRiskLevel": 0,
                                        "rollingPeriod": 144,
                                        "origin": "GB-NIR",
                                        "reportType": 1,
                                        "daysSinceOnset": 0,
                                        "testType": 1,
                                        "regions": [
                                            "GB"
                                        ]
                                    },
                                    {
                                        "keyData": "p05ot/jyF58G/95CkujQYQ==",
                                        "rollingStartNumber": 2686896,
                                        "transmissionRiskLevel": 0,
                                        "rollingPeriod": 101,
                                        "origin": "GB-NIR",
                                        "reportType": 1,
                                        "daysSinceOnset": 0,
                                        "testType": 1,
                                        "regions": [
                                            "GB"
                                        ]
                                    },
                                    {
                                        "keyData": "ViRF6pOEFdVnk73aBrEwcA==",
                                        "rollingStartNumber": 2686896,
                                        "transmissionRiskLevel": 0,
                                        "rollingPeriod": 101,
                                        "origin": "GB-NIR",
                                        "reportType": 1,
                                        "daysSinceOnset": 0,
                                        "testType": 1,
                                        "regions": [
                                            "GB"
                                        ]
                                    },
                                    {
                                        "keyData": "6X8NHosmohtYiMbqHrdaJA==",
                                        "rollingStartNumber": 2686896,
                                        "transmissionRiskLevel": 0,
                                        "rollingPeriod": 101,
                                        "origin": "GB-NIR",
                                        "reportType": 3,
                                        "daysSinceOnset": 0,
                                        "testType": 1,
                                        "regions": [
                                            "GB"
                                        ]
                                    },
                                    {
                                        "keyData": "Y4cQpuB6Jyhs6fKn2GjCEw==",
                                        "rollingStartNumber": 2686896,
                                        "transmissionRiskLevel": 0,
                                        "rollingPeriod": 101,
                                        "origin": "GB-NIR",
                                        "reportType": 1,
                                        "daysSinceOnset": 0,
                                        "testType": 3,
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
            get(urlPathEqualTo("/diagnosiskeys/download/2021-01-28"))
                .withQueryParam("batchTag", equalTo("75b326f7-ae6f-42f6-9354-00c0a6b797b3"))
                .withHeader("Authorization", equalTo("Bearer DUMMY_TOKEN"))
                .willReturn(
                    aResponse()
                        .withStatus(204)
                )
        )

        val batchTagService = InMemoryBatchTagService()
        val fakeS3 = FakeS3()

        val clock = { "2021-02-11T00:00:00.000Z".asInstant() }

        keyFederationDownloadHandler(
            wireMockServer = wireMock,
            keyFederationDownloadConfig = keyFederationDownloadConfig(wireMock, bucketName = bucketName),
            awsS3 = fakeS3,
            batchTagService = batchTagService,
            clock = clock
        ).handleRequest(ScheduledEvent(), aContext())

        expectThat(fakeS3) {
            getBucket(bucketName).hasSize(1).and {
                elementAt(0).content.withReadJsonOrThrows<StoredTemporaryExposureKeyPayload> {
                    get(StoredTemporaryExposureKeyPayload::temporaryExposureKeys)
                        .map(StoredTemporaryExposureKey::key)
                        .containsExactly(
                            "9m008UTn46C32jsWEw1Dnw==",
                            "p05ot/jyF58G/95CkujQYQ==",
                            "ViRF6pOEFdVnk73aBrEwcA=="
                        )

                }
            }
        }

        expectThat(batchTagService)
            .get(InMemoryBatchTagService::batchTag)
            .isNotNull()
            .isEqualTo(BatchTag.of("75b326f7-ae6f-42f6-9354-00c0a6b797b3"))

        wireMock.verify(2, getRequestedFor(urlPathEqualTo("/diagnosiskeys/download/2021-01-28")))
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
                        .withBody("""{ "batchTag": "75b326f7-ae6f-42f6-9354-00c0a6b797b3", "exposures": [] }""")
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
        val fakeS3 = FakeS3()

        keyFederationDownloadHandler(
            wireMockServer = wireMock,
            keyFederationDownloadConfig = keyFederationDownloadConfig(wireMock, bucketName = bucketName),
            awsS3 = fakeS3,
            batchTagService = batchTagService,
        ).handleRequest(ScheduledEvent(), aContext())

        expect {
            that(fakeS3).isEmpty(bucketName)

            that(batchTagService)
                .get(InMemoryBatchTagService::batchTag)
                .isNotNull()
                .isEqualTo(BatchTag.of("75b326f7-ae6f-42f6-9354-00c0a6b797b3"))
        }

        wireMock.verify(2, getRequestedFor(urlPathEqualTo("/diagnosiskeys/download/2020-08-01")))
    }

    private fun keyFederationDownloadConfig(
        wireMockServer: WireMockServer,
        bucketName: BucketName = BucketName.of("foo"),
        featureFlag: () -> Boolean = { true },
    ) = KeyFederationDownloadConfig(
        maxSubsequentBatchDownloadCount = 100,
        initialDownloadHistoryDays = 14,
        downloadFeatureFlag = featureFlag,
        downloadRiskLevelDefaultEnabled = false,
        downloadRiskLevelDefault = -1,
        submissionBucketName = bucketName,
        interopBaseUrl = wireMockServer.baseUrl(),
        interopAuthTokenSecretName = SecretName.of("authToken"),
        signingKeyParameterName = ParameterName.of("parameter"),
        federatedKeyDownloadPrefix = "federatedKeyDownloadPrefix",
        stateTableName = TableName.of("DUMMY_TABLE"),
        validOrigins = listOf("GB-EAW", "GB-NIR")
    )

    private fun keyFederationDownloadHandler(
        wireMockServer: WireMockServer,
        keyFederationDownloadConfig: KeyFederationDownloadConfig,
        awsS3: AwsS3 = FakeS3(),
        batchTagService: BatchTagService = InMemoryBatchTagService(),
        clock: () -> Instant = { "2020-08-15T00:00:00.000Z".asInstant() }
    ): KeyFederationDownloadHandler {
        val interopClient = InteropClient(
            interopBaseUrl = wireMockServer.baseUrl(),
            authToken = "DUMMY_TOKEN",
            jws = JWS(KmsCompatibleSigner(ecPrime256r1.private)),
            events = events
        )

        return KeyFederationDownloadHandler(
            clock = clock,
            events = events,
            config = keyFederationDownloadConfig,
            batchTagService = batchTagService,
            interopClient = interopClient,
            awsS3 = awsS3
        )
    }
}


