package uk.nhs.nhsx.keyfederation

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.s3.model.S3ObjectSummary
import com.amazonaws.xray.strategy.ContextMissingStrategy
import com.fasterxml.jackson.annotation.JsonProperty
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.matching.MatchResult
import com.github.tomakehurst.wiremock.matching.MatchResult.exactMatch
import com.github.tomakehurst.wiremock.matching.MatchResult.noMatch
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.nhs.nhsx.core.Json.readJsonOrNull
import uk.nhs.nhsx.core.SystemClock
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.events.IncomingHttpResponse
import uk.nhs.nhsx.core.events.InfoEvent
import uk.nhs.nhsx.core.events.OutgoingHttpRequest
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.diagnosiskeydist.s3.SubmissionFromS3Repository
import uk.nhs.nhsx.domain.ReportType
import uk.nhs.nhsx.domain.TestType
import uk.nhs.nhsx.keyfederation.upload.DiagnosisKeysUploadRequest
import uk.nhs.nhsx.keyfederation.upload.DiagnosisKeysUploadService
import uk.nhs.nhsx.keyfederation.upload.ExposureUpload
import uk.nhs.nhsx.keyfederation.upload.FederatedExposureUploadFactory
import uk.nhs.nhsx.keyfederation.upload.JWS
import uk.nhs.nhsx.testhelper.ContextBuilder.TestContext
import uk.nhs.nhsx.testhelper.mocks.FakeInteropDiagnosisKeysS3
import uk.nhs.nhsx.testhelper.mocks.FakeSubmissionRepository
import uk.nhs.nhsx.testhelper.wiremock.WireMockExtension
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset.UTC
import java.util.*

@ExtendWith(WireMockExtension::class)
class DiagnosisKeysUploadServiceTest(private val wireMock: WireMockServer) {

    @BeforeEach
    fun disableXrayLogging() {
        System.setProperty(ContextMissingStrategy.CONTEXT_MISSING_STRATEGY_SYSTEM_PROPERTY_OVERRIDE_KEY, "LOG_ERROR")
    }

    private val events = RecordingEvents()
    private val clock = Clock.fixed(Instant.parse("2021-02-02T11:13:00.000Z"), UTC)
    private val now = Instant.now(clock)

    @Test
    fun `test update risk level if default enabled`() {
        val jws = mockk<JWS>()
        every { jws.sign(any()) }.returns("DUMMY_SIGNATURE")

        val service = DiagnosisKeysUploadService(
            { now },
            InteropClient(wireMock.baseUrl(), "DUMMY_TOKEN", jws, events),
            FakeSubmissionRepository(listOf(now)),
            InMemoryBatchTagService(),
            FederatedExposureUploadFactory("GB-EAW"),
            true,
            2,
            14,
            0,
            100,
            TestContext(),
            events
        )

        val transformed = service.updateRiskLevelIfDefaultEnabled(
            ExposureUpload(
                "key",
                0,
                4,
                144,
                emptyList(),
                TestType.LAB_RESULT,
                ReportType.CONFIRMED_TEST,
                0
            )
        )

        assertThat(transformed.transmissionRiskLevel).isEqualTo(2)
    }

