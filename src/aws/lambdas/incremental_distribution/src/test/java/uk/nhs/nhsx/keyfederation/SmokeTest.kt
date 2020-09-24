package uk.nhs.nhsx.keyfederation

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit.WireMockRule
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.core.Jackson
import uk.nhs.nhsx.core.aws.s3.AwsS3Client
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.secretsmanager.AwsSecretManager
import uk.nhs.nhsx.core.aws.secretsmanager.SecretManager
import uk.nhs.nhsx.core.aws.secretsmanager.SecretName
import uk.nhs.nhsx.core.aws.xray.Tracing
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKeyPayload
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.function.Supplier

@Ignore
class SmokeTest {

    @Rule
    @JvmField
    val wireMockRule = WireMockRule(wireMockConfig().dynamicPort())

    init {
        Tracing.disableXRayComplaintsForMainClasses()
    }

    val s3Client: AmazonS3 by lazy { AmazonS3ClientBuilder.defaultClient() }
    val secretManager: SecretManager by lazy { AwsSecretManager() }
    val awsS3Client: AwsS3Client by lazy { AwsS3Client() }

    companion object {
        const val SUBMISSION_BUCKET_NAME = "dhsc-tt-dev-submission-keys"
    }

    @Before
    fun clearS3Bucket() {
        s3Client.listObjects(SUBMISSION_BUCKET_NAME).objectSummaries.forEach {
            s3Client.deleteObject(it.bucketName, it.key)
        }
    }

    fun getConfig() = KeyFederationConfig(
        true,
        true,
        BucketName.of(SUBMISSION_BUCKET_NAME),
        wireMockRule.baseUrl(),
        SecretName.of("/app/interop/AuthorizationToken"),
        SecretName.of("/app/interop/PrivateKey"),
        "federatedKeyPrefix",
        "DUMMY_TABLE"
    )


    @Test
    fun `download keys with no content does nothing`() {
        wireMockRule.stubFor(
            get("/diagnosiskeys/download/2020-08-01?batchTag=75b326f7-ae6f-42f6-9354-00c0a6b797b3")
                .willReturn(aResponse().withStatus(204))
        )

        val batchTagService = InMemoryBatchTagService(BatchTag.of("75b326f7-ae6f-42f6-9354-00c0a6b797b3"))

        Handler(
            Supplier { LocalDate.of(2020, 8, 15).atStartOfDay().toInstant(ZoneOffset.UTC) },
            getConfig(),
            secretManager,
            awsS3Client
        ).handleRequest(null, null)

        val objectSummaries = s3Client.listObjects(SUBMISSION_BUCKET_NAME).objectSummaries
        assertThat(objectSummaries).isEmpty()
        assertThat(batchTagService.latestBatchTag!!.value).isEqualTo("75b326f7-ae6f-42f6-9354-00c0a6b797b3")
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
                        "rollingStartNumber": 2662992,
                        "transmissionRiskLevel": 0,
                        "rollingPeriod": 144,
                        "regions": [
                            "GB"
                        ]
                    },
                    {
                        "keyData": "EwoHez3CQgdslvdxaf+ztw==",
                        "rollingStartNumber": 2662992,
                        "transmissionRiskLevel": 0,
                        "rollingPeriod": 144,
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
        Handler(
            Supplier { LocalDate.of(2020, 8, 15).atStartOfDay().toInstant(ZoneOffset.UTC) },
            getConfig(),
            secretManager,
            awsS3Client
        ).handleRequest(null, null)

        val objectSummaries = s3Client.listObjects(SUBMISSION_BUCKET_NAME).objectSummaries
        assertThat(objectSummaries).hasSize(1)

        val keys = objectSummaries.flatMap {
            val s3Object = s3Client.getObject(it.bucketName, it.key)
            Jackson.readJson(s3Object.objectContent, StoredTemporaryExposureKeyPayload::class.java).temporaryExposureKeys
        }.map { it.key }

        assertThat(keys).hasSize(2)
        assertThat(keys).containsAll(listOf("ogNW4Ra+Zdds1ShN56yv3w==", "EwoHez3CQgdslvdxaf+ztw=="))

        assertThat(batchTagService.latestBatchTag!!.value).isEqualTo("75b326f7-ae6f-42f6-9354-00c0a6b797b3")
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
        Handler(
            Supplier { LocalDate.of(2020, 8, 15).atStartOfDay().toInstant(ZoneOffset.UTC) },
            getConfig(),
            secretManager,
            awsS3Client
        ).handleRequest(null, null)

        val objectSummaries = s3Client.listObjects(SUBMISSION_BUCKET_NAME).objectSummaries
        assertThat(objectSummaries).isEmpty()
        assertThat(batchTagService.latestBatchTag!!.value).isEqualTo("75b326f7-ae6f-42f6-9354-00c0a6b797b3")
    }

}