package uk.nhs.nhsx.keyfederation

import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.s3.model.S3ObjectSummary
import com.amazonaws.xray.strategy.ContextMissingStrategy
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.type.TypeReference
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.matching.MatchResult
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import uk.nhs.nhsx.core.Jackson
import uk.nhs.nhsx.core.SystemObjectMapper
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.testhelper.mocks.FakeDiagnosisKeysS3
import uk.nhs.nhsx.diagnosiskeydist.s3.SubmissionFromS3Repository
import uk.nhs.nhsx.keyfederation.upload.DiagnosisKeysUploadRequest
import uk.nhs.nhsx.keyfederation.upload.DiagnosisKeysUploadService
import uk.nhs.nhsx.keyfederation.upload.ExposureUpload
import uk.nhs.nhsx.keyfederation.upload.JWS
import uk.nhs.nhsx.testhelper.mocks.FakeSubmissionRepository
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.LinkedHashMap
import kotlin.collections.set

class DiagnosisKeysUploadServiceTest {

    private val wireMockRule: WireMockServer = WireMockServer(0)

    @BeforeEach
    fun start() = wireMockRule.start()

    @AfterEach
    fun stop() = wireMockRule.stop()


    private val jws = Mockito.mock(JWS::class.java)

    private val context = Mockito.mock(Context::class.java)

    @Test
    fun testUpdateRiskLevelIfDefaultEnabled() {
        val service = DiagnosisKeysUploadService(
            null,
            null,
            null,
            null,
            true, 2,
            14, 0,
            100,
            null
        )
        val transformed = service.updateRiskLevelIfDefaultEnabled(ExposureUpload("key",
            0,
            4,
            144,
            null))
        assertEquals(2, transformed.transmissionRiskLevel)
    }

    @Test
    fun `upload diagnosis keys`() {
        System.setProperty(ContextMissingStrategy.CONTEXT_MISSING_STRATEGY_SYSTEM_PROPERTY_OVERRIDE_KEY, "LOG_ERROR")
        wireMockRule.stubFor(post("/diagnosiskeys/upload")
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withBody("""
                        {
                            "batchTag": "75b326f7-ae6f-42f6-9354-00c0a6b797b3",
                            "insertedExposures":0
                        }
                    """.trimIndent())
            ))
        Mockito.`when`(jws.sign(Mockito.anyString())).thenReturn("DUMMY_SIGNATURE")

        val service = DiagnosisKeysUploadService(
            InteropClient(wireMockRule.baseUrl(), "DUMMY_TOKEN", jws),
            FakeSubmissionRepository(listOf(Date.from(OffsetDateTime.now(ZoneOffset.UTC).toInstant()))),
            InMemoryBatchTagService(),
            "GB-EAW",
            false, -1,
            14, 0,
            100,
            null
        )
        service.loadKeysAndUploadToFederatedServer()