    @Test
    fun `upload diagnosis keys and raises event`() {
        val jws = mockk<JWS>()
        every { jws.sign(any()) }.returns("DUMMY_SIGNATURE")

        wireMock.stubFor(
            post("/diagnosiskeys/upload")
                .withHeader("Authorization", equalTo("Bearer" + " DUMMY_TOKEN")) // string split on purpose
                .withRequestBody(UploadPayloadPattern())
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

        val service = DiagnosisKeysUploadService(
            { now },
            InteropClient(wireMock.baseUrl(), "DUMMY_TOKEN", jws, events),
            FakeSubmissionRepository(listOf(now)),
            InMemoryBatchTagService(),
            FederatedExposureUploadFactory("GB-EAW"),
            false,
            -1,
            14,
            0,
            100,
            TestContext(),
            events
        )

        service.loadKeysAndUploadToFederatedServer()

        events.containsExactly(
            InfoEvent::class,
            InfoEvent::class,
            IncomingHttpResponse::class,
            UploadedDiagnosisKeys::class,
            DiagnosisKeysUploadIncomplete::class
        )
    }

    @Test
    fun `no filter for federation diagnosis keys and upload`() {
        val jws = mockk<JWS>()
        every { jws.sign(any()) }.returns("DUMMY_SIGNATURE")

        wireMock.stubFor(
            post("/diagnosiskeys/upload")
                .withHeader("Authorization", equalTo("Bearer DUMMY_TOKEN"))
                .withRequestBody(UploadPayloadPattern())
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withBody(
                            """
                            {
                                "batchTag": "75b326f7-ae6f-42f6-9354-00c0a6b797b3",
                                "insertedExposures":0
                            }""".trimIndent()
                        )
                )
        )

        val interopClient = mockk<InteropClient>()
        val payload = slot<List<ExposureUpload>>()

        every {
            interopClient.uploadKeys(capture(payload))
        }.answers {
            InteropClient(
                wireMock.baseUrl(),
                "DUMMY_TOKEN",
                jws, events
            ).uploadKeys(payload.captured)
        }

        val service = DiagnosisKeysUploadService(
            { now },
            interopClient,
            SubmissionFromS3Repository(
                FakeInteropDiagnosisKeysS3(listOf(s3ObjectSummary("mobile/LAB_RESULT/abc"), s3ObjectSummary("mobile/RAPID_RESULT/def"))),
                { true },
                BucketName.of("SUBMISSION_BUCKET"),
                RecordingEvents(),
                SystemClock.CLOCK
            ),
            InMemoryBatchTagService(),
            FederatedExposureUploadFactory("GB-EAW"),
            false,
            -1,
            14,
            0,
            100,
            TestContext(),
            RecordingEvents()
        )

        service.loadKeysAndUploadToFederatedServer()

        assertThat(payload.captured).hasSize(2)
    }

