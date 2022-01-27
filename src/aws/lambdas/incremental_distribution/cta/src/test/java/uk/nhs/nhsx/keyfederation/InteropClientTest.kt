@file:Suppress("TestFunctionName")

package uk.nhs.nhsx.keyfederation

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.exactly
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.endsWith
import strikt.assertions.first
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.message
import uk.nhs.nhsx.core.UniqueId
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.domain.BatchTag
import uk.nhs.nhsx.domain.ReportType.CONFIRMED_TEST
import uk.nhs.nhsx.domain.TestType.LAB_RESULT
import uk.nhs.nhsx.keyfederation.download.DiagnosisKeysDownloadResponse
import uk.nhs.nhsx.keyfederation.download.ExposureDownload
import uk.nhs.nhsx.keyfederation.download.NoContent
import uk.nhs.nhsx.keyfederation.upload.ExposureUpload
import uk.nhs.nhsx.keyfederation.upload.JWS
import uk.nhs.nhsx.testhelper.wiremock.WireMockExtension
import java.time.LocalDate
import java.util.*
import java.util.function.Supplier

@ExtendWith(WireMockExtension::class)
class InteropClientTest(private val wireMock: WireMockServer) {

    private val events = RecordingEvents()
    private val localDate = LocalDate.of(2020, 8, 19)

    @Test
    fun `download diagnosis keys starting from date without batch tag`() {
        with(wireMock) {
            stubFor(
                get(urlEqualTo("/diagnosiskeys/download/2020-08-19"))
                    .willReturn(
                        aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(interopDownloadPayload)
                    )
            )

            stubFor(
                get(urlPathEqualTo("/diagnosiskeys/download/2020-08-19"))
                    .withQueryParam("batchTag", equalTo("5a0df0cd-4663-4119-ade4-3a30ab8e164d"))
                    .willReturn(
                        aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(interopDownloadPayload2)
                    )
            )
        }

        val client = InteropClient(wireMock)

        expectThat(client.downloadKeys(localDate))
            .isA<DiagnosisKeysDownloadResponse>().and {
                get(DiagnosisKeysDownloadResponse::batchTag).isEqualTo(BatchTag.of("5a0df0cd-4663-4119-ade4-3a30ab8e164d"))
                get(DiagnosisKeysDownloadResponse::exposures).hasSize(7)
            }

        expectThat(client.downloadKeys(localDate, BatchTag.of("5a0df0cd-4663-4119-ade4-3a30ab8e164d")))
            .isA<DiagnosisKeysDownloadResponse>().and {
                get(DiagnosisKeysDownloadResponse::batchTag).isEqualTo(BatchTag.of("a25df527-d674-4972-bd62-ce3ff31417d4"))
                get(DiagnosisKeysDownloadResponse::exposures).hasSize(3)
            }

        with(wireMock) {
            verify(
                exactly(1),
                getRequestedFor(urlEqualTo("/diagnosiskeys/download/2020-08-19"))
            )
            verify(
                exactly(1),
                getRequestedFor(urlPathEqualTo("/diagnosiskeys/download/2020-08-19"))
                    .withQueryParam("batchTag", equalTo("5a0df0cd-4663-4119-ade4-3a30ab8e164d"))
            )
        }
    }

    @Test
    fun `download diagnosis keys starting from batch tag`() {
        with(wireMock) {
            stubFor(
                get(urlPathEqualTo("/diagnosiskeys/download/2020-08-19"))
                    .withQueryParam("batchTag", equalTo("nansi6f7-ae6f-42f6-9354-00c0a6b79728"))
                    .willReturn(
                        aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(interopDownloadPayload)
                    )
            )

            stubFor(
                get(urlPathEqualTo("/diagnosiskeys/download/2020-08-19"))
                    .withQueryParam("batchTag", equalTo("5a0df0cd-4663-4119-ade4-3a30ab8e164d"))
                    .willReturn(
                        aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(interopDownloadPayload2)
                    )
            )
        }

        val client = InteropClient(wireMock)

        expectThat(client.downloadKeys(localDate, BatchTag.of("nansi6f7-ae6f-42f6-9354-00c0a6b79728")))
            .isA<DiagnosisKeysDownloadResponse>().and {
                get(DiagnosisKeysDownloadResponse::batchTag).isEqualTo(BatchTag.of("5a0df0cd-4663-4119-ade4-3a30ab8e164d"))
                get(DiagnosisKeysDownloadResponse::exposures).hasSize(7)
            }

        expectThat(client.downloadKeys(localDate, BatchTag.of("5a0df0cd-4663-4119-ade4-3a30ab8e164d")))
            .isA<DiagnosisKeysDownloadResponse>().and {
                get(DiagnosisKeysDownloadResponse::batchTag).isEqualTo(BatchTag.of("a25df527-d674-4972-bd62-ce3ff31417d4"))
                get(DiagnosisKeysDownloadResponse::exposures).hasSize(3)
            }

        with(wireMock) {
            verify(
                exactly(1), getRequestedFor(urlPathEqualTo("/diagnosiskeys/download/2020-08-19"))
                    .withQueryParam("batchTag", equalTo("nansi6f7-ae6f-42f6-9354-00c0a6b79728"))
            )

            verify(
                exactly(1), getRequestedFor(urlPathEqualTo("/diagnosiskeys/download/2020-08-19"))
                    .withQueryParam("batchTag", equalTo("5a0df0cd-4663-4119-ade4-3a30ab8e164d"))
            )
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


        val client = InteropClient(wireMock)

        expectThat(client.downloadKeys(localDate)).isA<NoContent>()
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

        val client = InteropClient(wireMock)

        expectCatching { client.downloadKeys(localDate) }
            .isFailure()
            .message
            .isA<String>()
            .endsWith("failed with status code 500")
    }

    @Test
    fun `download diagnosis keys and handle unknown server payload`() {
        wireMock.stubFor(
            get("/diagnosiskeys/download/2020-08-19")
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json; charset=utf-8")
                        .withBody("")
                )
        )

        val client = InteropClient(wireMock)

        expectCatching { client.downloadKeys(localDate) }
            .isFailure()
    }

