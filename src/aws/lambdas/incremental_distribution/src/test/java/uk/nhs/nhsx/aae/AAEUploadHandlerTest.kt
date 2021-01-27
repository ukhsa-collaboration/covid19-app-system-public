package uk.nhs.nhsx.aae

import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.S3Object
import com.amazonaws.services.s3.model.S3ObjectSummary
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.any
import com.github.tomakehurst.wiremock.client.WireMock.anyUrl
import com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.google.common.io.ByteSource
import org.apache.http.entity.ContentType
import org.assertj.core.api.Assertions.assertThat
import org.bouncycastle.asn1.ASN1InputStream
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.aws.s3.AwsS3
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.Locator
import uk.nhs.nhsx.core.aws.s3.MetaHeader
import uk.nhs.nhsx.core.aws.secretsmanager.SecretManager
import uk.nhs.nhsx.core.aws.secretsmanager.SecretName
import uk.nhs.nhsx.core.aws.secretsmanager.SecretValue
import uk.nhs.nhsx.testhelper.ContextBuilder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import java.util.*


class AAEUploadHandlerTest {

    private val wireMockRule: WireMockServer = WireMockServer(0)

    lateinit var handler: AAEUploadHandler

    @BeforeEach
    fun start() {
        wireMockRule.start()
        handler = AAEUploadHandler(
            FakeParquetS3(),
            AAEUploader(
                AAEUploadConfig("${wireMockRule.baseUrl()}/", "", "p12Cert", "p12CertPassword", "subKey"),
                FakeSecretManager()
            )
        )
    }

    @AfterEach
    fun stop() = wireMockRule.stop()

    @Test
    fun `handle sqs event successfully uploads to aae`() {
        wireMockRule.stubFor(any(anyUrl()).willReturn(aResponse().withStatus(201)))

        val sqsEvent = SQSEvent().apply {
            records = listOf(
                SQSEvent.SQSMessage().apply { body = """{ "bucketName": "TEST_BUCKET", "key": "TEST_PREFIX/TEST_KEY" }""" }
            )
        }

        val result = handler.handleRequest(sqsEvent, ContextBuilder.aContext())
        assertThat(result).isEqualTo("success")
        wireMockRule.verify(1, putRequestedFor(urlEqualTo("/TEST_KEY")))
    }

    @Test
    fun `handle sqs event cannot find object in s3`() {
        wireMockRule.stubFor(any(anyUrl()).willReturn(aResponse().withStatus(201)))

        val sqsEvent = SQSEvent().apply {
            records = listOf(
                SQSEvent.SQSMessage().apply { body = """{ "bucketName": "TEST_BUCKET", "key": "TEST_PREFIX/TEST_KEY_NOT_FOUND" }""" }
            )
        }

        val result = handler.handleRequest(sqsEvent, ContextBuilder.aContext())
        assertThat(result).isEqualTo("not-found")
        wireMockRule.verify(0, putRequestedFor(anyUrl()))
    }

    @Test
    fun `handle sqs event cannot be parsed`() {
        wireMockRule.stubFor(any(anyUrl()).willReturn(aResponse().withStatus(201)))

        val sqsEvent = SQSEvent().apply {
            records = listOf(
                SQSEvent.SQSMessage().apply { body = "{}" }
            )
        }

        val result = handler.handleRequest(sqsEvent, ContextBuilder.aContext())
        assertThat(result).isEqualTo("parsing-error")
        wireMockRule.verify(0, putRequestedFor(anyUrl()))
    }

    @Test
    fun `handle sqs event format conversion failure`() {
        wireMockRule.stubFor(any(anyUrl()).willReturn(aResponse().withStatus(201)))

        val sqsEvent = SQSEvent().apply {
            records = listOf(
                SQSEvent.SQSMessage().apply { body = """{ "bucketName": "TEST_BUCKET", "key": "format-conversion-failed/TEST_KEY_ERROR_FILE" }""" }
            )
        }

        val result = handler.handleRequest(sqsEvent, ContextBuilder.aContext())
        assertThat(result).isEqualTo("format-conversion-error")
        wireMockRule.verify(0, putRequestedFor(anyUrl()))
    }

}

class FakeSecretManager : SecretManager {

    companion object {
        const val TEST_PWD = "abc123"
    }

    private val pkcs12: ByteArray

    init {
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(4096)
        val keyPair = keyPairGenerator.generateKeyPair()

        val certBuilder = X509v3CertificateBuilder(
            X500Name("cn=example"),
            BigInteger(10, SecureRandom()),
            Date(System.currentTimeMillis()),
            Date(System.currentTimeMillis() + 1000 * 60 * 60),
            X500Name("dc=name"),
            SubjectPublicKeyInfo.getInstance(ASN1InputStream(keyPair.public.encoded).readObject())
        )
        val signer = JcaContentSignerBuilder("SHA256WithRSA").build(keyPair.private)
        val holder = certBuilder.build(signer)
        val certificate = JcaX509CertificateConverter().setProvider(BouncyCastleProvider()).getCertificate(holder)

        val keyStore = KeyStore.getInstance("PKCS12")
        keyStore.load(null, null)
        keyStore.setKeyEntry("main", keyPair.private, TEST_PWD.toCharArray(), arrayOf(certificate))
        val outputStream = ByteArrayOutputStream()
        keyStore.store(outputStream, TEST_PWD.toCharArray())
        pkcs12 = outputStream.toByteArray()
    }

    override fun getSecret(secretName: SecretName?): Optional<SecretValue> {
        return Optional.of(SecretValue.of(TEST_PWD))
    }

    override fun getSecretBinary(secretName: SecretName?): ByteArray {
        return pkcs12
    }
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

    override fun upload(locator: Locator?, contentType: ContentType?, bytes: ByteSource?, vararg meta: MetaHeader?) {
        throw UnsupportedOperationException()
    }

    override fun getObjectSummaries(bucketName: BucketName): MutableList<S3ObjectSummary>? {
        throw UnsupportedOperationException()
    }

    override fun getObject(locator: Locator): Optional<S3Object> {
        return Optional.ofNullable(objects[locator.key.value])
    }

    override fun deleteObject(locator: Locator) {
        throw UnsupportedOperationException()
    }
}