    @Test
    fun `filter prefix for federation diagnosis keys and upload`() {
        val jws = mockk<JWS>()
        every { jws.sign(any()) }.returns("DUMMY_SIGNATURE")

        wireMock.stubFor(
            post("/diagnosiskeys/upload")
                .withHeader("Authorization", equalTo("Bearer DUMMY_TOKEN"))
                .withRequestBody(UploadPayloadPattern())
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

        val interopClient = mockk<InteropClient>()
        val payload = slot<List<ExposureUpload>>()

        every {
            interopClient.uploadKeys(capture(payload))
        }.answers {
            InteropClient(
                wireMock.baseUrl(),
                "DUMMY_TOKEN",
                jws, events
            ).uploadKeys(payload.captured)
        }

        val service = DiagnosisKeysUploadService(
            { now },
            interopClient,
            SubmissionFromS3Repository(
                FakeInteropDiagnosisKeysS3(listOf(s3ObjectSummary("mobile/RAPID_RESULT/abc.json"), s3ObjectSummary("bar"))),
                { objectKey -> objectKey.value.startsWith("mobile") },
                BucketName.of("SUBMISSION_BUCKET"),
                RecordingEvents(),
                SystemClock.CLOCK
            ),
            InMemoryBatchTagService(),
            FederatedExposureUploadFactory("GB-EAW"),
            false,
            -1,
            14,
            0,
            100,
            TestContext(),
            RecordingEvents()
        )

        service.loadKeysAndUploadToFederatedServer()

        assertThat(payload.captured).hasSize(1)
    }

    @Test
    fun `verify diagnosis keys upload updates time in database`() {
        val jws = mockk<JWS>()
        every { jws.sign(any()) }.returns("DUMMY_SIGNATURE")

        wireMock.stubFor(
            post("/diagnosiskeys/upload")
                .withHeader("Authorization", equalTo("Bearer DUMMY_TOKEN"))
                .withRequestBody(UploadPayloadPattern())
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

        val batchTagService = spyk(InMemoryBatchTagService())

        val service = DiagnosisKeysUploadService(
            { now },
            InteropClient(wireMock.baseUrl(), "DUMMY_TOKEN", jws, events),
            FakeSubmissionRepository(listOf(now)),
            batchTagService,
            FederatedExposureUploadFactory("GB-EAW"),
            false,
            -1,
            14,
            0,
            100,
            TestContext(),
            RecordingEvents()
        )

        service.loadKeysAndUploadToFederatedServer()

        verify { batchTagService.updateLastUploadState(any()) }
    }

    @Test
    fun `stop the upload loop if the remaining time is not sufficient`() {
        val jws = mockk<JWS>()
        every { jws.sign(any()) }.returns("DUMMY_SIGNATURE")

        val context = mockk<Context>()
        every { context.remainingTimeInMillis }.returns(-2)

        wireMock.stubFor(
            post("/diagnosiskeys/upload")
                .withHeader("Authorization", equalTo("Bearer DUMMY_TOKEN"))
                .withRequestBody(UploadPayloadPattern())
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withBody(
                            """
                            {
                                "batchTag": "75b326f7-ae6f-42f6-9354-00c0a6b797b3",
                                "insertedExposures":1
                            }
                            """.trimIndent()
                        )
                )
        )

        val lastModifiedDateBatchOne = Date.from(now.minusSeconds(4))
        val lastModifiedDateBatchTwo = Date.from(now)

        val service = DiagnosisKeysUploadService(
            { now },
            InteropClient(wireMock.baseUrl(), "DUMMY_TOKEN", jws, events),
            SubmissionFromS3Repository(
                FakeInteropDiagnosisKeysS3(
                    listOf(
                        s3ObjectSummary("foo", lastModifiedDateBatchOne),
                        s3ObjectSummary("bar", lastModifiedDateBatchOne),
                        s3ObjectSummary("abc", lastModifiedDateBatchOne),
                        s3ObjectSummary("def", lastModifiedDateBatchTwo)
                    )
                ),
                { true },
                BucketName.of("SUBMISSION_BUCKET"),
                RecordingEvents(),
                SystemClock.CLOCK
            ),
            InMemoryBatchTagService(),
            FederatedExposureUploadFactory("GB-EAW"),
            false,
            -1,
            14,
            5,
            2,
            context,
            RecordingEvents()
        )

        assertThat(service.loadKeysAndUploadToFederatedServer()).isEqualTo(3)
    }

    @Test
    fun `continue uploading the keys if we have enough time to execute the next batch`() {
        val jws = mockk<JWS>()
        every { jws.sign(any()) }.returns("DUMMY_SIGNATURE")

        val context = mockk<Context>()
        every { context.remainingTimeInMillis }.returns(1000000)

        wireMock.stubFor(
            post("/diagnosiskeys/upload")
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withBody(
                            """
                            {
                                "batchTag": "75b326f7-ae6f-42f6-9354-00c0a6b797b3",
                                "insertedExposures":1
                            }
                            """.trimIndent()
                        )
                )
        )

        val lastModifiedDate = Date.from(now.minusSeconds(4))

        val service = DiagnosisKeysUploadService(
            { now },
            InteropClient(wireMock.baseUrl(), "DUMMY_TOKEN", jws, events),
            SubmissionFromS3Repository(
                FakeInteropDiagnosisKeysS3(
                    listOf(
                        s3ObjectSummary("foo", lastModifiedDate),
                        s3ObjectSummary("bar", lastModifiedDate),
                        s3ObjectSummary("abc", lastModifiedDate),
                        s3ObjectSummary("def", lastModifiedDate)
                    )
                ),
                { true },
                BucketName.of("SUBMISSION_BUCKET"),
                RecordingEvents(),
                SystemClock.CLOCK
            ),
            InMemoryBatchTagService(),
            FederatedExposureUploadFactory("GB-EAW"),
            false,
            -1,
            14,
            5,
            2,
            context,
            RecordingEvents()
        )

        assertThat(service.loadKeysAndUploadToFederatedServer()).isEqualTo(4)
    }

    private fun s3ObjectSummary(
        s3Key: String,
        lastModifiedDate: Date = Date.from(Instant.now(clock))
    ): S3ObjectSummary {
        return S3ObjectSummary().apply {
            key = s3Key
            lastModified = lastModifiedDate
        }
    }

    class UploadPayloadPattern(@JsonProperty matchesPayloadPattern: String = """{ batchTag: [a-f0-9\-]+, payload: "DUMMY_SIGNATURE" }""") :
        StringValuePattern(matchesPayloadPattern) {
        override fun match(value: String): MatchResult = readJsonOrNull<DiagnosisKeysUploadRequest>(value)
            ?.let {
                when {
                    it.batchTag.value.matches(Regex("[a-f0-9\\-]+")) && it.payload == "DUMMY_SIGNATURE" -> exactMatch()
                    else -> noMatch()
                }
            } ?: noMatch()
    }

}

