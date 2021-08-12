@file:Suppress("TestFunctionName")

package uk.nhs.nhsx.keyfederation

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.s3.model.S3ObjectSummary
import com.fasterxml.jackson.annotation.JsonProperty
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.matching.MatchResult
import com.github.tomakehurst.wiremock.matching.MatchResult.exactMatch
import com.github.tomakehurst.wiremock.matching.MatchResult.noMatch
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import uk.nhs.nhsx.core.Json.readJsonOrNull
import uk.nhs.nhsx.core.SystemClock
import uk.nhs.nhsx.core.aws.s3.AwsS3
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.core.aws.xray.Tracing
import uk.nhs.nhsx.core.events.IncomingHttpResponse
import uk.nhs.nhsx.core.events.InfoEvent
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.diagnosiskeydist.SubmissionRepository
import uk.nhs.nhsx.diagnosiskeydist.s3.SubmissionFromS3Repository
import uk.nhs.nhsx.domain.ReportType.CONFIRMED_TEST
import uk.nhs.nhsx.domain.TestType.LAB_RESULT
import uk.nhs.nhsx.keyfederation.upload.DiagnosisKeysUploadRequest
import uk.nhs.nhsx.keyfederation.upload.DiagnosisKeysUploadService
import uk.nhs.nhsx.keyfederation.upload.ExposureUpload
import uk.nhs.nhsx.keyfederation.upload.FederatedExposureUploadFactory
import uk.nhs.nhsx.testhelper.assertions.containsExactly
import uk.nhs.nhsx.testhelper.assertions.withCaptured
import uk.nhs.nhsx.testhelper.mocks.FakeInteropDiagnosisKeysS3
import uk.nhs.nhsx.testhelper.mocks.FakeSubmissionRepository
import uk.nhs.nhsx.testhelper.s3.S3ObjectSummary
import uk.nhs.nhsx.testhelper.wiremock.WireMockExtension
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset.UTC

@ExtendWith(WireMockExtension::class)
class DiagnosisKeysUploadServiceTest(private val wireMock: WireMockServer) {

    private val events = RecordingEvents()
    private val clock = Clock.fixed(Instant.parse("2021-02-02T11:13:00.000Z"), UTC)
    private val now = Instant.now(clock)

    @BeforeEach
    fun disableXrayLogging() = Tracing.disableXRayComplaintsForMainClasses()

    @Test
    fun `test update risk level if default enabled`() {
        val service = DiagnosisKeysUploadService(
            interopClient = InteropClient(wireMock),
            submissionRepository = FakeSubmissionRepository(listOf(now)),
            uploadRiskLevelDefaultEnabled = true,
            uploadRiskLevelDefault = 2
        )

        val transformed = service.updateRiskLevelIfDefaultEnabled(
            ExposureUpload(
                keyData = "key",
                rollingStartNumber = 0,
                transmissionRiskLevel = 4,
                rollingPeriod = 144,
                regions = emptyList(),
                testType = LAB_RESULT,
                reportType = CONFIRMED_TEST,
                daysSinceOnset = 0
            )
        )

        expectThat(transformed)
            .get(ExposureUpload::transmissionRiskLevel)
            .isEqualTo(2)
    }

    @Test
    fun `upload diagnosis keys and raises event`() {
        wireMock.stubFor(
            post("/diagnosiskeys/upload")
                .withHeader("Authorization", equalTo("Bearer DUMMY_TOKEN"))
                .withRequestBody(UploadPayloadPattern())
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withBody("""{ "batchTag": "75b326f7-ae6f-42f6-9354-00c0a6b797b3", "insertedExposures":0 }""")
                )
        )

        val service = DiagnosisKeysUploadService(
            interopClient = InteropClient(wireMock),
            submissionRepository = FakeSubmissionRepository(listOf(now)),
        )

        service.loadKeysAndUploadToFederatedServer()

        wireMock.verify(postRequestedFor(urlEqualTo("/diagnosiskeys/upload")))