        wireMockRule.verify(
            postRequestedFor(urlEqualTo("/diagnosiskeys/upload"))
                .withHeader("Authorization", equalTo("Bearer" + " DUMMY_TOKEN")) // string split on purpose
                .withRequestBody(UploadPayloadPattern())
        )
    }

    @Test
    fun `no filter for federation diagnosis keys and upload`() {
        System.setProperty(ContextMissingStrategy.CONTEXT_MISSING_STRATEGY_SYSTEM_PROPERTY_OVERRIDE_KEY, "LOG_ERROR")
        wireMockRule.stubFor(post("/diagnosiskeys/upload")
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withBody("""
                        {
                            "batchTag": "75b326f7-ae6f-42f6-9354-00c0a6b797b3",
                            "insertedExposures":0
                        }
                    """.trimIndent())
            ))

        Mockito.`when`(jws.sign(Mockito.anyString())).thenReturn("DUMMY_SIGNATURE")

        val spyInteropClient = Mockito.spy(InteropClient(wireMockRule.baseUrl(), "DUMMY_TOKEN", jws))

        val service = DiagnosisKeysUploadService(
            spyInteropClient,
            SubmissionFromS3Repository(
                FakeDiagnosisKeysS3(
                    listOf(
                        S3ObjectSummary().apply {
                            key = "foo"
                            lastModified = Date.from(OffsetDateTime.now(ZoneOffset.UTC).toInstant())
                        },
                        S3ObjectSummary().apply {
                            key = "bar"
                            lastModified = Date.from(OffsetDateTime.now(ZoneOffset.UTC).toInstant())
                        }
                    )),
                { true },
                BucketName.of("SUBMISSION_BUCKET")
            ),
            InMemoryBatchTagService(),
            "GB-EAW",
            false, -1,
            14, 0,
            100,
             null
        )
        service.loadKeysAndUploadToFederatedServer()

        val captor = ArgumentCaptor.forClass(String::class.java)
        Mockito.verify(spyInteropClient).uploadKeys(captor.capture())

        val exposuresUploaded = SystemObjectMapper.MAPPER.readValue(captor.value, object : TypeReference<List<ExposureUpload>>() {})

        assertThat(exposuresUploaded).hasSize(2)

        wireMockRule.verify(
            postRequestedFor(urlEqualTo("/diagnosiskeys/upload"))
                .withHeader("Authorization", equalTo("Bearer" + " DUMMY_TOKEN")) // string split on purpose
                .withRequestBody(UploadPayloadPattern())
        )
    }

    @Test
    fun `filter prefix for federation diagnosis keys and upload`() {
        System.setProperty(ContextMissingStrategy.CONTEXT_MISSING_STRATEGY_SYSTEM_PROPERTY_OVERRIDE_KEY, "LOG_ERROR")
        wireMockRule.stubFor(post("/diagnosiskeys/upload")
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withBody("""
                        {
                            "batchTag": "75b326f7-ae6f-42f6-9354-00c0a6b797b3",
                            "insertedExposures":0
                        }
                    """.trimIndent())
            ))

        Mockito.`when`(jws.sign(Mockito.anyString())).thenReturn("DUMMY_SIGNATURE")

        val spyInteropClient = Mockito.spy(InteropClient(wireMockRule.baseUrl(), "DUMMY_TOKEN", jws))

        val service = DiagnosisKeysUploadService(
            spyInteropClient,
            SubmissionFromS3Repository(FakeDiagnosisKeysS3(listOf(
                S3ObjectSummary().apply {
                    key = "prefix-foo"
                    lastModified = Date.from(OffsetDateTime.now(ZoneOffset.UTC).toInstant())
                },
                S3ObjectSummary().apply {
                    key = "bar"
                    lastModified = Date.from(OffsetDateTime.now(ZoneOffset.UTC).toInstant())
                }
            )), { objectKey -> !objectKey.value.startsWith("prefix") }, BucketName.of("SUBMISSION_BUCKET")
            ),
            InMemoryBatchTagService(),
            "GB-EAW",
            false, -1,
            14, 0,
            100,
            null
        )
        service.loadKeysAndUploadToFederatedServer()

        val captor = ArgumentCaptor.forClass(String::class.java)
        Mockito.verify(spyInteropClient).uploadKeys(captor.capture())

        val exposuresUploaded = SystemObjectMapper.MAPPER.readValue(captor.value, object : TypeReference<List<ExposureUpload>>() {})

        assertThat(exposuresUploaded).hasSize(1)

        wireMockRule.verify(
            postRequestedFor(urlEqualTo("/diagnosiskeys/upload"))
                .withHeader("Authorization", equalTo("Bearer" + " DUMMY_TOKEN")) // string split on purpose
                .withRequestBody(UploadPayloadPattern())
        )
    }

    @Test
    fun `verify diagnosis keys upload updates time in database`() {
        System.setProperty(ContextMissingStrategy.CONTEXT_MISSING_STRATEGY_SYSTEM_PROPERTY_OVERRIDE_KEY, "LOG_ERROR")
        wireMockRule.stubFor(post("/diagnosiskeys/upload")
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withBody("""
                        {
                            "batchTag": "75b326f7-ae6f-42f6-9354-00c0a6b797b3",
                            "insertedExposures":0
                        }
                    """.trimIndent())
            ))

        Mockito.`when`(jws.sign(Mockito.anyString())).thenReturn("DUMMY_SIGNATURE")

        val batchTagService = Mockito.spy(InMemoryBatchTagService())
        val service = DiagnosisKeysUploadService(
            InteropClient(wireMockRule.baseUrl(), "DUMMY_TOKEN", jws),
            FakeSubmissionRepository(listOf(Date.from(OffsetDateTime.now(ZoneOffset.UTC).toInstant()))),
            batchTagService,
            "GB-EAW",
            false, -1,
            14, 0,
            100,
             null
        )
        service.loadKeysAndUploadToFederatedServer()

        Mockito.verify(batchTagService).updateLastUploadState(Mockito.anyLong())
    }

    @Test
    fun `stop the upload loop if the remaining time is not sufficient`() {
        wireMockRule.stubFor(post("/diagnosiskeys/upload")
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withBody("""
                        {
                            "batchTag": "75b326f7-ae6f-42f6-9354-00c0a6b797b3",
                            "insertedExposures":1
                        }
                    """.trimIndent())
            ))

        Mockito.`when`(jws.sign(Mockito.anyString())).thenReturn("DUMMY_SIGNATURE")
        Mockito.`when`(context.remainingTimeInMillis).thenReturn(-2)

        val spyInteropClient = Mockito.spy(InteropClient(wireMockRule.baseUrl(), "DUMMY_TOKEN", jws))
        val lastModifiedDateBatchOne = Date.from(OffsetDateTime.now(ZoneOffset.UTC).toInstant().minusSeconds(4))
        val lastModifiedDateBatchTwo = Date.from(OffsetDateTime.now(ZoneOffset.UTC).toInstant())

        val service = DiagnosisKeysUploadService(
            spyInteropClient,
            SubmissionFromS3Repository(
                FakeDiagnosisKeysS3(
                    listOf(
                        S3ObjectSummary().apply {
                            key = "foo"
                            lastModified = lastModifiedDateBatchOne
                        },
                        S3ObjectSummary().apply {
                            key = "bar"
                            lastModified = lastModifiedDateBatchOne
                        },
                        S3ObjectSummary().apply {
                            key = "abc"
                            lastModified = lastModifiedDateBatchOne
                        },
                        S3ObjectSummary().apply {
                            key = "def"
                            lastModified = lastModifiedDateBatchTwo
                        }
                    )),
                { true },
                BucketName.of("SUBMISSION_BUCKET")
            ),
            InMemoryBatchTagService(),
            "GB-EAW",
            false, -1,
            14, 5,
            2,
            context
        )
        val exposuresUploaded = service.loadKeysAndUploadToFederatedServer()
        assertThat(exposuresUploaded).isEqualTo(3)

    }


    fun `continue uploading the keys if we have enough time to execute the next batch`() {
        wireMockRule.stubFor(post("/diagnosiskeys/upload")
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withBody("""
                        {
                            "batchTag": "75b326f7-ae6f-42f6-9354-00c0a6b797b3",
                            "insertedExposures":1
                        }
                    """.trimIndent())
            ))

        Mockito.`when`(jws.sign(Mockito.anyString())).thenReturn("DUMMY_SIGNATURE")
        Mockito.`when`(context.remainingTimeInMillis).thenReturn(1000000)

        val spyInteropClient = Mockito.spy(InteropClient(wireMockRule.baseUrl(), "DUMMY_TOKEN", jws))

        val service = DiagnosisKeysUploadService(
            spyInteropClient,
            SubmissionFromS3Repository(
                FakeDiagnosisKeysS3(
                    listOf(
                        S3ObjectSummary().apply {
                            key = "foo"
                            lastModified = Date.from(OffsetDateTime.now(ZoneOffset.UTC).toInstant().minusSeconds(4))
                        },
                        S3ObjectSummary().apply {
                            key = "bar"
                            lastModified = Date.from(OffsetDateTime.now(ZoneOffset.UTC).toInstant().minusSeconds(4))
                        },
                        S3ObjectSummary().apply {
                            key = "abc"
                            lastModified = Date.from(OffsetDateTime.now(ZoneOffset.UTC).toInstant().minusSeconds(4))
                        },
                        S3ObjectSummary().apply {
                            key = "def"
                            lastModified = Date.from(OffsetDateTime.now(ZoneOffset.UTC).toInstant())
                        }
                    )),
                { true },
                BucketName.of("SUBMISSION_BUCKET")
            ),
            InMemoryBatchTagService(),
            "GB-EAW",
            false, -1,
            14, 5,
            2,
            context
        )
        val exposuresUploaded = service.loadKeysAndUploadToFederatedServer()
        assertThat(exposuresUploaded).isEqualTo(4)

    }

    class UploadPayloadPattern(@JsonProperty matchesPayloadPattern: String = """{ batchTag: [a-f0-9\-]+, payload: "DUMMY_SIGNATURE" }""") : StringValuePattern(matchesPayloadPattern) {
        override fun match(value: String?): MatchResult {
            return Jackson.deserializeMaybe(value, DiagnosisKeysUploadRequest::class.java).map {
                if (it.batchTag.matches(Regex("[a-f0-9\\-]+")) && it.payload == "DUMMY_SIGNATURE") {
                    MatchResult.exactMatch()
                } else {
                    MatchResult.noMatch()
                }
            }.orElse(
                MatchResult.noMatch()
            )
        }

    }

    private fun scanResult(): List<MutableMap<String, AttributeValue>> {
        val currentUtcTime: String = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ"))
        val item: MutableMap<String, AttributeValue> = LinkedHashMap()
        item["id"] = AttributeValue("0")
        item["lastReceivedBatchTag"] = AttributeValue("75b326f7-ae6f-42f6-9354-00c0a6b797b3")
        item["lastUploadState"] = AttributeValue(currentUtcTime)
        return listOf(item)
    }

}

