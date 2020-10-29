package uk.nhs.nhsx.keyfederation

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit.WireMockRule
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.hasSize
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import uk.nhs.nhsx.keyfederation.download.DiagnosisKeysDownloadResponse
import uk.nhs.nhsx.keyfederation.upload.JWS
import java.time.LocalDate

class InteropClientTest {

    @Rule
    @JvmField
    val wireMockRule = WireMockRule(wireMockConfig().dynamicPort())

    private val jws = Mockito.mock(JWS::class.java)

    @Ignore
    @Test
    fun `download diagnosis keys and handle missing 204`() {
        wireMockRule.stubFor(WireMock.get("/diagnosiskeys/download/2020-08-19")
            .willReturn(
                WireMock.aResponse()
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
                    """.trimIndent())
            )
        )

        val service = InteropClient(wireMockRule.baseUrl(), "DUMMY_TOKEN", jws)
        val downloadKeysResponses: List<DiagnosisKeysDownloadResponse> = service.downloadKeys(LocalDate.of(2020, 8, 19), null)

        assertThat(downloadKeysResponses.size, equalTo(1))
        assertThat(downloadKeysResponses[0].batchTag, equalTo("75b326f7-ae6f-42f6-9354-00c0a6b797b3"))
        assertThat(downloadKeysResponses[0].exposures, hasSize(2))

        assertThat(wireMockRule.findAllUnmatchedRequests(), hasSize(0))
    }

    @Test
    fun `download diagnosis keys starting from date without batch tag`() {
        wireMockRule.stubFor(WireMock.get("/diagnosiskeys/download/2020-08-19")
            .willReturn(
                WireMock.aResponse()
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
                    """.trimIndent())
            )
        )