    @Test
    fun `download diagnosis keys and receive extra unknown payload fields`() {
        with(wireMock) {
            stubFor(
                get("/diagnosiskeys/download/2020-08-19")
                    .willReturn(
                        aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json; charset=utf-8")
                            .withBody(
                                """
                                {
                                    "batchTag": "80e77dc6-8c27-42fb-8e38-1a0b1f51bf01",
                                    "exposures": [{
                                        "keyData": "xnGNbiVKd7xarkv9Gbdi5w==",
                                        "rollingStartNumber": 2662992,
                                        "transmissionRiskLevel": 0,
                                        "rollingPeriod": 144,
                                        "origin": "JE",
                                        "testType": 1,
                                        "reportType": 1,
                                        "daysSinceOnset": 0,
                                        "regions": ["GB"],
                                        "unknownField": "unknownValue"
                                    }]
                                }
                                """
                            )
                    )
            )

            stubFor(
                get("/diagnosiskeys/download/2020-08-19?batchTag=80e77dc6-8c27-42fb-8e38-1a0b1f51bf01")
                    .willReturn(
                        aResponse()
                            .withStatus(204)
                            .withHeader("Content-Type", "application/json; charset=utf-8")
                            .withBody("")
                    )
            )
        }

        val client = InteropClient(wireMock)

        expectThat(client.downloadKeys(localDate))
            .isA<DiagnosisKeysDownloadResponse>().and {
                get(DiagnosisKeysDownloadResponse::batchTag).isEqualTo(BatchTag.of("80e77dc6-8c27-42fb-8e38-1a0b1f51bf01"))
                get(DiagnosisKeysDownloadResponse::exposures).first().isEqualTo(
                    ExposureDownload(
                        keyData = "xnGNbiVKd7xarkv9Gbdi5w==",
                        rollingStartNumber = 2662992,
                        transmissionRiskLevel = 0,
                        rollingPeriod = 144,
                        origin = "JE",
                        regions = listOf("GB"),
                        testType = LAB_RESULT,
                        reportType = CONFIRMED_TEST,
                        daysSinceOnset = 0
                    )
                )
            }
    }

