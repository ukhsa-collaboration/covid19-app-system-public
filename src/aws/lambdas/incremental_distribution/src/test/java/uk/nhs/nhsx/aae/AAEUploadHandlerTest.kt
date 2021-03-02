package uk.nhs.nhsx.aae

import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.S3Object
import com.amazonaws.services.s3.model.S3ObjectSummary
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.any
import com.github.tomakehurst.wiremock.client.WireMock.anyUrl
import com.github.tomakehurst.wiremock.client.WireMock.exactly
import com.github.tomakehurst.wiremock.client.WireMock.put
import com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.throws
import org.apache.http.entity.ContentType
import org.assertj.core.api.Assertions.assertThat
import org.bouncycastle.asn1.ASN1InputStream
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.nhs.nhsx.core.aws.s3.AwsS3
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.ByteArraySource
import uk.nhs.nhsx.core.aws.s3.Locator
import uk.nhs.nhsx.core.aws.s3.MetaHeader
import uk.nhs.nhsx.core.aws.secretsmanager.SecretManager
import uk.nhs.nhsx.core.aws.secretsmanager.SecretName
import uk.nhs.nhsx.core.aws.secretsmanager.SecretValue
import uk.nhs.nhsx.core.events.ExceptionThrown
import uk.nhs.nhsx.core.events.OutgoingHttpRequest
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.core.queued.QueuedEventCompleted
import uk.nhs.nhsx.core.queued.QueuedEventStarted
import uk.nhs.nhsx.testhelper.ContextBuilder
import uk.nhs.nhsx.testhelper.wiremock.WireMockExtension
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.Date
import java.util.Optional

@ExtendWith(WireMockExtension::class)
class AAEUploadHandlerTest(private val wireMock: WireMockServer) {

    @Test
    fun `handle sqs event successfully uploads to aae`() {
        wireMock.stubFor(
            put("/TEST_KEY")
                .willReturn(
                    aResponse()
                        .withStatus(201)
                )
        )

        val sqsEvent = SQSEvent().apply {
            records = listOf(
                SQSEvent.SQSMessage().apply {
                    body = """{ "bucketName": "TEST_BUCKET", "key": "TEST_PREFIX/TEST_KEY" }"""
                }
            )
        }

        val result = newHandler(wireMock).handleRequest(sqsEvent, ContextBuilder.aContext())

        assertThat(result).isEqualTo("AAEDataUploadedToS3(sqsMessageId=null, bucketName=TEST_BUCKET, key=TEST_PREFIX/TEST_KEY)")

        events.containsExactly(QueuedEventStarted::class, OutgoingHttpRequest::class, AAEDataUploadedToS3::class, QueuedEventCompleted::class)
    }

