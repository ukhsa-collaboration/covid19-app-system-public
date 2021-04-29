package uk.nhs.nhsx.analyticsedge

import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.S3Object
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.any
import com.github.tomakehurst.wiremock.client.WireMock.anyUrl
import com.github.tomakehurst.wiremock.client.WireMock.exactly
import com.github.tomakehurst.wiremock.client.WireMock.put
import com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.throws
import org.apache.http.client.utils.URIBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.nhs.nhsx.analyticsedge.upload.EdgeDataUploadHandler
import uk.nhs.nhsx.analyticsedge.upload.EdgeUploader
import uk.nhs.nhsx.analyticsedge.upload.EdgeUploaderConfig
import uk.nhs.nhsx.analyticsexporter.DataUploadedToS3
import uk.nhs.nhsx.analyticsexporter.S3ObjectNotFound
import uk.nhs.nhsx.core.SystemClock
import uk.nhs.nhsx.core.TestEnvironments
import uk.nhs.nhsx.core.aws.s3.AwsS3
import uk.nhs.nhsx.core.aws.s3.Locator
import uk.nhs.nhsx.core.aws.secretsmanager.SecretManager
import uk.nhs.nhsx.core.aws.secretsmanager.SecretName
import uk.nhs.nhsx.core.aws.secretsmanager.SecretValue
import uk.nhs.nhsx.core.events.ExceptionThrown
import uk.nhs.nhsx.core.events.OutgoingHttpRequest
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.core.handler.QueuedEventCompleted
import uk.nhs.nhsx.core.handler.QueuedEventStarted
import uk.nhs.nhsx.testhelper.ContextBuilder
import uk.nhs.nhsx.testhelper.proxy
import uk.nhs.nhsx.testhelper.wiremock.WireMockExtension
import java.io.ByteArrayInputStream
import java.util.Optional

@ExtendWith(WireMockExtension::class)
class EdgeUploadHandlerTest(private val wireMock: WireMockServer) {

    @Test
    fun `handle sqs event successfully uploads to edge`() {
        wireMock.stubFor(
            put("/TEST.csv?SAS_TOKEN")
                .willReturn(
                    aResponse()
                        .withStatus(201)
                )
        )

        val sqsEvent = SQSEvent().apply {
            records = listOf(
                SQSEvent.SQSMessage().apply {
                    body = """{ "bucketName": "TEST_BUCKET", "key": "TEST.csv" }"""
                }
            )
        }

        val result = newHandler(wireMock).handleRequest(sqsEvent, ContextBuilder.aContext())

        assertThat(result).isEqualTo("DataUploadedToS3(sqsMessageId=null, bucketName=TEST_BUCKET, key=TEST.csv)")

        events.containsExactly(
            QueuedEventStarted::class,
            OutgoingHttpRequest::class,
            DataUploadedToS3::class,
            QueuedEventCompleted::class
        )

        val outgoingRequestEvent = events.find { event -> event is  OutgoingHttpRequest }
        assertThat(URIBuilder((outgoingRequestEvent as OutgoingHttpRequest).uri).build().query).isNullOrEmpty()
    }

    @Test
    fun `handle sqs event error upload to edge`() {
        wireMock.stubFor(
            put("/TEST.csv?SAS_TOKEN")
                .willReturn(
                    aResponse()
                        .withStatus(500)
                )
        )

        val sqsEvent = SQSEvent().apply {
            records = listOf(
                SQSEvent.SQSMessage().apply {
                    body = """{ "bucketName": "TEST_BUCKET", "key": "TEST.csv" }"""
                }
            )
        }

        assertThat(
            { newHandler(wireMock).handleRequest(sqsEvent, ContextBuilder.aContext()) },
            throws<RuntimeException>()
        )

        events.containsExactly(QueuedEventStarted::class, OutgoingHttpRequest::class, ExceptionThrown::class)
    }

    @Test
    fun `handle sqs event cannot find object in s3`() {
        wireMock.stubFor(
            any(anyUrl())
                .willReturn(
                    aResponse()
                        .withStatus(201)
                )
        )

        val sqsEvent = SQSEvent().apply {
            records = listOf(
                SQSEvent.SQSMessage()
                    .apply { body = """{ "bucketName": "TEST_BUCKET", "key": "FILE_NOT_FOUND.csv" }""" }
            )
        }

        val result = newHandler(wireMock).handleRequest(sqsEvent, ContextBuilder.aContext())

        assertThat(result).isEqualTo("S3ObjectNotFound(sqsMessageId=null, bucketName=TEST_BUCKET, key=FILE_NOT_FOUND.csv)")
        wireMock.verify(exactly(0), putRequestedFor(anyUrl()))

        events.containsExactly(QueuedEventStarted::class, S3ObjectNotFound::class, QueuedEventCompleted::class)

    }

    @Test
    fun `handle sqs event cannot be parsed`() {
        wireMock.stubFor(
            any(anyUrl())
                .willReturn(
                    aResponse()
                        .withStatus(201)
                )
        )

        val sqsEvent = SQSEvent().apply {
            records = listOf(
                SQSEvent.SQSMessage().apply { body = "{}" }
            )
        }

        val result = newHandler(wireMock).handleRequest(sqsEvent, ContextBuilder.aContext())

        assertThat(result).isEqualTo("ExceptionThrown(exception=java.lang.RuntimeException: SQS message parsing failed (no retry): sqsMessage.id=null, body={}, message=SQS message parsing failed (no retry): sqsMessage.id=null, body={})")
        wireMock.verify(exactly(0), putRequestedFor(anyUrl()))

        events.containsExactly(QueuedEventStarted::class, ExceptionThrown::class, QueuedEventCompleted::class)
    }

    private val events = RecordingEvents()

    private fun newHandler(server: WireMockServer) = EdgeDataUploadHandler(
        TestEnvironments.TEST.apply(
            mapOf(
                "MAINTENANCE_MODE" to "false",
                "custom_oai" to "OAI"
            )
        ),
        SystemClock.CLOCK,
        events,
        FakeS3(),
        getEdgeUploaderConfig(server),
        EdgeUploader(
            getEdgeUploaderConfig(server),
            FakeSecretManager("SAS_TOKEN"),
            events
        )
    )
    private fun getEdgeUploaderConfig(server: WireMockServer) : EdgeUploaderConfig = EdgeUploaderConfig(
        server.baseUrl(),
        "SAS_TOKEN_KEY",
        ""
    )
}

class FakeSecretManager constructor(private val sasToken: String) : SecretManager {


    override fun getSecret(secretName: SecretName): Optional<SecretValue> =
        when (secretName.value) {
            "SAS_TOKEN_KEY" -> Optional.of(SecretValue.of(sasToken))
            else -> Optional.empty()
        }

    override fun getSecretBinary(secretName: SecretName): ByteArray = ByteArray(0)
}

class FakeS3 : AwsS3 by proxy() {

    private val objects = mapOf(
        "TEST.csv" to S3Object().apply {
            key = "TEST.csv"
            setObjectContent(ByteArrayInputStream(ByteArray(0)))
            objectMetadata = ObjectMetadata().apply { contentType = "text/csv" }
        }
    )

    override fun getObject(locator: Locator): Optional<S3Object> = Optional.ofNullable(objects[locator.key.value])
}
