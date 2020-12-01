package uk.nhs.nhsx.keyfederation;

import com.amazonaws.services.lambda.runtime.Context;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.nhs.nhsx.core.Jackson;
import uk.nhs.nhsx.keyfederation.download.DiagnosisKeysDownloadResponse;
import uk.nhs.nhsx.keyfederation.upload.DiagnosisKeysUploadRequest;
import uk.nhs.nhsx.keyfederation.upload.DiagnosisKeysUploadResponse;
import uk.nhs.nhsx.keyfederation.upload.JWS;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.net.http.HttpRequest.BodyPublishers.ofString;
import static uk.nhs.nhsx.core.Jackson.toJson;
import static uk.nhs.nhsx.core.UncheckedException.uncheckedGet;

public class InteropClient {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Logger logger = LogManager.getLogger(InteropClient.class);

    private final String interopBaseUrl;
    private final String authToken;
    private final JWS jws;

    private final HttpClient client = HttpClient.newHttpClient();

    public InteropClient(String interopBaseUrl, String authToken, JWS jws) {
        this.interopBaseUrl = interopBaseUrl;
        this.authToken = authToken;
        this.jws = jws;
    }

    public List<DiagnosisKeysDownloadResponse> downloadKeys(final LocalDate date, final BatchTag batchTag, int maxBatchDownloadCount, Context context) {
        var batch = Optional.ofNullable(batchTag).map(b -> "?batchTag=" + b.value).orElse("");
        var exposureKeysNextBatch = getExposureKeysBatch(date, batch);

        ArrayList<DiagnosisKeysDownloadResponse> responses = new ArrayList<>();
        long iterationDuration = 0L;
        for (int i = 0; i < maxBatchDownloadCount && exposureKeysNextBatch.isPresent(); i++) {
            var startTime = System.currentTimeMillis();
            var diagnosisKeysDownloadResponse = exposureKeysNextBatch.get();
            responses.add(diagnosisKeysDownloadResponse);
            logger.info("Downloaded {} keys from federated server, BatchTag {} (batch {})",
                    diagnosisKeysDownloadResponse.exposures.size(),
                    diagnosisKeysDownloadResponse.batchTag,
                    i);
            exposureKeysNextBatch = getExposureKeysBatch(date, "?batchTag=" + diagnosisKeysDownloadResponse.batchTag);
            iterationDuration = Math.max(iterationDuration,System.currentTimeMillis() - startTime);
            if(iterationDuration >= context.getRemainingTimeInMillis()){
                logger.warn("There is not enough time to complete another iteration");
                break;
            }
        }

        logger.info("Downloaded keys from federated server finished, batchCount={}", responses.size());

        return responses;
    }

    private Optional<DiagnosisKeysDownloadResponse> getExposureKeysBatch(LocalDate date, String batchTag) {
        var request = HttpRequest.newBuilder()
            .header("Authorization", "Bearer " + authToken)
            .uri(URI.create(interopBaseUrl + "/diagnosiskeys/download/" + date.format(FORMATTER) + batchTag))
            .build();

        var response = uncheckedGet(() -> client.send(request, HttpResponse.BodyHandlers.ofString()));

        logger.debug("GET {} -> {}", request.uri(), response.statusCode());

        if (response.statusCode() == 200) {
            return Optional.of(
                Jackson.deserializeMaybe(response.body(), DiagnosisKeysDownloadResponse.class)
                    .orElseThrow(RuntimeException::new)
            );
        }

        if (response.statusCode() == 204) {
            return Optional.empty();
        }

        logger.error("Request to download keys from federated key server with batch tag " + batchTag + " failed with status code " + response.statusCode());
        throw new RuntimeException("Unexpected HTTP status code " + response.statusCode());
    }

    public DiagnosisKeysUploadResponse uploadKeys(String payload) {
        try {
            DiagnosisKeysUploadRequest requestBody = new DiagnosisKeysUploadRequest(UUID.randomUUID().toString(), jws.sign(payload));

            HttpRequest uploadRequest = HttpRequest.newBuilder()
                .header("Authorization", "Bearer " + authToken)
                .header("Content-Type", "application/json")
                .uri(URI.create(interopBaseUrl + "/diagnosiskeys/upload"))
                .POST(ofString(toJson(requestBody)))
                .build();
            HttpResponse<String> httpResponse = client.send(uploadRequest, HttpResponse.BodyHandlers.ofString());

            logger.debug("POST {} -> {}", uploadRequest.uri(), httpResponse.statusCode());

            if (httpResponse.statusCode() == 200) {
                return Jackson.deserializeMaybe(httpResponse.body(), DiagnosisKeysUploadResponse.class).orElseThrow(RuntimeException::new);
            } else {
                logger.error("Request to upload keys to federated key server failed with status code " + httpResponse.statusCode());
                throw new RuntimeException("Unexpected HTTP status code " + httpResponse.statusCode());
            }
        } catch (InterruptedException | IOException  e) {
            logger.error("Request to upload keys to federated key server failed", e);
            throw new RuntimeException(e);
        }
    }
}
