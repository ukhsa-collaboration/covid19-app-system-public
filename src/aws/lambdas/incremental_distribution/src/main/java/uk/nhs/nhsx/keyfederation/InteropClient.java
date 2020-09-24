package uk.nhs.nhsx.keyfederation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jose4j.lang.JoseException;
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
import java.util.UUID;

import static java.net.http.HttpRequest.BodyPublishers.ofString;
import static uk.nhs.nhsx.core.Jackson.toJson;

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

    public List<DiagnosisKeysDownloadResponse> downloadKeys(LocalDate date, BatchTag batchTag) throws Exception {
        String batch = batchTag == null ? "" : "?batchTag=" + batchTag.value;
        DiagnosisKeysDownloadResponse exposureKeysNextBatch = getExposureKeysBatch(date, batch);

        ArrayList<DiagnosisKeysDownloadResponse> responses = new ArrayList<>();
        while (exposureKeysNextBatch != null) {
            responses.add(exposureKeysNextBatch);
            logger.info(String.format("Downloaded %s keys from federated server, Batch %s",
                exposureKeysNextBatch.exposures.size(),
                exposureKeysNextBatch.batchTag));
            exposureKeysNextBatch = getExposureKeysBatch(date, "?batchTag=" + exposureKeysNextBatch.batchTag);
        }
        return responses;
    }

    private DiagnosisKeysDownloadResponse getExposureKeysBatch(LocalDate date, String batchTag) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .header("Authorization", "Bearer " + authToken)
            .uri(URI.create(interopBaseUrl + "/diagnosiskeys/download/" + date.format(FORMATTER) + batchTag))
            .build();
        HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (httpResponse.statusCode() == 200) {
            return Jackson.deserializeMaybe(httpResponse.body(), DiagnosisKeysDownloadResponse.class).orElseThrow(RuntimeException::new);
        } else if (httpResponse.statusCode() != 204){
            logger.warn("Request to federated key server with batch tag " + batchTag + " failed with status code " + httpResponse.statusCode());
        }
        return null;
    }

    public DiagnosisKeysUploadResponse uploadKeys(String payload) {
        try {
            String signedPayload = jws.compactSignedPayload(payload); //signing

            DiagnosisKeysUploadRequest requestBody = new DiagnosisKeysUploadRequest(UUID.randomUUID().toString(), signedPayload);

            HttpRequest uploadRequest = HttpRequest.newBuilder()
                .header("Authorization", "Bearer " + authToken)
                .uri(URI.create(interopBaseUrl + "/diagnosiskeys/upload"))
                .POST(ofString(toJson(requestBody)))
                .build();
            HttpResponse<String> httpResponse = client.send(uploadRequest, HttpResponse.BodyHandlers.ofString());
            if (httpResponse.statusCode() == 200) {
                return Jackson.deserializeMaybe(httpResponse.body(), DiagnosisKeysUploadResponse.class).orElseThrow(RuntimeException::new);
            } else {
                logger.warn("Request to upload keys to federated key server failed with status code " + httpResponse.statusCode());
            }
            return null;
        } catch (InterruptedException | IOException | JoseException e) {
            logger.error("Upload request failed", e);
            throw new RuntimeException(e);
        }
    }
}
