package uk.nhs.nhsx.keyfederation;

import org.apache.http.entity.ContentType;
import uk.nhs.nhsx.core.Jackson;
import uk.nhs.nhsx.core.aws.s3.BucketName;
import uk.nhs.nhsx.core.aws.s3.ByteArraySource;
import uk.nhs.nhsx.core.aws.s3.Locator;
import uk.nhs.nhsx.core.aws.s3.ObjectKey;
import uk.nhs.nhsx.core.aws.s3.S3Storage;
import uk.nhs.nhsx.core.events.Events;
import uk.nhs.nhsx.core.events.InfoEvent;
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKey;
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKeyPayload;
import uk.nhs.nhsx.keyfederation.download.DiagnosisKeysDownloadResponse;
import uk.nhs.nhsx.keyfederation.download.ExposureKeysPayload;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toList;

public class FederatedKeyUploader {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);

    private final S3Storage s3Storage;
    private final BucketName bucketName;
    private final String federatedKeySourcePrefix;
    private final Supplier<Instant> clock;
    private final Supplier<String> dateStringProvider;
    private final List<String> validOrigins;
    private final Events events;

    public FederatedKeyUploader(S3Storage s3Storage,
                                BucketName bucketName,
                                String federatedKeySourcePrefix,
                                Supplier<Instant> clock,
                                List<String> validOrigins,
                                Events events) {
        this.clock = clock;
        this.s3Storage = s3Storage;
        this.bucketName = bucketName;
        this.federatedKeySourcePrefix = federatedKeySourcePrefix;
        this.dateStringProvider = () -> DATE_TIME_FORMATTER.format(clock.get());
        this.validOrigins = validOrigins;
        this.events = events;
    }

    public void acceptKeysFromFederatedServer(DiagnosisKeysDownloadResponse payload) {
        indexStoreModelKeysPerOrigin(payload)
            .forEach((origin, keys) -> handleOriginKeys(payload, origin, keys));
    }

    public Map<String, List<StoredTemporaryExposureKey>> indexStoreModelKeysPerOrigin(DiagnosisKeysDownloadResponse payload) {
        var keysPerOrigin = new HashMap<String, List<StoredTemporaryExposureKey>>();
        payload.exposures.forEach(it -> {
            if (!keysPerOrigin.containsKey(it.origin)) {
                keysPerOrigin.put(it.origin, new ArrayList<>());
            }
            var keysList = keysPerOrigin.get(it.origin);
            var storedTemporaryExposureKey = new StoredTemporaryExposureKey(
                it.keyData, it.rollingStartNumber, it.rollingPeriod, it.transmissionRiskLevel
            );
            keysList.add(storedTemporaryExposureKey);
        });
        return keysPerOrigin;
    }

    private void handleOriginKeys(DiagnosisKeysDownloadResponse payload,
                                  String origin,
                                  List<StoredTemporaryExposureKey> temporaryExposureKeys) {
        var validKeys = temporaryExposureKeys.stream()
            .filter(this::isValidKey)
            .collect(toList());

        var invalidKeysCount = temporaryExposureKeys.size() - validKeys.size();

        events.emit(getClass(), new DownloadedFederatedDiagnosisKeys(validKeys.size(), invalidKeysCount, origin));

        if (validOrigins.contains(origin)) {
            if (validKeys.size() > 0) {
                var exposureKeysPayload = new ExposureKeysPayload(origin, payload.batchTag, validKeys);

                uploadOriginKeysToS3(exposureKeysPayload);
            } else {
                events.emit(getClass(), new InfoEvent(
                    "Skip store to s3 because no valid keys were found or all keys were invalid, " +
                        "origin="+origin +", batchTag=" + payload.batchTag)
                );
            }
        } else {
            events.emit(getClass(), new InvalidOriginKeys(origin, payload.batchTag));
        }
    }

    private Boolean isValidKey(StoredTemporaryExposureKey temporaryExposureKey) {
        return Optional.ofNullable(temporaryExposureKey)
            .filter(tek -> isKeyValid(tek.key))
            .filter(tek -> isRollingStartNumberValid(clock, tek.rollingStartNumber, tek.rollingPeriod, events))
            .filter(tek -> isRollingPeriodValid(tek.rollingPeriod))
            .filter(tek -> isTransmissionRiskLevelValid(tek.transmissionRisk))
            .isPresent();
    }

    private boolean isKeyValid(String key) {
        var isValid = key != null && isBase64EncodedAndLessThan32Bytes(key);
        if (!isValid) events.emit(getClass(), new InvalidTemporaryExposureKey(key));
        return isValid;
    }

    public static boolean isRollingStartNumberValid(Supplier<Instant> clock, long rollingStartNumber, int rollingPeriod, Events events) {
        var now = clock.get();
        long TEN_MINUTES_INTERVAL_SECONDS = 600L;
        var currentInstant = now.getEpochSecond() / TEN_MINUTES_INTERVAL_SECONDS;
        var expiryPeriod = clock.get().minus(14, ChronoUnit.DAYS).getEpochSecond() / TEN_MINUTES_INTERVAL_SECONDS;
        var isValid = (rollingStartNumber + rollingPeriod) >= expiryPeriod &&
            rollingStartNumber <= currentInstant;

        if (!isValid) {
            events.emit(FederatedKeyUploader.class, new InvalidRollingStartNumber(now, rollingStartNumber, rollingPeriod));
        }

        return isValid;
    }

    private boolean isRollingPeriodValid(int isRollingPeriod) {
        var isValid = isRollingPeriod > 0 && isRollingPeriod <= 144;
        if (!isValid) events.emit(getClass(), new InvalidRollingPeriod(isRollingPeriod));
        return isValid;
    }

    private boolean isBase64EncodedAndLessThan32Bytes(String value) {
        try {
            byte[] key = Base64.getDecoder().decode(value);
            return key.length < 32;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean isTransmissionRiskLevelValid(int transmissionRiskLevel) {
        var isValid = transmissionRiskLevel >= 0 && transmissionRiskLevel <= 7;
        if (!isValid) events.emit(getClass(), new InvalidTransmissionRiskLevel(transmissionRiskLevel));
        return isValid;
    }

    private void uploadOriginKeysToS3(ExposureKeysPayload exposureKeysPayload) {
        var payload = new StoredTemporaryExposureKeyPayload(exposureKeysPayload.temporaryExposureKeys);
        var objectKey = ObjectKey.of(
            federatedKeySourcePrefix + "/" + exposureKeysPayload.origin + "/" +
                dateStringProvider.get() + "/" + exposureKeysPayload.batchTag + ".json"
        );
        s3Storage.upload(
            Locator.of(bucketName, objectKey),
            ContentType.APPLICATION_JSON,
            ByteArraySource.fromUtf8String(Jackson.toJson(payload))
        );
    }
}
