package uk.nhs.nhsx.keyfederation;

import uk.nhs.nhsx.core.Jackson;
import uk.nhs.nhsx.core.events.Events;
import uk.nhs.nhsx.core.events.OutgoingHttpRequest;
import uk.nhs.nhsx.core.events.UnprocessableJson;
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
import java.util.Optional;
import java.util.UUID;

import static java.net.http.HttpRequest.BodyPublishers.ofString;
import static uk.nhs.nhsx.core.Jackson.toJson;
import static uk.nhs.nhsx.core.UncheckedException.uncheckedGet;

public class InteropClient {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final String interopBaseUrl;
    private final String authToken;
    private final JWS jws;
    private final Events events;

    private final HttpClient client;

    public InteropClient(String interopBaseUrl, String authToken, JWS jws, Events events) {
        this.interopBaseUrl = interopBaseUrl;
        this.authToken = authToken;
        this.jws = jws;
        this.events = events;
        client = HttpClient.newHttpClient();
    }

    public Optional<DiagnosisKeysDownloadResponse> getExposureKeysBatch(LocalDate date, String batchTag) {
        var request = HttpRequest.newBuilder()
            .header("Authorization", "Bearer " + authToken)
            .uri(URI.create(interopBaseUrl + "/diagnosiskeys/download/" + date.format(FORMATTER) + batchTag))
            .build();

        var response = uncheckedGet(() -> client.send(request, HttpResponse.BodyHandlers.ofString()));

        events.emit(getClass(), new OutgoingHttpRequest(request.uri().toString(), request.method(), response.statusCode()));

        if (response.statusCode() == 200) {
            return Optional.of(
                Jackson.readMaybe(response.body(), DiagnosisKeysDownloadResponse.class,  e -> events.emit(getClass(), new UnprocessableJson(e)))
                    .orElseThrow(RuntimeException::new)
            );
        }

        if (response.statusCode() == 204) {
            return Optional.empty();
        }

        throw new RuntimeException("Request to download keys from federated key server with batch tag " + batchTag + " failed with status code " + response.statusCode());
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

            if (httpResponse.statusCode() == 200) {
                return Jackson.readMaybe(httpResponse.body(), DiagnosisKeysUploadResponse.class,  e -> events.emit(getClass(), new UnprocessableJson(e))).orElseThrow(RuntimeException::new);
            } else {
                throw new RuntimeException("Request to upload keys to federated key server failed with status code " + httpResponse.statusCode());
            }
        } catch (InterruptedException | IOException  e) {
            throw new RuntimeException("Request to upload keys to federated key server failed", e);
        }
    }
}
