package uk.nhs.nhsx.keyfederation

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.keyfederation.download.ExposureDownload
import uk.nhs.nhsx.keyfederation.upload.JWS
import uk.nhs.nhsx.testhelper.wiremock.WireMockExtension
import java.time.LocalDate

@ExtendWith(WireMockExtension::class)
class InteropClientTest(private val wireMock: WireMockServer) {

    private val jws = mockk<JWS>()

    private val localDate = LocalDate.of(2020, 8, 19)

    @Test
    fun `download diagnosis keys starting from date without batch tag`() {
        wireMock.stubFor(
            get("/diagnosiskeys/download/2020-08-19")
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
                                        "rollingStartNumber": 2662992,
                                        "transmissionRiskLevel": 0,
                                        "rollingPeriod": 144,
                                        "origin": "JE",
                                        "regions": [
                                            "GB"
                                        ]
                                    },
                                    {
                                        "keyData": "EwoHez3CQgdslvdxaf+ztw==",
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
                            """.trimIndent()
                        )
                )
        )

        wireMock.stubFor(
            get("/diagnosiskeys/download/2020-08-19?batchTag=75b326f7-ae6f-42f6-9354-00c0a6b797b3")
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json; charset=utf-8")
                        .withBody(
                            """
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
                                    },
                                    {
                                        "keyData": "ui0wpyxH4QaeIo9f6A6f7A==",
                                        "rollingStartNumber": 2662992,
                                        "transmissionRiskLevel": 0,
                                        "rollingPeriod": 144,
                                        "origin": "JE",
                                        "regions": [
                                            "GB"
                                        ]
                                    },
                                    {
                                        "keyData": "MLSUh0NsJG/XIExJQJiqkg==",
                                        "rollingStartNumber": 2662992,
                                        "transmissionRiskLevel": 0,
                                        "rollingPeriod": 144,
                                        "origin": "JE",
                                        "regions": [
                                            "GB"
                                        ]
                                    },
                                    {
                                        "keyData": "z5Zb+aa7ROvPg0ldP0z+GQ==",
                                        "rollingStartNumber": 2662992,
                                        "transmissionRiskLevel": 0,
                                        "rollingPeriod": 144,
                                        "origin": "JE",
                                        "regions": [
                                            "GB"
                                        ]
                                    },
                                    {
                                        "keyData": "hlFVyF6l+7eMrmIUknRYeg==",
                                        "rollingStartNumber": 2662848,
                                        "transmissionRiskLevel": 0,
                                        "rollingPeriod": 144,
                                        "origin": "JE",
                                        "regions": [
                                            "IE"
                                        ]
                                    }
                                ]
                            }
                            """.trimIndent()
                        )
                )
        )

        wireMock.stubFor(
            get("/diagnosiskeys/download/2020-08-19?batchTag=80e77dc6-8c27-42fb-8e38-1a0b1f51bf01")
                .willReturn(
                    aResponse()
                        .withStatus(204)
                        .withHeader("Content-Type", "application/json; charset=utf-8")
                        .withBody("")
                )
        )

        val service = InteropClient(wireMock.baseUrl(), "DUMMY_TOKEN", jws, RecordingEvents())

        service.getExposureKeysBatch(localDate, "").apply {
            assertThat(this).get().satisfies {
                assertThat(it.batchTag).isEqualTo("75b326f7-ae6f-42f6-9354-00c0a6b797b3")
                assertThat(it.exposures).hasSize(2)
            }
        }

        service.getExposureKeysBatch(localDate, "?batchTag=75b326f7-ae6f-42f6-9354-00c0a6b797b3").apply {
            assertThat(this).get().satisfies {
                assertThat(it.batchTag).isEqualTo("80e77dc6-8c27-42fb-8e38-1a0b1f51bf01")
                assertThat(it.exposures).hasSize(5)
            }
        }
    }

    @Test
    fun `download diagnosis keys starting from batch tag`() {
        wireMock.stubFor(
            get("/diagnosiskeys/download/2020-08-19?batchTag=nansi6f7-ae6f-42f6-9354-00c0a6b79728")
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
                                    "rollingStartNumber": 2662992,
                                    "transmissionRiskLevel": 0,
                                    "rollingPeriod": 144,
                                    "origin": "JE",
                                    "regions": [
                                        "GB"
                                    ]
                                },
                                {
                                    "keyData": "EwoHez3CQgdslvdxaf+ztw==",
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
                    """.trimIndent()
                        )
                )
        )

        wireMock.stubFor(
            get("/diagnosiskeys/download/2020-08-19?batchTag=75b326f7-ae6f-42f6-9354-00c0a6b797b3")
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json; charset=utf-8")
                        .withBody(
                            """
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
                    """.trimIndent()
                        )
                )
        )

        wireMock.stubFor(
            get("/diagnosiskeys/download/2020-08-19?batchTag=80e77dc6-8c27-42fb-8e38-1a0b1f51bf01")
                .willReturn(
                    aResponse()
                        .withStatus(204)
                        .withHeader("Content-Type", "application/json; charset=utf-8")
                        .withBody("")
                )
        )

        val service = InteropClient(wireMock.baseUrl(), "DUMMY_TOKEN", jws, RecordingEvents())
        val batchTag = BatchTag.of("nansi6f7-ae6f-42f6-9354-00c0a6b79728")

        service.getExposureKeysBatch(localDate, "?batchTag=${batchTag.value}").apply {
            assertThat(this).get().satisfies {
                assertThat(it.batchTag).isEqualTo("75b326f7-ae6f-42f6-9354-00c0a6b797b3")
                assertThat(it.exposures).hasSize(2)
            }
        }

        service.getExposureKeysBatch(localDate, "?batchTag=75b326f7-ae6f-42f6-9354-00c0a6b797b3").apply {
            assertThat(this).get().satisfies {
                assertThat(it.batchTag).isEqualTo("80e77dc6-8c27-42fb-8e38-1a0b1f51bf01")
                assertThat(it.exposures).hasSize(1)
            }
        }
    }

    @Test
    fun `download diagnosis keys and receive zero content`() {
        wireMock.stubFor(
            get("/diagnosiskeys/download/2020-08-19")
                .willReturn(
                    aResponse()
                        .withStatus(204)
                        .withHeader("Content-Type", "application/json; charset=utf-8")
                        .withBody("")
                )
        )

        val service = InteropClient(wireMock.baseUrl(), "DUMMY_TOKEN", jws, RecordingEvents())

        service.getExposureKeysBatch(localDate, "").apply {
            assertThat(this).isNotPresent
        }
    }

    @Test
    fun `download diagnosis keys and handle server error`() {
        wireMock.stubFor(
            get("/diagnosiskeys/download/2020-08-19")
                .willReturn(
                    aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json; charset=utf-8")
                        .withBody("")
                )
        )

        val service = InteropClient(wireMock.baseUrl(), "DUMMY_TOKEN", jws, RecordingEvents())

        assertThatThrownBy { service.getExposureKeysBatch(localDate, "") }
            .hasMessageEndingWith("failed with status code 500")
    }

    @Test
    fun `download diagnosis keys and receive extra unknown payload fields`() {
        wireMock.stubFor(
            get("/diagnosiskeys/download/2020-08-19")
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json; charset=utf-8")
                        .withBody(
                            """
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
                    """.trimIndent()
                        )
                )
        )

        wireMock.stubFor(
            get("/diagnosiskeys/download/2020-08-19?batchTag=80e77dc6-8c27-42fb-8e38-1a0b1f51bf01")
                .willReturn(
                    aResponse()
                        .withStatus(204)
                        .withHeader("Content-Type", "application/json; charset=utf-8")
                        .withBody("")
                )
        )

        val service = InteropClient(wireMock.baseUrl(), "DUMMY_TOKEN", jws, RecordingEvents())
        val response = service.getExposureKeysBatch(localDate, "")

        assertThat(response).get().satisfies { key ->
            assertThat(key.batchTag).isEqualTo("80e77dc6-8c27-42fb-8e38-1a0b1f51bf01")
            assertThat(key.exposures).first().satisfies {
                assertThat(it).usingRecursiveComparison().isEqualTo(
                    ExposureDownload(
                        "xnGNbiVKd7xarkv9Gbdi5w==",
                        2662992,
                        0,
                        144,
                        "JE",
                        listOf("GB")
                    )
                )
            }
        }
    }
}