        wireMockRule.stubFor(WireMock.get("/diagnosiskeys/download/2020-08-19?batchTag=75b326f7-ae6f-42f6-9354-00c0a6b797b3")
            .willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json; charset=utf-8")
                    .withBody("""
                        {
                            "batchTag": "80e77dc6-8c27-42fb-8e38-1a0b1f51bf01",
                            "exposures": [
                                {
                                    "keyData": "xnGNbiVKd7xarkv9Gbdi5w==",
                                    "rollingStartNumber": 2662992,
                                    "transmissionRiskLevel": 0,
                                    "rollingPeriod": 144,
                                    "regions": [
                                        "GB"
                                    ]
                                },
                                {
                                    "keyData": "ui0wpyxH4QaeIo9f6A6f7A==",
                                    "rollingStartNumber": 2662992,
                                    "transmissionRiskLevel": 0,
                                    "rollingPeriod": 144,
                                    "regions": [
                                        "GB"
                                    ]
                                },
                                {
                                    "keyData": "MLSUh0NsJG/XIExJQJiqkg==",
                                    "rollingStartNumber": 2662992,
                                    "transmissionRiskLevel": 0,
                                    "rollingPeriod": 144,
                                    "regions": [
                                        "GB"
                                    ]
                                },
                                {
                                    "keyData": "z5Zb+aa7ROvPg0ldP0z+GQ==",
                                    "rollingStartNumber": 2662992,
                                    "transmissionRiskLevel": 0,
                                    "rollingPeriod": 144,
                                    "regions": [
                                        "GB"
                                    ]
                                },
                                {
                                    "keyData": "hlFVyF6l+7eMrmIUknRYeg==",
                                    "rollingStartNumber": 2662848,
                                    "transmissionRiskLevel": 0,
                                    "rollingPeriod": 144,
                                    "regions": [
                                        "IE"
                                    ]
                                }
                            ]
                        }
                    """.trimIndent())
            ))

        wireMockRule.stubFor(WireMock.get("/diagnosiskeys/download/2020-08-19?batchTag=80e77dc6-8c27-42fb-8e38-1a0b1f51bf01")
            .willReturn(
                WireMock.aResponse()
                    .withStatus(204)
                    .withHeader("Content-Type", "application/json; charset=utf-8")
                    .withBody(""))
        )

        val service = InteropClient(wireMockRule.baseUrl(), "DUMMY_TOKEN", jws)
        val downloadKeysResponses: List<DiagnosisKeysDownloadResponse> = service.downloadKeys(LocalDate.of(2020, 8, 19), null)

        assertThat(downloadKeysResponses.size, equalTo(2))
        assertThat(downloadKeysResponses[0].batchTag, equalTo("75b326f7-ae6f-42f6-9354-00c0a6b797b3"))
        assertThat(downloadKeysResponses[1].batchTag, equalTo("80e77dc6-8c27-42fb-8e38-1a0b1f51bf01"))
        assertThat(downloadKeysResponses[0].exposures, hasSize(2))
        assertThat(downloadKeysResponses[1].exposures, hasSize(5))

        assertThat(wireMockRule.findAllUnmatchedRequests(), hasSize(0))
    }

    @Test
    fun `download diagnosis keys starting from batch tag`() {
        wireMockRule.stubFor(WireMock.get("/diagnosiskeys/download/2020-08-19?batchTag=nansi6f7-ae6f-42f6-9354-00c0a6b79728")
            .willReturn(
                WireMock.aResponse()
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
                    """.trimIndent())
            )
        )

        wireMockRule.stubFor(WireMock.get("/diagnosiskeys/download/2020-08-19?batchTag=75b326f7-ae6f-42f6-9354-00c0a6b797b3")
            .willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json; charset=utf-8")
                    .withBody("""
                        {
                            "batchTag": "80e77dc6-8c27-42fb-8e38-1a0b1f51bf01",
                            "exposures": [
                                {
                                    "keyData": "xnGNbiVKd7xarkv9Gbdi5w==",
                                    "rollingStartNumber": 2662992,
                                    "transmissionRiskLevel": 0,
                                    "rollingPeriod": 144,
                                    "regions": [
                                        "GB"
                                    ]
                                }
                            ]
                        }
                    """.trimIndent())
            ))

        wireMockRule.stubFor(WireMock.get("/diagnosiskeys/download/2020-08-19?batchTag=80e77dc6-8c27-42fb-8e38-1a0b1f51bf01")
            .willReturn(
                WireMock.aResponse()
                    .withStatus(204)
                    .withHeader("Content-Type", "application/json; charset=utf-8")
                    .withBody(""))
        )

        val service = InteropClient(wireMockRule.baseUrl(), "DUMMY_TOKEN", jws)
        val batchTag = BatchTag.of("nansi6f7-ae6f-42f6-9354-00c0a6b79728")
        val downloadKeysResponses: List<DiagnosisKeysDownloadResponse> = service.downloadKeys(LocalDate.of(2020, 8, 19), batchTag)

        assertThat(downloadKeysResponses.size, equalTo(2))
        assertThat(downloadKeysResponses[0].batchTag, equalTo("75b326f7-ae6f-42f6-9354-00c0a6b797b3"))
        assertThat(downloadKeysResponses[1].batchTag, equalTo("80e77dc6-8c27-42fb-8e38-1a0b1f51bf01"))
        assertThat(downloadKeysResponses[0].exposures, hasSize(2))
        assertThat(downloadKeysResponses[1].exposures, hasSize(1))

        assertThat(wireMockRule.findAllUnmatchedRequests(), hasSize(0))
    }

    @Test
    fun `download diagnosis keys and receive zero content`() {
        wireMockRule.stubFor(WireMock.get("/diagnosiskeys/download/2020-08-19")
            .willReturn(
                WireMock.aResponse()
                    .withStatus(204)
                    .withHeader("Content-Type", "application/json; charset=utf-8")
                    .withBody(""))
        )

        val service = InteropClient(wireMockRule.baseUrl(), "DUMMY_TOKEN", jws)
        val downloadKeysResponses: List<DiagnosisKeysDownloadResponse> = service.downloadKeys(LocalDate.of(2020, 8, 19), null)

        assertThat(downloadKeysResponses.size, equalTo(0))

        assertThat(wireMockRule.findAllUnmatchedRequests(), hasSize(0))
    }

    @Test
    fun `download diagnosis keys and handle server error`() {
        wireMockRule.stubFor(WireMock.get("/diagnosiskeys/download/2020-08-19")
            .willReturn(
                WireMock.aResponse()
                    .withStatus(500)
                    .withHeader("Content-Type", "application/json; charset=utf-8")
                    .withBody(""))
        )

        val service = InteropClient(wireMockRule.baseUrl(), "DUMMY_TOKEN", jws)
        val downloadKeysResponses: List<DiagnosisKeysDownloadResponse> = service.downloadKeys(LocalDate.of(2020, 8, 19), null)

        assertThat(downloadKeysResponses.size, equalTo(0))

        assertThat(wireMockRule.findAllUnmatchedRequests(), hasSize(0))
    }

    @Test
    fun `download diagnosis keys and receive extra unknown payload fields`() {
        wireMockRule.stubFor(WireMock.get("/diagnosiskeys/download/2020-08-19")
            .willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json; charset=utf-8")
                    .withBody("""
                        {
                            "batchTag": "80e77dc6-8c27-42fb-8e38-1a0b1f51bf01",
                            "exposures": [
                                {
                                    "keyData": "xnGNbiVKd7xarkv9Gbdi5w==",
                                    "rollingStartNumber": 2662992,
                                    "transmissionRiskLevel": 0,
                                    "rollingPeriod": 144,
                                    "origin": "JE",
                                    "regions": [
                                        "GB"
                                    ]
                                }
                            ]
                        }
                    """.trimIndent()))
        )

        wireMockRule.stubFor(WireMock.get("/diagnosiskeys/download/2020-08-19?batchTag=80e77dc6-8c27-42fb-8e38-1a0b1f51bf01")
            .willReturn(
                WireMock.aResponse()
                    .withStatus(204)
                    .withHeader("Content-Type", "application/json; charset=utf-8")
                    .withBody(""))
        )

        val service = InteropClient(wireMockRule.baseUrl(), "DUMMY_TOKEN", jws)
        val downloadKeysResponses = service.downloadKeys(LocalDate.of(2020, 8, 19), null)

        assertThat(downloadKeysResponses.size, equalTo(1))
        val actualResponse = downloadKeysResponses[0]
        assertThat(actualResponse.batchTag, equalTo("80e77dc6-8c27-42fb-8e38-1a0b1f51bf01"))
        assertThat(actualResponse.exposures[0].keyData, equalTo("xnGNbiVKd7xarkv9Gbdi5w=="))
        assertThat(actualResponse.exposures[0].rollingStartNumber, equalTo(2662992))
        assertThat(actualResponse.exposures[0].transmissionRiskLevel, equalTo(0))
        assertThat(actualResponse.exposures[0].rollingPeriod, equalTo(144))
        assertThat(actualResponse.exposures[0].regions, equalTo(listOf("GB")))

        assertThat(wireMockRule.findAllUnmatchedRequests(), hasSize(0))
    }
}