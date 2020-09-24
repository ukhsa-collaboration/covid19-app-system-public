package uk.nhs.nhsx.keyfederation

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.ScanResult
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest
import com.amazonaws.services.s3.model.S3ObjectSummary
import com.amazonaws.xray.strategy.ContextMissingStrategy
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.type.TypeReference
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit.WireMockRule
import com.github.tomakehurst.wiremock.matching.MatchResult
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import org.assertj.core.api.Assertions.assertThat
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import uk.nhs.nhsx.core.Jackson
import uk.nhs.nhsx.core.SystemObjectMapper
import uk.nhs.nhsx.diagnosiskeydist.s3.FakeDiagnosisKeysS3
import uk.nhs.nhsx.diagnosiskeydist.s3.SubmissionFromS3Repository
import uk.nhs.nhsx.keyfederation.upload.DiagnosisKeysUploadRequest
import uk.nhs.nhsx.keyfederation.upload.DiagnosisKeysUploadService
import uk.nhs.nhsx.keyfederation.upload.JWS
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

class DiagnosisKeysUploadServiceTest {

    @Rule
    @JvmField
    val wireMockRule = WireMockRule(wireMockConfig().dynamicPort())

    private val jws = Mockito.mock(JWS::class.java)

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
        Mockito.`when`(jws.compactSignedPayload(Mockito.anyString())).thenReturn("DUMMY_SIGNATURE")

        val service = DiagnosisKeysUploadService(
            InteropClient(wireMockRule.baseUrl(), "DUMMY_TOKEN", jws),
            MockSubmissionRepository(listOf(Date.from(OffsetDateTime.now(ZoneOffset.UTC).toInstant()))),
            InMemoryBatchTagService()
        )
        service.uploadRequest()

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

        Mockito.`when`(jws.compactSignedPayload(Mockito.anyString())).thenReturn("DUMMY_SIGNATURE")

        val spyInteropClient = Mockito.spy(InteropClient(wireMockRule.baseUrl(), "DUMMY_TOKEN", jws))

        val service = DiagnosisKeysUploadService(
            spyInteropClient,
            SubmissionFromS3Repository(FakeDiagnosisKeysS3(listOf(
                S3ObjectSummary().apply {
                    key = "foo"
                    lastModified = Date.from(LocalDateTime.now().toInstant(ZoneOffset.UTC))
                },
                S3ObjectSummary().apply {
                    key = "bar"
                    lastModified = Date.from(LocalDateTime.now().toInstant(ZoneOffset.UTC))
                }
            ))),
            InMemoryBatchTagService()
        )
        service.uploadRequest()

        val captor = ArgumentCaptor.forClass(String::class.java)
        Mockito.verify(spyInteropClient).uploadKeys(captor.capture())

        val exposuresUploaded = SystemObjectMapper.MAPPER.readValue(captor.value, object : TypeReference<List<Exposure>>() {})

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

        Mockito.`when`(jws.compactSignedPayload(Mockito.anyString())).thenReturn("DUMMY_SIGNATURE")

        val spyInteropClient = Mockito.spy(InteropClient(wireMockRule.baseUrl(), "DUMMY_TOKEN", jws))

        val service = DiagnosisKeysUploadService(
            spyInteropClient,
            SubmissionFromS3Repository(FakeDiagnosisKeysS3(listOf(
                S3ObjectSummary().apply {
                    key = "prefix-foo"
                    lastModified = Date.from(LocalDateTime.now().toInstant(ZoneOffset.UTC))
                },
                S3ObjectSummary().apply {
                    key = "bar"
                    lastModified = Date.from(LocalDateTime.now().toInstant(ZoneOffset.UTC))
                }
            ))) { objectKey -> !objectKey.startsWith("prefix") },
            InMemoryBatchTagService()
        )
        service.uploadRequest()

        val captor = ArgumentCaptor.forClass(String::class.java)
        Mockito.verify(spyInteropClient).uploadKeys(captor.capture())

        val exposuresUploaded = SystemObjectMapper.MAPPER.readValue(captor.value, object : TypeReference<List<Exposure>>() {})

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

        Mockito.`when`(jws.compactSignedPayload(Mockito.anyString())).thenReturn("DUMMY_SIGNATURE")

        val batchTagService = Mockito.spy(InMemoryBatchTagService())
        val service = DiagnosisKeysUploadService(
            InteropClient(wireMockRule.baseUrl(), "DUMMY_TOKEN", jws),
            MockSubmissionRepository(listOf(Date.from(OffsetDateTime.now(ZoneOffset.UTC).toInstant()))),
            batchTagService
        )
        service.uploadRequest()

        Mockito.verify(batchTagService).updateLastUploadState(Mockito.anyString())
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