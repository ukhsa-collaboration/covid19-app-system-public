@file:Suppress("TestFunctionName")

package uk.nhs.nhsx.keyfederation.upload

import com.amazonaws.services.lambda.runtime.events.ScheduledEvent
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import io.mockk.called
import io.mockk.every
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import strikt.api.Assertion
import strikt.api.expectThat
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.map
import uk.nhs.nhsx.core.TestEnvironments
import uk.nhs.nhsx.core.aws.dynamodb.TableName
import uk.nhs.nhsx.core.aws.s3.AwsS3
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.secretsmanager.SecretName
import uk.nhs.nhsx.core.aws.ssm.ParameterName
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.keyfederation.InMemoryBatchTagService
import uk.nhs.nhsx.keyfederation.InteropClient
import uk.nhs.nhsx.keyfederation.TestKeyPairs.ecPrime256r1
import uk.nhs.nhsx.testhelper.ContextBuilder.Companion.aContext
import uk.nhs.nhsx.testhelper.assertions.captured
import uk.nhs.nhsx.testhelper.mocks.FakeS3
import uk.nhs.nhsx.testhelper.mocks.Md5TemporaryExposureKeyGenerator
import uk.nhs.nhsx.testhelper.mocks.exposureS3Object
import uk.nhs.nhsx.testhelper.wiremock.WireMockExtension
import java.time.Duration
import java.time.Instant

@ExtendWith(WireMockExtension::class)
class KeyFederationUploadHandlerTest(private val wireMock: WireMockServer) {

    private val events = RecordingEvents()
    private val now = Instant.parse("2020-02-05T10:00:00.000Z")
    private val bucketName = BucketName.of("SUBMISSION_BUCKET")
    private val fakeS3 = FakeS3()