    @Test
    fun `handle sqs event error upload to aae`() {
        wireMock.stubFor(
            put("/TEST_KEY")
                .willReturn(
                    aResponse()
                        .withStatus(500)
                )
        )

        val sqsEvent = SQSEvent().apply {
            records = listOf(
                SQSEvent.SQSMessage().apply {
                    body = """{ "bucketName": "TEST_BUCKET", "key": "TEST_PREFIX/TEST_KEY" }"""
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
                    .apply { body = """{ "bucketName": "TEST_BUCKET", "key": "TEST_PREFIX/TEST_KEY_NOT_FOUND" }""" }
            )
        }

        val result = newHandler(wireMock).handleRequest(sqsEvent, ContextBuilder.aContext())

        assertThat(result).isEqualTo("S3ObjectNotFound(sqsMessageId=null, bucketName=TEST_BUCKET, key=TEST_PREFIX/TEST_KEY_NOT_FOUND)")
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

    @Test
    fun `handle sqs event format conversion failure`() {
        wireMock.stubFor(
            any(anyUrl())
                .willReturn(
                    aResponse()
                        .withStatus(201)
                )
        )

        val sqsEvent = SQSEvent().apply {
            records = listOf(
                SQSEvent.SQSMessage().apply {
                    body = """{ "bucketName": "TEST_BUCKET", "key": "format-conversion-failed/TEST_KEY_ERROR_FILE" }"""
                }
            )
        }

        val result = newHandler(wireMock).handleRequest(sqsEvent, ContextBuilder.aContext())

        assertThat(result).isEqualTo("S3ToParquetObjectConversionFailure(sqsMessageId=null, bucketName=TEST_BUCKET, key=format-conversion-failed/TEST_KEY_ERROR_FILE)")
        wireMock.verify(exactly(0), putRequestedFor(anyUrl()))

        events.containsExactly(QueuedEventStarted::class, S3ToParquetObjectConversionFailure::class, QueuedEventCompleted::class)
    }

    private val events = RecordingEvents()

    private fun newHandler(server: WireMockServer) = AAEUploadHandler(
        FakeParquetS3(),
        AAEUploader(
            AAEUploadConfig(
                "${server.baseUrl()}/",
                "",
                "p12Cert",
                "p12CertPassword",
                "subKey"
            ),
            FakeSecretManager(),
            events
        ),
        events
    )
}

class FakeSecretManager : SecretManager {

    private val testPassword = "abc123"
    private val pkcs12: ByteArray
    private val now = Instant.now()

    init {
        val keyPair = KeyPairGenerator.getInstance("RSA").let {
            it.initialize(4096)
            it.generateKeyPair()
        }

        val certBuilder = X509v3CertificateBuilder(
            X500Name("cn=example"),
            BigInteger(10, SecureRandom()),
            Date.from(now),
            Date.from(now.plus(Duration.ofHours(1))),
            X500Name("dc=name"),
            SubjectPublicKeyInfo.getInstance(ASN1InputStream(keyPair.public.encoded).readObject())
        )

        val signer = JcaContentSignerBuilder("SHA256WithRSA").build(keyPair.private)
        val holder = certBuilder.build(signer)

        val certificate = JcaX509CertificateConverter()
            .setProvider(BouncyCastleProvider())
            .getCertificate(holder)

        val keyStore = KeyStore.getInstance("PKCS12").apply {
            load(null, null)
            setKeyEntry("main", keyPair.private, testPassword.toCharArray(), arrayOf(certificate))
        }

        pkcs12 = ByteArrayOutputStream().use {
            keyStore.store(it, testPassword.toCharArray())
            it.toByteArray()
        }
    }

    override fun getSecret(secretName: SecretName): Optional<SecretValue> = Optional.of(SecretValue.of(testPassword))

    override fun getSecretBinary(secretName: SecretName): ByteArray = pkcs12
}

class FakeParquetS3 : AwsS3 {

    private val objects = mapOf(
        "TEST_PREFIX/TEST_KEY" to S3Object().apply {
            key = "TEST_PREFIX/TEST_KEY"
            setObjectContent(ByteArrayInputStream(ByteArray(0)))
            objectMetadata = ObjectMetadata().apply { contentType = "" }
        },
        "format-conversion-failed/TEST_KEY_ERROR_FILE" to S3Object().apply {
            key = "format-conversion-failed/TEST_KEY_ERROR_FILE"
            setObjectContent(ByteArrayInputStream(ByteArray(0)))
            objectMetadata = ObjectMetadata().apply { contentType = "" }
        }
    )

    override fun upload(locator: Locator, contentType: ContentType, bytes: ByteArraySource, meta: List<MetaHeader>) {
        throw UnsupportedOperationException()
    }

    override fun getObjectSummaries(bucketName: BucketName): MutableList<S3ObjectSummary> {
        throw UnsupportedOperationException()
    }

    override fun getObject(locator: Locator): Optional<S3Object> = Optional.ofNullable(objects[locator.key.value])

    override fun deleteObject(locator: Locator) {
        throw UnsupportedOperationException()
    }
}