        expectThat(events).containsExactly(
            InfoEvent::class,
            InfoEvent::class,
            IncomingHttpResponse::class,
            UploadedDiagnosisKeys::class,
            DiagnosisKeysUploadIncomplete::class
        )
    }

    @Test
    fun `no filter for federation diagnosis keys and upload`() {
        wireMock.stubFor(
            post("/diagnosiskeys/upload")
                .withHeader("Authorization", equalTo("Bearer DUMMY_TOKEN"))
                .withRequestBody(UploadPayloadPattern())
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withBody("""{ "batchTag": "75b326f7-ae6f-42f6-9354-00c0a6b797b3", "insertedExposures":0 }""")
                )
        )

        val payload = slot<List<ExposureUpload>>()
        val interopClient = spyk(InteropClient(wireMock)) {
            every { uploadKeys(capture(payload)) } answers { callOriginal() }
        }

        val submissionRepository = SubmissionFromS3Repository(
            FakeInteropDiagnosisKeysS3(
                S3ObjectSummary("mobile/LAB_RESULT/abc", lastModified = now),
                S3ObjectSummary("mobile/RAPID_RESULT/def", lastModified = now)
            )
        )

        val service = DiagnosisKeysUploadService(
            interopClient = interopClient,
            submissionRepository = submissionRepository,
        )

        service.loadKeysAndUploadToFederatedServer()

        wireMock.verify(postRequestedFor(urlEqualTo("/diagnosiskeys/upload")))

        expectThat(payload).withCaptured { hasSize(2) }
    }

    @Test
    fun `filter prefix for federation diagnosis keys and upload`() {
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

        val payload = slot<List<ExposureUpload>>()
        val interopClient = spyk(InteropClient(wireMock)) {
            every { uploadKeys(capture(payload)) } answers { callOriginal() }
        }

        val awsS3 = FakeInteropDiagnosisKeysS3(
            S3ObjectSummary("mobile/RAPID_RESULT/abc.json", lastModified = now),
            S3ObjectSummary("bar", lastModified = now)
        )
        val submissionRepository = SubmissionFromS3Repository(awsS3) { it.value.startsWith("mobile") }

        val service = DiagnosisKeysUploadService(
            interopClient = interopClient,
            submissionRepository = submissionRepository,
        )

        service.loadKeysAndUploadToFederatedServer()

        wireMock.verify(postRequestedFor(urlEqualTo("/diagnosiskeys/upload")))

        expectThat(payload).withCaptured { hasSize(1) }
    }

    @Test
    fun `verify diagnosis keys upload updates time in database`() {
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

        val interopClient = InteropClient(wireMock)
        val submissionRepository = FakeSubmissionRepository(listOf(now))
        val service = DiagnosisKeysUploadService(
            interopClient = interopClient,
            submissionRepository = submissionRepository,
            batchTagService = batchTagService
        )

        service.loadKeysAndUploadToFederatedServer()

        verify { batchTagService.updateLastUploadState(any()) }

        wireMock.verify(postRequestedFor(urlEqualTo("/diagnosiskeys/upload")))
    }

    @Test
    fun `stop the upload loop if the remaining time is not sufficient`() {
        wireMock.stubFor(
            post("/diagnosiskeys/upload")
                .withHeader("Authorization", equalTo("Bearer DUMMY_TOKEN"))
                .withRequestBody(UploadPayloadPattern())
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withBody(
                            """{ "batchTag": "75b326f7-ae6f-42f6-9354-00c0a6b797b3", "insertedExposures": 1 } """
                        )
                )
        )

        val lastModifiedDateBatchOne = now.minusSeconds(4)
        val lastModifiedDateBatchTwo = now

        val interopClient = InteropClient(wireMock)
        val submissionRepository = SubmissionFromS3Repository(
            FakeInteropDiagnosisKeysS3(
                S3ObjectSummary("mobile/LAB_RESULT/foo", lastModified = lastModifiedDateBatchOne),
                S3ObjectSummary("mobile/LAB_RESULT/bar", lastModified = lastModifiedDateBatchOne),
                S3ObjectSummary("mobile/LAB_RESULT/abc", lastModified = lastModifiedDateBatchOne),
                S3ObjectSummary("mobile/LAB_RESULT/def", lastModified = lastModifiedDateBatchTwo)
            )
        )

        val service = DiagnosisKeysUploadService(
            interopClient = interopClient,
            submissionRepository = submissionRepository,
            maxUploadBatchSize = 5,
            maxSubsequentBatchUploadCount = 2,
            context = mockk { every { remainingTimeInMillis } returns -2 }
        )

        val submissionCount = service.loadKeysAndUploadToFederatedServer()

        expectThat(submissionCount).describedAs("submission count").isEqualTo(3)
        wireMock.verify(postRequestedFor(urlEqualTo("/diagnosiskeys/upload")))
    }

    @Test
    fun `continue uploading the keys if we have enough time to execute the next batch`() {
        wireMock.stubFor(
            post("/diagnosiskeys/upload")
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withBody("""{ "batchTag": "75b326f7-ae6f-42f6-9354-00c0a6b797b3", "insertedExposures": 1 }""")
                )
        )

        val lastModifiedDate = now.minusSeconds(4)

        val interopClient = InteropClient(wireMock)

        val submissionRepository = SubmissionFromS3Repository(
            FakeInteropDiagnosisKeysS3(
                S3ObjectSummary("mobile/LAB_RESULT/foo", lastModified = lastModifiedDate),
                S3ObjectSummary("mobile/LAB_RESULT/bar", lastModified = lastModifiedDate),
                S3ObjectSummary("mobile/LAB_RESULT/abc", lastModified = lastModifiedDate),
                S3ObjectSummary("mobile/LAB_RESULT/def", lastModified = lastModifiedDate)
            )
        )

        val service = DiagnosisKeysUploadService(
            interopClient = interopClient,
            submissionRepository = submissionRepository,
            maxUploadBatchSize = 5,
            maxSubsequentBatchUploadCount = 2,
        )

        val submissionCount = service.loadKeysAndUploadToFederatedServer()

        expectThat(submissionCount).describedAs("submission count").isEqualTo(4)
        wireMock.verify(postRequestedFor(urlEqualTo("/diagnosiskeys/upload")))
    }

    private class UploadPayloadPattern(@JsonProperty matchesPayloadPattern: String = """{ batchTag: [a-f0-9\-]+, payload: "DUMMY_SIGNATURE" }""") :
        StringValuePattern(matchesPayloadPattern) {
        override fun match(value: String): MatchResult = readJsonOrNull<DiagnosisKeysUploadRequest>(value)
            ?.let {
                when {
                    it.batchTag.value.matches(Regex("[a-f0-9\\-]+")) && it.payload == "DUMMY_SIGNATURE" -> exactMatch()
                    else -> noMatch()
                }
            } ?: noMatch()
    }

    private fun DiagnosisKeysUploadService(
        interopClient: InteropClient,
        submissionRepository: SubmissionRepository,
        batchTagService: BatchTagService = InMemoryBatchTagService(),
        exposureUploadFactory: FederatedExposureUploadFactory = FederatedExposureUploadFactory("GB-EAW"),
        uploadRiskLevelDefaultEnabled: Boolean = false,
        uploadRiskLevelDefault: Int = -1,
        maxUploadBatchSize: Int = 0,
        maxSubsequentBatchUploadCount: Int = 100,
        context: Context = mockk { every { remainingTimeInMillis } returns 1000000 }
    ) = DiagnosisKeysUploadService(
        clock = { now },
        interopClient = interopClient,
        submissionRepository = submissionRepository,
        batchTagService = batchTagService,
        exposureUploadFactory = exposureUploadFactory,
        uploadRiskLevelDefaultEnabled = uploadRiskLevelDefaultEnabled,
        uploadRiskLevelDefault = uploadRiskLevelDefault,
        initialUploadHistoryDays = 14,
        maxUploadBatchSize = maxUploadBatchSize,
        maxSubsequentBatchUploadCount = maxSubsequentBatchUploadCount,
        context = context,
        events = events
    )

    private fun InteropClient(wireMock: WireMockServer) = InteropClient(
        interopBaseUrl = wireMock.baseUrl(),
        authToken = "DUMMY_TOKEN",
        jws = mockk { every { sign(any()) } returns "DUMMY_SIGNATURE" },
        events = events
    )

    private fun FakeInteropDiagnosisKeysS3(vararg summaries: S3ObjectSummary) =
        FakeInteropDiagnosisKeysS3(summaries.toList())

    private fun SubmissionFromS3Repository(
        awsS3: AwsS3,
        keyFilter: (ObjectKey) -> Boolean = { true }
    ) = SubmissionFromS3Repository(
        awsS3 = awsS3,
        objectKeyFilter = keyFilter,
        submissionBucketName = BucketName.of("SUBMISSION_BUCKET"),
        events = events,
        clock = SystemClock.CLOCK
    )
}
