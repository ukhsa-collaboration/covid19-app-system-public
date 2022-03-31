@file:Suppress("TestFunctionName")

package uk.nhs.nhsx.keyfederation

import com.amazonaws.services.lambda.runtime.Context
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
import uk.nhs.nhsx.keyfederation.client.DiagnosisKeysUploadRequest
import uk.nhs.nhsx.keyfederation.client.ExposureUpload
import uk.nhs.nhsx.keyfederation.client.HttpInteropClient
import uk.nhs.nhsx.keyfederation.client.InteropClient
import uk.nhs.nhsx.keyfederation.storage.BatchTagService
import uk.nhs.nhsx.keyfederation.upload.DiagnosisKeysUploadService
import uk.nhs.nhsx.keyfederation.upload.FederatedExposureUploadFactory
import uk.nhs.nhsx.testhelper.assertions.containsExactly
import uk.nhs.nhsx.testhelper.assertions.withCaptured
import uk.nhs.nhsx.testhelper.mocks.FakeS3
import uk.nhs.nhsx.testhelper.mocks.FakeSubmissionRepository
import uk.nhs.nhsx.testhelper.mocks.exposureS3Object
import uk.nhs.nhsx.testhelper.wiremock.WireMockExtension
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset.UTC

@ExtendWith(WireMockExtension::class)
class DiagnosisKeysUploadServiceTest(private val wireMock: WireMockServer) {

    private val events = RecordingEvents()
    private val clock = Clock.fixed(Instant.parse("2021-02-02T11:13:00.000Z"), UTC)
    private val now = Instant.now(clock)
    private val submissionBucketName = BucketName.of("SUBMISSION_BUCKET")
    private val fakeS3 = FakeS3()

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

        fakeS3.add(exposureS3Object("mobile/LAB_RESULT/abc", submissionBucketName, "3/TzKOK2u0O/eHeK4R0VSg=="), now)
        fakeS3.add(exposureS3Object("mobile/RAPID_RESULT/def", submissionBucketName, "3/TzKOK2u0O/eHeK4R0VSg=="), now)

        val submissionRepository = SubmissionFromS3Repository(fakeS3)

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

        fakeS3.add(
            exposureS3Object("mobile/RAPID_RESULT/abc.json", submissionBucketName, "3/TzKOK2u0O/eHeK4R0VSg=="),
            now
        )
        fakeS3.add(exposureS3Object("bar", submissionBucketName, "3/TzKOK2u0O/eHeK4R0VSg=="), now)

        val submissionRepository = SubmissionFromS3Repository(fakeS3) { it.value.startsWith("mobile") }

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

        fakeS3.add(
            exposureS3Object("mobile/LAB_RESULT/foo", submissionBucketName, "3/TzKOK2u0O/eHeK4R0VSg=="),
            lastModifiedDateBatchOne
        )
        fakeS3.add(
            exposureS3Object("mobile/LAB_RESULT/bar", submissionBucketName, "3/TzKOK2u0O/eHeK4R0VSg=="),
            lastModifiedDateBatchOne
        )
        fakeS3.add(
            exposureS3Object("mobile/LAB_RESULT/abc", submissionBucketName, "3/TzKOK2u0O/eHeK4R0VSg=="),
            lastModifiedDateBatchOne
        )
        fakeS3.add(
            exposureS3Object("mobile/LAB_RESULT/def", submissionBucketName, "3/TzKOK2u0O/eHeK4R0VSg=="),
            lastModifiedDateBatchTwo
        )

        val interopClient = InteropClient(wireMock)
        val submissionRepository = SubmissionFromS3Repository(fakeS3)

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

        fakeS3.add(
            exposureS3Object("mobile/LAB_RESULT/foo", submissionBucketName, "3/TzKOK2u0O/eHeK4R0VSg=="),
            lastModifiedDate
        )
        fakeS3.add(
            exposureS3Object("mobile/LAB_RESULT/bar", submissionBucketName, "3/TzKOK2u0O/eHeK4R0VSg=="),
            lastModifiedDate
        )
        fakeS3.add(
            exposureS3Object("mobile/LAB_RESULT/abc", submissionBucketName, "3/TzKOK2u0O/eHeK4R0VSg=="),
            lastModifiedDate
        )
        fakeS3.add(
            exposureS3Object("mobile/LAB_RESULT/def", submissionBucketName, "3/TzKOK2u0O/eHeK4R0VSg=="),
            lastModifiedDate
        )

        val submissionRepository = SubmissionFromS3Repository(fakeS3)

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
                    it.batchTag.value.matches(Regex("[a-f\\d\\-]+")) && it.payload == "DUMMY_SIGNATURE" -> exactMatch()
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

    private fun InteropClient(wireMock: WireMockServer) = HttpInteropClient(
        interopBaseUrl = wireMock.baseUrl(),
        authToken = "DUMMY_TOKEN",
        jws = mockk { every { sign(any()) } returns "DUMMY_SIGNATURE" },
        events = events
    )

    private fun SubmissionFromS3Repository(
        awsS3: AwsS3,
        keyFilter: (ObjectKey) -> Boolean = { true }
    ) = SubmissionFromS3Repository(
        awsS3 = awsS3,
        objectKeyFilter = keyFilter,
        submissionBucketName = submissionBucketName,
        loadSubmissionsTimeout = Duration.ofMinutes(12),
        loadSubmissionsThreadPoolSize = 15,
        events = events,
        clock = SystemClock.CLOCK
    )
}
