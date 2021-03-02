package uk.nhs.nhsx.aae;

import com.amazonaws.services.s3.model.S3Object;
import org.apache.http.ssl.SSLContexts;
import uk.nhs.nhsx.core.aws.secretsmanager.SecretManager;
import uk.nhs.nhsx.core.aws.secretsmanager.SecretName;
import uk.nhs.nhsx.core.events.Events;
import uk.nhs.nhsx.core.events.OutgoingHttpRequest;

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
    public static final Duration HTTP_PUT_TIME_OUT = Duration.ofSeconds(10);
    public static final int AAE_UPLOAD_SUCCESS_HTTP_RESPONSE_CODE = 201;
    public static final String SUBSCRIPTION_HEADER_NAME = "Ocp-Apim-Subscription-Key";

    private final AAEUploadConfig config;
    private final Events events;
    private final String subscription;
    private final HttpClient httpClient;

    public AAEUploader(AAEUploadConfig config, SecretManager secretManager, Events events) {
        this.config = config;
        this.events = events;
        this.subscription = secretManager
            .getSecret(SecretName.of(config.subscriptionKeySecretName))
            .map(it -> it.value)
            .orElseThrow(() -> new RuntimeException("subscriptionKey does not exits in secret manager"));

        try {
            this.httpClient = createMutualAuthHttpClient(secretManager, config);
        } catch (Exception e) {
            throw new RuntimeException("Initialization failed (missing or wrong .p12 data/password in SecretsManager?)", e);
        }
    }

    private HttpClient createMutualAuthHttpClient(SecretManager secretManager, AAEUploadConfig config) throws Exception {
        var p12Certificate = secretManager
            .getSecretBinary(SecretName.of(config.p12CertificateSecretName));

        var p12CertificatePassword = secretManager
            .getSecret(SecretName.of(config.p12CertificatePasswordSecretName))
            .map(it -> it.value.toCharArray())
            .orElseThrow(() -> new RuntimeException("p12CertificatePassword does not exits in secret manager"));

        return createMutualAuthHttpClient(
            new ByteArrayInputStream(p12Certificate),
            p12CertificatePassword,
            HTTP_PUT_TIME_OUT
        );
    }

    @SuppressWarnings("SameParameterValue")
    static HttpClient createMutualAuthHttpClient(InputStream p12CertInputStream,
                                                 char[] p12CertPassword,
                                                 Duration timeout) throws Exception {
        var tks = KeyStore.getInstance("PKCS12");
        tks.load(p12CertInputStream, p12CertPassword);

        var sslContext = SSLContexts.custom()
            .loadKeyMaterial(tks, p12CertPassword)
            .build();

        return HttpClient.newBuilder()
            .connectTimeout(timeout)
            .sslContext(sslContext)
            .build();
    }

    public void uploadToAAE(S3Object s3Object) throws Exception {
        var uploadRequest = HttpRequest.newBuilder()
            .header(SUBSCRIPTION_HEADER_NAME, subscription)
            .header("Content-Type", s3Object.getObjectMetadata().getContentType())
            .uri(URI.create(config.aaeUrlPrefix + getFilename(s3Object) + config.aaeUrlSuffix))
            .PUT(HttpRequest.BodyPublishers.ofByteArray(getContent(s3Object)))
            .build();

        var httpResponse = httpClient.send(uploadRequest, HttpResponse.BodyHandlers.ofString());

        events.emit(getClass(), new OutgoingHttpRequest(uploadRequest.uri().toString(), uploadRequest.method(), httpResponse.statusCode()));

        if (httpResponse.statusCode() != AAE_UPLOAD_SUCCESS_HTTP_RESPONSE_CODE) {
            throw new RuntimeException("Unexpected HTTP response code " + httpResponse.statusCode());
        }
    }

    static byte[] getContent(S3Object obj) throws IOException {
        try (var in = obj.getObjectContent()) {
            return in.readAllBytes();
        }
    }

    static String getFilename(S3Object obj) {
        return obj.getKey().lastIndexOf("/") != -1
            ? obj.getKey().substring(obj.getKey().lastIndexOf("/") + 1)
            : obj.getKey();
    }
}