    @Test
    fun `upload diagnosis keys`() {
        wireMock.stubFor(
            post("/diagnosiskeys/upload")
                .withHeader("Authorization", equalTo("""Bearer DUMMY_TOKEN"""))
                .withRequestBody(
                    equalToJson(
                        """
                        {
                            "batchTag": "d6826d4b-d30f-47c5-ab21-932a874ad7fb",
                            "payload": "eyJhbGciOiJFUzI1NiJ9.W3sia2V5RGF0YSI6ImtleSIsInJvbGxpbmdTdGFydE51bWJlciI6MCwidHJhbnNtaXNzaW9uUmlza0xldmVsIjo0LCJyb2xsaW5nUGVyaW9kIjoxNDQsInJlZ2lvbnMiOltdLCJ0ZXN0VHlwZSI6MSwicmVwb3J0VHlwZSI6MSwiZGF5c1NpbmNlT25zZXQiOjB9XQ==."
                        }
                        """.trimIndent()
                    )
                )
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{ "batchTag": "75b326f7-ae6f-42f6-9354-00c0a6b797b3", "insertedExposures":0 }""")
                )
        )

        val jws = JWS {
            mockk {
                every { asJWSCompatible() } returns ByteArray(0)
            }
        }

        val client = InteropClient(wireMock, jws) {
            UUID.fromString("d6826d4b-d30f-47c5-ab21-932a874ad7fb")
        }

        client.uploadKeys(
            listOf(
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
        )

        wireMock.verify(postRequestedFor(urlEqualTo("/diagnosiskeys/upload")))
    }

    @Test
    fun `throws exception when uploading keys fails`() {
        wireMock.stubFor(
            post("/diagnosiskeys/upload")
                .willReturn(
                    aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{ "message": "this failed" }""")
                )
        )

        val jws = JWS {
            mockk {
                every { asJWSCompatible() } returns ByteArray(0)
            }
        }

        val client = InteropClient(wireMock, jws) {
            UUID.fromString("d6826d4b-d30f-47c5-ab21-932a874ad7fb")
        }

        expectCatching {
            client.uploadKeys(
                listOf(
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
            )
        }.isFailure()
    }

    @Test
    fun `throws exception when uploading keys fails with unexpected response`() {
        wireMock.stubFor(
            post("/diagnosiskeys/upload")
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{ "message": "this failed" }""")
                )
        )

        val jws = JWS {
            mockk {
                every { asJWSCompatible() } returns ByteArray(0)
            }
        }

        val client = InteropClient(wireMock, jws) {
            UUID.fromString("d6826d4b-d30f-47c5-ab21-932a874ad7fb")
        }

        expectCatching {
            client.uploadKeys(
                listOf(
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
            )
        }.isFailure()
    }

    private val interopDownloadPayload =
        """
        {
            "batchTag": "5a0df0cd-4663-4119-ade4-3a30ab8e164d",
            "exposures": [{
                    "keyData": "aLnv60YyaPE3A72sylGDWQ==",
                    "rollingStartNumber": 2686608,
                    "transmissionRiskLevel": 0,
                    "rollingPeriod": 144,
                    "origin": "GB-SCT",
                    "reportType": 1,
                    "daysSinceOnset": 0,
                    "testType": 1,
                    "regions": [
                        "GB"
                    ]
                },
                {
                    "keyData": "M1Hp0pr5XxecQueVruc0dw==",
                    "rollingStartNumber": 2686896,
                    "transmissionRiskLevel": 0,
                    "rollingPeriod": 92,
                    "origin": "GB-SCT",
                    "reportType": 1,
                    "daysSinceOnset": 0,
                    "testType": 1,
                    "regions": [
                        "GB"
                    ]
                },
                {
                    "keyData": "n5PSUvx3Cav2AaUf4inmbw==",
                    "rollingStartNumber": 2686464,
                    "transmissionRiskLevel": 0,
                    "rollingPeriod": 144,
                    "origin": "GB-SCT",
                    "reportType": 1,
                    "daysSinceOnset": 0,
                    "testType": 1,
                    "regions": [
                        "GB"
                    ]
                },
                {
                    "keyData": "HggOybX+KAROyyliTXSztw==",
                    "rollingStartNumber": 2686752,
                    "transmissionRiskLevel": 0,
                    "rollingPeriod": 144,
                    "origin": "GB-SCT",
                    "reportType": 1,
                    "daysSinceOnset": 0,
                    "testType": 1,
                    "regions": [
                        "GB"
                    ]
                },
                {
                    "keyData": "idV+wX5+PXxV24g7TLPXBA==",
                    "rollingStartNumber": 2686752,
                    "transmissionRiskLevel": 0,
                    "rollingPeriod": 144,
                    "origin": "GB-SCT",
                    "reportType": 1,
                    "daysSinceOnset": 0,
                    "testType": 1,
                    "regions": [
                        "GB"
                    ]
                },
                {
                    "keyData": "DujEPwtjPHFa+4dGh6zd7Q==",
                    "rollingStartNumber": 2686896,
                    "transmissionRiskLevel": 0,
                    "rollingPeriod": 92,
                    "origin": "GB-SCT",
                    "reportType": 1,
                    "daysSinceOnset": 0,
                    "testType": 1,
                    "regions": [
                        "GB"
                    ]
                },
                {
                    "keyData": "YrCChoXZKE4pKoS6p3tL+w==",
                    "rollingStartNumber": 2686464,
                    "transmissionRiskLevel": 0,
                    "rollingPeriod": 144,
                    "origin": "GB-SCT",
                    "reportType": 1,
                    "daysSinceOnset": 0,
                    "testType": 1,
                    "regions": [
                        "GB"
                    ]
                }
            ]
        }
        """.trimIndent()

    private val interopDownloadPayload2 =
        """
        {
            "batchTag": "a25df527-d674-4972-bd62-ce3ff31417d4",
            "exposures": [{
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
                }
            ]
        }                       
        """.trimIndent()

    private fun InteropClient(
        wireMockServer: WireMockServer,
        jws: JWS = mockk { every { sign(any()) } returns "DUMMY_SIGNATURE" },
        uniqueId: Supplier<UUID> = UniqueId.ID
    ) = InteropClient(
        interopBaseUrl = wireMockServer.baseUrl(),
        authToken = "DUMMY_TOKEN",
        jws = jws,
        events = events,
        uniqueId = uniqueId
    )
}