    @Test
    fun `enable upload should call interop`() {
        wireMock.stubFor(
            post("/diagnosiskeys/upload")
                .withHeader("Authorization", equalTo("Bearer DUMMY_TOKEN"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withBody("""{"batchTag": "75b326f7-ae6f-42f6-9354-00c0a6b797b3", "insertedExposures":0 }""")
                )
        )

        val payload = slot<List<ExposureUpload>>()
        val interopClient = spyk(interopClient(wireMock)) {
            every { uploadKeys(capture(payload)) } answers { callOriginal() }
        }

        fakeS3.add(
            exposureS3Object(
                "mobile/LAB_RESULT/foobar",
                bucketName,
                Md5TemporaryExposureKeyGenerator("mobile/LAB_RESULT/foobar")
            ), now
        )

        keyFederationUploadHandler(
            keyFederationUploadConfig(wireMock),
            fakeS3,
            interopClient
        ).handleRequest(ScheduledEvent(), aContext())

        wireMock.verify(postRequestedFor(urlEqualTo("/diagnosiskeys/upload")))

        expectThat(payload)
            .captured
            .containsInAnyOrder("mobile/LAB_RESULT/foobar")
    }

    @Test
    fun `disable upload should not call interop`() {
        val interopClient = spyk(interopClient(wireMock))

        fakeS3.add(
            exposureS3Object(
                "mobile/LAB_RESULT/foobar",
                bucketName,
                Md5TemporaryExposureKeyGenerator("mobile/LAB_RESULT/foobar")
            ), now
        )

        keyFederationUploadHandler(
            keyFederationUploadConfig(wireMock, { false }),
            fakeS3,
            interopClient
        ).handleRequest(ScheduledEvent(), aContext())

        verify { interopClient.uploadKeys(any()) wasNot called }
    }

    @Test
    fun `upload keys should only include mobile results`() {
        wireMock.stubFor(
            post("/diagnosiskeys/upload")
                .withHeader("Authorization", equalTo("Bearer DUMMY_TOKEN"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withBody("""{ "batchTag": "75b326f7-ae6f-42f6-9354-00c0a6b797b3", "insertedExposures":0 }""")
                )
        )

        // broken because of FederatedExposureUploadFactory -> testType != null
        fakeS3.add(exposureS3Object("foo", bucketName, Md5TemporaryExposureKeyGenerator("foo")), now)
        fakeS3.add(exposureS3Object("bar", bucketName, Md5TemporaryExposureKeyGenerator("bar")), now)
        fakeS3.add(exposureS3Object("foobar", bucketName, Md5TemporaryExposureKeyGenerator("foobar")), now)
        fakeS3.add(
            exposureS3Object(
                "federatedKeyPrefix/foo",
                bucketName,
                Md5TemporaryExposureKeyGenerator("federatedKeyPrefix/foo")
            ), now
        )
        fakeS3.add(
            exposureS3Object(
                "nearform/GB-EAW/bar",
                bucketName,
                Md5TemporaryExposureKeyGenerator("nearform/GB-EAW/bar")
            ), now
        )
        fakeS3.add(
            exposureS3Object(
                "mobile/LAB_RESULT/foobar",
                bucketName,
                Md5TemporaryExposureKeyGenerator("mobile/LAB_RESULT/foobar")
            ), now
        )
        fakeS3.add(
            exposureS3Object(
                "mobile/RAPID_RESULT/foobar",
                bucketName,
                Md5TemporaryExposureKeyGenerator("mobile/RAPID_RESULT/foobar")
            ), now
        )

        val config = keyFederationUploadConfig(
            wireMock = wireMock,
            federatedKeyUploadPrefixes = listOf("nearform/GB-EAW", "nearform/NI", "nearform/JE")
        )

        val payload = slot<List<ExposureUpload>>()
        val interopClient = spyk(interopClient(wireMock)) {
            every { uploadKeys(capture(payload)) } answers { callOriginal() }
        }

        keyFederationUploadHandler(
            config,
            fakeS3,
            interopClient
        ).handleRequest(ScheduledEvent(), aContext())

        wireMock.verify(postRequestedFor(urlEqualTo("/diagnosiskeys/upload")))

        expectThat(payload)
            .captured
            .containsInAnyOrder(
                "mobile/LAB_RESULT/foobar",
                "mobile/RAPID_RESULT/foobar"
            )
    }

    private fun keyFederationUploadConfig(
        wireMock: WireMockServer,
        uploadFeatureFlag: () -> Boolean = { true },
        federatedKeyUploadPrefixes: List<String> = emptyList()
    ) = KeyFederationUploadConfig(
        maxSubsequentBatchUploadCount = 100,
        initialUploadHistoryDays = 14,
        maxUploadBatchSize = 0,
        uploadFeatureFlag = uploadFeatureFlag,
        uploadRiskLevelDefaultEnabled = false,
        uploadRiskLevelDefault = -1,
        interopBaseUrl = wireMock.baseUrl(),
        interopAuthTokenSecretName = SecretName.of("authToken"),
        signingKeyParameterName = ParameterName.of("parameter"),
        stateTableName = TableName.of("DUMMY_TABLE"),
        region = "GB-EAW",
        federatedKeyUploadPrefixes = federatedKeyUploadPrefixes,
        loadSubmissionsTimeout = Duration.ofMinutes(12),
        loadSubmissionsThreadPoolSize = 15
    )

    private fun keyFederationUploadHandler(
        config: KeyFederationUploadConfig,
        awsS3Client: AwsS3,
        interopClient: InteropClient
    ) = KeyFederationUploadHandler(
        environment = TestEnvironments.environmentWith(),
        clock = { now },
        events = events,
        submissionBucket = bucketName,
        config = config,
        batchTagService = InMemoryBatchTagService(),
        interopClient = interopClient,
        awsS3Client = awsS3Client
    )

    private fun interopClient(wireMock: WireMockServer) = InteropClient(
        interopBaseUrl = wireMock.baseUrl(),
        authToken = "DUMMY_TOKEN",
        jws = JWS(KmsCompatibleSigner(ecPrime256r1.private)),
        events = events
    )

    private fun Assertion.Builder<List<ExposureUpload>>.containsInAnyOrder(vararg elements: String) {
        val expected = elements.toList().map(Md5TemporaryExposureKeyGenerator::invoke)
        map(ExposureUpload::keyData).containsExactlyInAnyOrder(expected)
    }
}


