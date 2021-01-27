package uk.nhs.nhsx.aae;

import com.amazonaws.services.s3.model.S3Object;
import com.google.common.io.ByteStreams;
import org.apache.http.ssl.SSLContexts;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.nhs.nhsx.core.Environment;
import uk.nhs.nhsx.core.aws.secretsmanager.AwsSecretManager;
import uk.nhs.nhsx.core.aws.secretsmanager.SecretManager;
import uk.nhs.nhsx.core.aws.secretsmanager.SecretName;

import javax.net.ssl.SSLContext;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyStore;
import java.time.Duration;

public class AAEUploader {
    public static final int HTTP_PUT_TIME_OUT_SECONDS = 10;
    public static final int AAE_UPLOAD_SUCCESS_HTTP_RESPONSE_CODE = 201;
    public static final String SUBSCRIPTION_HEADER_NAME = "Ocp-Apim-Subscription-Key";

    private static final Logger logger = LogManager.getLogger(AAEUploader.class);

    private final AAEUploadConfig config;
    private final String subscription;
    private final HttpClient httpClient;

    public AAEUploader() {
        this(AAEUploadConfig.fromEnvironment(Environment.fromSystem()), new AwsSecretManager());
    }

    public AAEUploader(AAEUploadConfig config, SecretManager secretManager) {
        this.config = config;
        this.subscription = secretManager.getSecret(SecretName.of(config.subscriptionKeySecretName)).get().value;

        try {
            byte[] p12Certificate = secretManager.getSecretBinary(SecretName.of(config.p12CertificateSecretName));
            char[] p12CertificatePassword = secretManager.getSecret(SecretName.of(config.p12CertificatePasswordSecretName)).get().value.toCharArray();
            this.httpClient = createMutualAuthHttpClient(new ByteArrayInputStream(p12Certificate), p12CertificatePassword, HTTP_PUT_TIME_OUT_SECONDS);
        }
        catch (Exception e) {
            logger.error("Initialization failed (missing or wrong .p12 data/password in SecretsManager?)", e);
            throw new RuntimeException(e);
        }
    }

    static HttpClient createMutualAuthHttpClient(InputStream p12CertInputStream, char[] p12CertPassword, int timeOutSeconds) throws Exception {
        KeyStore tks = KeyStore.getInstance("PKCS12");
        tks.load(p12CertInputStream, p12CertPassword);

        SSLContext sslcontext = SSLContexts.custom()
            .loadKeyMaterial(tks, p12CertPassword)
            .build();

        return HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(timeOutSeconds))
            .sslContext(sslcontext)
            .build();
    }

    public void uploadToAAE(S3Object s3Object) throws Exception {
        HttpRequest uploadRequest = HttpRequest.newBuilder()
            .header(SUBSCRIPTION_HEADER_NAME, subscription)
            .header("Content-Type", s3Object.getObjectMetadata().getContentType())
            .uri(URI.create(config.aaeUrlPrefix + getFilename(s3Object) + config.aaeUrlSuffix))
            .PUT(HttpRequest.BodyPublishers.ofByteArray(getContent(s3Object)))
            .build();

        logger.debug("HTTPS PUT Request: uri={}", uploadRequest.uri());

        HttpResponse<String> httpResponse = httpClient.send(uploadRequest, HttpResponse.BodyHandlers.ofString());

        logger.debug("HTTPS PUT Response: code={}, body={}", httpResponse.statusCode(), httpResponse.body());

        if (httpResponse.statusCode() != AAE_UPLOAD_SUCCESS_HTTP_RESPONSE_CODE) {
            throw new RuntimeException("Unexpected HTTP response code " + httpResponse.statusCode());
        }
    }

    static byte[] getContent(S3Object obj) throws IOException {
        try (var in = obj.getObjectContent()) {
            return ByteStreams.toByteArray(in);
        }
    }

    static String getFilename(S3Object obj) {
        return obj.getKey().lastIndexOf("/") != -1
            ? obj.getKey().substring(obj.getKey().lastIndexOf("/") + 1)
            : obj.getKey();
    }
}
